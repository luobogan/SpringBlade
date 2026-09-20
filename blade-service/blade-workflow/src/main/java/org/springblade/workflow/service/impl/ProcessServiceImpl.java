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
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public long pendingJobCount() {
        return managementService.createJobQuery().count()
            + managementService.createTimerJobQuery().count();
    }

}
