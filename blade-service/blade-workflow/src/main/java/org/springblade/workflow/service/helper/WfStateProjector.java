package org.springblade.workflow.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 方案C 台账状态投影器（骨架 / 影子模式）。
 *
 * <p>职责：把<b>引擎事件</b>翻译成<b>期望的 wf_* 状态</b>（wf_instance / wf_task）。
 * 当前骨架仅<b>记录期望映射</b>、不写库（设计 §6 阶段1 影子模式），用于验证映射正确性。</p>
 *
 * <p>事件 → 期望台账写入的对照（对齐方案C §2.3，状态码以治理文档为准）：
 * <ul>
 *   <li>PROCESS_STARTED    → wf_instance 建行 + 回填 engine_inst_id</li>
 *   <li>PROCESS_COMPLETED  → wf_instance.status=通过(1)+end_time；关闭残留待办</li>
 *   <li>PROCESS_CANCELLED  → wf_instance.status=撤销(3)+end_time；关闭全部待办（对应 deleteProcessInstance）</li>
 *   <li>ENTITY_SUSPENDED   → wf_instance.status=暂停(4)</li>
 *   <li>ENTITY_ACTIVATED   → wf_instance.status=运行中(0)</li>
 *   <li>TASK_CREATED       → wf_task 建待办行（关联 engine_task_id / assignee）</li>
 *   <li>TASK_ASSIGNED      → 回填 wf_task.assignee</li>
 *   <li>TASK_COMPLETED     → wf_task.status=已办(2)+operate_time</li>
 * </ul>
 * </p>
 *
 * <p>后续阶段2/3 的落地要点（届时实现，本骨架不触碰）：
 * <ol>
 *   <li>注入 WfInstanceMapper / WfTaskMapper，按 engine_inst_id / engine_task_id 先查后写（幂等，§4.4）；</li>
 *   <li>写失败按 C1 强一致抛异常回滚（isFailOnExceptionReturned=true）；</li>
 *   <li>绝不回查 ACT_RU_*（事件触发时可能已删除，§4.5），一律用事件自带 id 关联；</li>
 *   <li>entity 为 Task 时从事件 entity 取 id，entity 为 ProcessInstance 时从事件 processInstanceId 取。</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
public class WfStateProjector {

    /** PROCESS_STARTED：期望 wf_instance 建行并回填 engine_inst_id */
    public void onProcessStarted(String procInstId) {
        log.info("[WfStateProjector][shadow] PROCESS_STARTED -> 期望 wf_instance 建行并回填 engine_inst_id; engineInstId={}", procInstId);
    }

    /** PROCESS_COMPLETED：期望 wf_instance.status=通过(1)+end_time，关闭残留待办 */
    public void onProcessCompleted(String procInstId) {
        log.info("[WfStateProjector][shadow] PROCESS_COMPLETED -> 期望 wf_instance.status=通过(1)+end_time, 关闭残留待办; engineInstId={}", procInstId);
    }

    /** PROCESS_CANCELLED：期望 wf_instance.status=撤销(3)+end_time，关闭全部待办（对应 deleteProcessInstance） */
    public void onProcessCancelled(String procInstId) {
        log.info("[WfStateProjector][shadow] PROCESS_CANCELLED -> 期望 wf_instance.status=撤销(3)+end_time, 关闭全部待办; engineInstId={}", procInstId);
    }

    /** ENTITY_SUSPENDED（ProcessInstance）：期望 wf_instance.status=暂停(4) */
    public void onEntitySuspended(String procInstId) {
        log.info("[WfStateProjector][shadow] ENTITY_SUSPENDED -> 期望 wf_instance.status=暂停(4); engineInstId={}", procInstId);
    }

    /** ENTITY_ACTIVATED（ProcessInstance）：期望 wf_instance.status=运行中(0) */
    public void onEntityActivated(String procInstId) {
        log.info("[WfStateProjector][shadow] ENTITY_ACTIVATED -> 期望 wf_instance.status=运行中(0); engineInstId={}", procInstId);
    }

    /** TASK_CREATED：期望 wf_task 建待办行（关联 engine_task_id 与 assignee） */
    public void onTaskCreated(String taskId, String procInstId, String assignee) {
        log.info("[WfStateProjector][shadow] TASK_CREATED -> 期望 wf_task 建待办行(engine_task_id={}, assignee={}); engineInstId={}",
            taskId, assignee, procInstId);
    }

    /** TASK_ASSIGNED：期望回填 wf_task.assignee */
    public void onTaskAssigned(String taskId, String assignee) {
        log.info("[WfStateProjector][shadow] TASK_ASSIGNED -> 期望 wf_task.assignee={}; engineTaskId={}", assignee, taskId);
    }

    /** TASK_COMPLETED：期望 wf_task.status=已办(2)+operate_time */
    public void onTaskCompleted(String taskId, String procInstId) {
        log.info("[WfStateProjector][shadow] TASK_COMPLETED -> 期望 wf_task.status=已办(2)+operate_time; engineTaskId={}", taskId);
    }
}
