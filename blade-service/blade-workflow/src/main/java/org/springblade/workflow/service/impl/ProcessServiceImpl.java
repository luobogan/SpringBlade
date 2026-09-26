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
        Deployment deployment = repositoryService.createDeployment()
            .name(procKey)
            .key(procKey)
            .addString(procKey + ".bpmn20.xml", bpmnXml)
            .deploy();
        log.info("[blade-workflow] BPMN 已部署到引擎. procKey={}, deploymentId={}", procKey, deployment.getId());
        return deployment.getId();
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
