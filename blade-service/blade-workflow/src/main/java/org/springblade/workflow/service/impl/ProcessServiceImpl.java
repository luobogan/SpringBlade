package org.springblade.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flowable 引擎适配实现
 *
 * <p>只封装引擎原子能力，不写 wf_* 表。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements IProcessService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final ManagementService managementService;

    @Override
    public String startInstance(String procKey, String bizKey, Map<String, Object> variables) {
        Map<String, Object> vars = (variables == null) ? new HashMap<>(8) : variables;
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(procKey, bizKey, vars);
        log.info("[blade-workflow] 引擎发起流程. procKey={}, bizKey={}, engineInstId={}",
            procKey, bizKey, instance.getId());
        return instance.getId();
    }

    @Override
    public String latestProcDefId(String procKey) {
        if (procKey == null || procKey.isBlank()) {
            return null;
        }
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(procKey)
            .latestVersion()
            .singleResult();
        return pd == null ? null : pd.getId();
    }

    @Override
    public String startInstanceById(String processDefinitionId, String bizKey, Map<String, Object> variables) {
        Map<String, Object> vars = (variables == null) ? new HashMap<>(8) : variables;
        ProcessInstance instance = runtimeService.startProcessInstanceById(processDefinitionId, bizKey, vars);
        log.info("[blade-workflow] 引擎按定义ID发起流程. procDefId={}, bizKey={}, engineInstId={}",
            processDefinitionId, bizKey, instance.getId());
        return instance.getId();
    }

    @Override
    public List<TaskVO> currentTasks(String engineInstId) {
        List<Task> tasks = taskService.createTaskQuery()
            .processInstanceId(engineInstId)
            .list();
        return toTaskVO(tasks);
    }

    @Override
    public List<TaskVO> queryTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(assignee)
            .orderByTaskCreateTime()
            .desc()
            .list();
        return toTaskVO(tasks);
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, (variables == null) ? new HashMap<>(4) : variables);
    }

    @Override
    public Object getVariable(String engineInstId, String name) {
        if (engineInstId == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            return runtimeService.getVariable(engineInstId, name);
        } catch (Exception e) {
            // 实例可能已结束（历史态），变量读不到属正常，按 null 处理
            return null;
        }
    }

    @Override
    public void removeVariables(String engineInstId, List<String> names) {
        if (engineInstId == null || names == null || names.isEmpty()) {
            return;
        }
        try {
            for (String name : names) {
                runtimeService.removeVariable(engineInstId, name);
            }
        } catch (Exception e) {
            // 实例已结束时删变量无意义，忽略
        }
    }

    @Override
    public void setProcessVariable(String engineInstId, String name, Object value) {
        if (engineInstId == null || engineInstId.isBlank() || name == null || name.isBlank()) {
            return;
        }
        try {
            runtimeService.setVariable(engineInstId, name, value);
        } catch (Exception e) {
            // 实例已结束时写变量无意义，忽略（不影响业务主链路）
            log.debug("[blade-workflow] 写入流程变量失败（实例可能已结束）: {}", e.getMessage());
        }
    }

    @Override
    public String deployProcess(String procKey, String bpmnXml) {
        if (procKey == null || procKey.isBlank() || bpmnXml == null || bpmnXml.isBlank()) {
            throw new IllegalArgumentException("部署 BPMN 失败：procKey 与 bpmnXml 均不能为空");
        }
        String safeXml = sanitizeForDeploy(bpmnXml);
        Deployment deployment = repositoryService.createDeployment()
            .name(procKey)
            .key(procKey)
            .addString(procKey + ".bpmn20.xml", safeXml)
            .deploy();
        log.info("[blade-workflow] BPMN 已部署到引擎. procKey={}, deploymentId={}", procKey, deployment.getId());
        return deployment.getId();
    }

    /**
     * 清理前端画布导出的<b>非标准 BPMNDI 属性</b>，使 XML 能通过 Flowable 生产部署的
     * XML Schema 校验（{@code deployProcess} 不关校验；测试部署 {@link #deployProcessForTest}
     * 已 {@code disableSchemaValidation} 故不受影响）。
     *
     * <p>根因：画布保存的 {@code bpmndi:BPMNEdge / BPMNShape} 上带 {@code strokeWidth="..."}
     * 等扩展属性，BPMNDI XSD 未定义该属性，Schema 校验直接拒绝部署——
     * 报 {@code cvc-complex-type.3.2.2: 元素 'bpmndi:BPMNEdge' 中不允许出现属性 'strokeWidth'}，
     * 所有<b>导入类流程</b>（如 comprehensiveApproval 系列）正式发布必挂。
     * 清理只删图形装饰属性、不改流程语义（节点/连线/条件表达式均原样保留）。</p>
     */
    public static String sanitizeBpmnDiForSchema(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.indexOf("strokeWidth") < 0) {
            return bpmnXml;
        }
        // 连同前导空白一并删除，避免留下游离空白
        String cleaned = bpmnXml.replaceAll("\\s+strokeWidth=\"[^\"]*\"", "");
        if (!cleaned.equals(bpmnXml)) {
            log.info("[blade-workflow] 已清理 BPMN 非标准 DI 属性 strokeWidth（前端画布扩展，Schema 校验不允许）");
        }
        return cleaned;
    }

    /**
     * 生产部署前的 BPMN 消毒总入口（保持<b>流程语义不变</b>，只修「画布/导入器产出
     * 但 Flowable 严格校验不允许」的冗余/非法写法）：
     * <ul>
     *   <li>{@link #sanitizeBpmnDiForSchema}：剥离非标准 BPMNDI 属性（如 {@code strokeWidth}）；</li>
     *   <li>{@link #stripDefaultFlowCondition}：剥离「网关 default 流」上的冗余条件表达式。</li>
     * </ul>
     * 仅 {@code deployProcess}（生产发布，保留完整 Schema + 语义校验）需要；
     * 测试部署 {@link #deployProcessForTest} 已双关校验，不必消毒。
     */
    public static String sanitizeForDeploy(String bpmnXml) {
        return stripDefaultFlowCondition(sanitizeBpmnDiForSchema(bpmnXml));
    }

    /** 网关标签（含 default 属性）：exclusive / inclusive 两种，标签可自闭合 */
    private static final java.util.regex.Pattern GATEWAY_WITH_DEFAULT = java.util.regex.Pattern.compile(
        "<(exclusiveGateway|inclusiveGateway)\\b[^>]*\\bdefault=\"([^\"]+)\"[^>]*/?>");

    /**
     * 剔除「网关 default 流」上的 {@code conditionExpression}。
     *
     * <p>根因：BPMN 语义上 default 流只在<b>其余条件全部不成立</b>时被选中，
     * 它自身的条件恒冗余；Flowable 语义校验集
     * {@code flowable-exclusive-gateway-condition-on-seq-flow} 直接拒绝部署——
     * 报 {@code Default sequenceflow has a condition, which is not allowed}。
     * 画布/导入器会把分支条件同时冗余写到 default 流上（如 comprehensiveApproval 的
     * {@code Gateway_Amount default="Flow_6"} 且 Flow_6 带 {@code ${amount > 5000}}）。
     * 剥离后行为不变：default 分支依旧在其它条件全假时命中。</p>
     *
     * <p>只处理「确实被 default 引用」的流；普通条件流原样保留。采用 tempered 匹配
     * （{@code (?!</sequenceFlow>).}）确保条件一定取自该流自身块内，绝不越界误删相邻流的条件。</p>
     */
    static String stripDefaultFlowCondition(String bpmnXml) {
        if (bpmnXml == null || !bpmnXml.contains("default=")) {
            return bpmnXml;
        }
        java.util.LinkedHashSet<String> defaultFlowIds = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher gw = GATEWAY_WITH_DEFAULT.matcher(bpmnXml);
        while (gw.find()) {
            defaultFlowIds.add(gw.group(2));
        }
        String result = bpmnXml;
        for (String flowId : defaultFlowIds) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?s)(<sequenceFlow\\b[^>]*\\bid=\"" + java.util.regex.Pattern.quote(flowId)
                    + "\"[^>]*>(?:(?!</sequenceFlow>).)*?)"
                    + "<conditionExpression\\b[^>]*>.*?</conditionExpression>"
                    + "(.*?</sequenceFlow>)");
            java.util.regex.Matcher m = p.matcher(result);
            if (m.find()) {
                result = m.replaceAll("$1$2");
                log.info("[blade-workflow] 已剥离网关 default 流上的冗余条件. flowId={}", flowId);
            }
        }
        return result;
    }

    @Override
    public String deployProcessForTest(String procKey, String bpmnXml) {
        if (procKey == null || procKey.isBlank() || bpmnXml == null || bpmnXml.isBlank()) {
            throw new IllegalArgumentException("测试部署 BPMN 失败：procKey 与 bpmnXml 均不能为空");
        }
        Deployment deployment = repositoryService.createDeployment()
            .name(procKey)
            .key(procKey)
            .addString(procKey + ".bpmn20.xml", bpmnXml)
            .disableSchemaValidation()
            .disableBpmnValidation()
            .deploy();
        log.info("[blade-workflow] BPMN 已测试部署到引擎（已关闭语义校验，未改发布状态）. procKey={}, deploymentId={}",
            procKey, deployment.getId());
        return deployment.getId();
    }

    @Override
    public void moveActivity(String engineInstId, String fromActivityKey, String toActivityKey,
                             Map<String, Object> variables) {
        if (engineInstId == null || fromActivityKey == null || toActivityKey == null) {
            throw new IllegalArgumentException("指定流转失败：实例ID与起止节点均不能为空");
        }
        if (variables != null && !variables.isEmpty()) {
            runtimeService.setVariables(engineInstId, variables);
        }
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(engineInstId)
            .moveActivityIdTo(fromActivityKey, toActivityKey)
            .changeState();
        log.info("[blade-workflow] 引擎指定流转跳转. engineInstId={}, {} -> {}",
            engineInstId, fromActivityKey, toActivityKey);
    }

    @Override
    public void moveActivityToActivities(String engineInstId, String fromActivityKey,
                                         List<String> toActivityKeys, Map<String, Object> variables) {
        if (engineInstId == null || fromActivityKey == null || toActivityKeys == null
            || toActivityKeys.isEmpty()) {
            throw new IllegalArgumentException("指定流转（多目标）失败：实例ID、起止节点均不能为空");
        }
        if (variables != null && !variables.isEmpty()) {
            runtimeService.setVariables(engineInstId, variables);
        }
        // 单目标退化为普通 move，保持语义一致；多目标则并行扇出（每组一个 token）
        if (toActivityKeys.size() == 1) {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(engineInstId)
                .moveActivityIdTo(fromActivityKey, toActivityKeys.get(0))
                .changeState();
        } else {
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(engineInstId)
                .moveSingleActivityIdToActivityIds(fromActivityKey, toActivityKeys)
                .changeState();
        }
        log.info("[blade-workflow] 引擎指定流转（多目标）跳转. engineInstId={}, {} -> {}",
            engineInstId, fromActivityKey, String.join(",", toActivityKeys));
    }

    @Override
    public void addComment(String taskId, String procInstId, String type, String message) {
        if (procInstId == null || message == null) {
            return;
        }
        try {
            // 引擎原生审批/流转意见：落 ACT_HI_COMMENT（history ≥ audit，见 FlowableConfig）
            taskService.addComment(taskId, procInstId, type, message);
        } catch (Exception e) {
            // 实例/任务已结束时写意见无意义，忽略（不影响业务主链路）
            log.debug("[blade-workflow] 写入审批意见失败（实例可能已结束）: {}", e.getMessage());
        }
    }

    @Override
    public void addUserIdentityLink(String taskId, String userId, String identityLinkType) {
        if (taskId == null || userId == null || identityLinkType == null) {
            return;
        }
        try {
            // 引擎原生身份关联：抄送/传阅复用同一 engineTaskId，不产生新的可办任务行
            taskService.addUserIdentityLink(taskId, userId, identityLinkType);
        } catch (Exception e) {
            log.debug("[blade-workflow] 添加身份关联失败（任务可能已结束）: {}", e.getMessage());
        }
    }

    private static List<TaskVO> toTaskVO(List<Task> tasks) {
        List<TaskVO> result = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            TaskVO vo = new TaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
            vo.setAssignee(task.getAssignee());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<HistoricActivityInstance> historicActivities(String engineInstId) {
        return historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(engineInstId)
            .orderByHistoricActivityInstanceStartTime()
            .asc()
            .list();
    }

    @Override
    public HistoricProcessInstance historicProcess(String engineInstId) {
        return historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(engineInstId)
            .singleResult();
    }

    @Override
    public String latestDeploymentId(String procKey) {
        if (procKey == null || procKey.isBlank()) {
            return null;
        }
        // 用 var 而不是 import org.flowable.engine.repository.ProcessDefinition：
        // 查询返回类型即 ProcessDefinition，无需额外导入（少一处改动，也避免与同包 Deployment 混用）
        var pd = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(procKey)
            .latestVersion()
            .singleResult();
        return pd == null ? null : pd.getDeploymentId();
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
        log.info("[blade-workflow] 测试部署已卸载. deploymentId={}", deploymentId);
    }

    @Override
    public void suspendProcessDefinition(String procDefId) {
        if (procDefId == null || procDefId.isBlank()) {
            return;
        }
        try {
            repositoryService.suspendProcessDefinitionById(procDefId);
            log.info("[blade-workflow] 已挂起引擎流程定义. procDefId={}", procDefId);
        } catch (org.flowable.common.engine.api.FlowableObjectNotFoundException ex) {
            // 引擎中已无该定义（如测试部署被清理）：忽略，撤回仍能正常回退业务状态
            log.warn("[blade-workflow] 挂起流程定义时未找到（已不存在），忽略. procDefId={}", procDefId);
        }
    }

    @Override
    public void deleteProcessInstance(String engineInstId, String reason) {
        if (engineInstId == null || engineInstId.isBlank()) {
            return;
        }
        try {
            runtimeService.deleteProcessInstance(engineInstId, reason);
            log.info("[blade-workflow] 已删除引擎流程实例. engineInstId={}, reason={}", engineInstId, reason);
        } catch (org.flowable.common.engine.api.FlowableException ex) {
            // 引擎实例已结束/不存在（如已自动完成、或弱关联实例已清理）：忽略，
            // 业务终态仍生效，不让引擎异常把业务事务一起回滚（消除 ACT_RU_* 孤儿漂移）
            log.warn("[blade-workflow] 删除引擎流程实例时未找到（已不存在），忽略. engineInstId={}", engineInstId);
        }
    }

    @Override
    public void suspendProcessInstance(String engineInstId) {
        if (engineInstId == null || engineInstId.isBlank()) {
            return;
        }
        try {
            runtimeService.suspendProcessInstanceById(engineInstId);
            log.info("[blade-workflow] 已挂起引擎流程实例. engineInstId={}", engineInstId);
        } catch (org.flowable.common.engine.api.FlowableException ex) {
            // 实例缺失或已挂起：忽略，业务「已暂停」状态仍生效
            log.warn("[blade-workflow] 挂起引擎流程实例失败（已不存在/已挂起），忽略. engineInstId={}", engineInstId);
        }
    }

    @Override
    public void activateProcessInstance(String engineInstId) {
        if (engineInstId == null || engineInstId.isBlank()) {
            return;
        }
        try {
            runtimeService.activateProcessInstanceById(engineInstId);
            log.info("[blade-workflow] 已激活引擎流程实例. engineInstId={}", engineInstId);
        } catch (org.flowable.common.engine.api.FlowableException ex) {
            // 实例缺失或已激活：忽略，业务「运行中」状态仍生效
            log.warn("[blade-workflow] 激活引擎流程实例失败（已不存在/已激活），忽略. engineInstId={}", engineInstId);
        }
    }

    @Override
    public List<String> deploymentIdsByKeyLike(String keyLike) {
        if (keyLike == null || keyLike.isBlank()) {
            return new ArrayList<>();
        }
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKeyLike(keyLike)
            .list();
        // 同一部署可能含多个流程定义：按部署ID去重（LinkedHashSet 保持稳定顺序，便于日志复现）
        Set<String> ids = new LinkedHashSet<>();
        for (ProcessDefinition pd : defs) {
            if (pd.getDeploymentId() != null) {
                ids.add(pd.getDeploymentId());
            }
        }
        return new ArrayList<>(ids);
    }

    @Override
    public long pendingJobCount() {
        return managementService.createJobQuery().count()
            + managementService.createTimerJobQuery().count();
    }

}
