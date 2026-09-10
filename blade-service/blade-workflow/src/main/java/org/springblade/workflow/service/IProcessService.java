package org.springblade.workflow.service;

import org.springblade.workflow.vo.TaskVO;

import java.util.List;
import java.util.Map;

/**
 * 流程<b>引擎适配</b>服务（Flowable）
 *
 * <p>职责边界：只做「引擎层面」的原子操作（发起、查任务、完成），
 * <b>不</b>涉及 wf_* 业务语义表。业务语义（实例/任务/日志/权限）由
 * {@link IWfInstanceService}、{@link IWfTaskService} 负责，
 * 以此保证 wf_* 与引擎表解耦，保留替换引擎的演进位。</p>
 */
public interface IProcessService {

    /**
     * 发起引擎流程实例
     *
     * @param procKey   流程定义Key（BPMN process id）
     * @param bizKey    业务主键（formId:dataId）
     * @param variables 流程变量
     * @return 引擎实例ID（PROC_INST_ID_）
     */
    String startInstance(String procKey, String bizKey, Map<String, Object> variables);

    /**
     * 查询指定引擎实例的当前活动任务
     */
    List<TaskVO> currentTasks(String engineInstId);

    /**
     * 按办理人查询引擎任务
     */
    List<TaskVO> queryTasks(String assignee);

    /**
     * 完成引擎任务
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 部署 BPMN 2.0 定义到引擎
     *
     * @param procKey  流程定义Key（BPMN process id，作为部署资源名前缀）
     * @param bpmnXml  BPMN 2.0 XML 文本（bpmn-js 画布产出）
     * @return 引擎部署ID
     */
    String deployProcess(String procKey, String bpmnXml);

}
