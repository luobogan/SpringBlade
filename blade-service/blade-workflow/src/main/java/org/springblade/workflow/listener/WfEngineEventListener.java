package org.springblade.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.task.api.Task;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.helper.WfStateProjector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 方案C 事件驱动台账状态监听（骨架 / 影子模式）。
 *
 * <p>全局 {@link FlowableEventListener}：一处注册覆盖全部流程，与动态 BPMN 完全解耦（方案C §2.1）。
 * 当前仅把引擎事件派发给 {@link WfStateProjector} 做<b>影子记录</b>（只记日志、不写库，对应设计 §6 阶段1），
 * 用于在不改动业务双写的前提下，先验证「引擎事件 → 期望 wf_* 状态」映射的正确性。</p>
 *
 * <p>开关 {@code blade.workflow.ledger-listener.enabled} 默认 <b>false</b>（方案C §7）：关闭即不注册，
 * 完全回到方案A 的显式双写；开启后进入影子模式。后续阶段2/3 才在 {@link WfStateProjector} 内开启真实反写。</p>
 *
 * <p>依赖约束（方案C §2.2 方式一）：本监听<b>不注入任何引擎 Service</b>，仅依赖 {@link WfStateProjector}
 * （其内部只依赖 wf_* Mapper），故通过 {@code configuration.setEventListeners(...)} 直接装配，
 * 不会与 {@code processEngineConfiguration} 形成构造期循环依赖。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "blade.workflow.ledger-listener.enabled", havingValue = "true", matchIfMissing = false)
public class WfEngineEventListener implements FlowableEventListener {

    private final WfStateProjector projector;
    /** flowtable 扩展双轨开关：strict-event-fail 仅作用于本台账投影 listener（默认 false=旧行为） */
    private final WfFlowtableCompatProperties compat;

    public WfEngineEventListener(WfStateProjector projector, WfFlowtableCompatProperties compat) {
        this.projector = projector;
        this.compat = compat;
    }

    /**
     * 事件派发。批次1 后台账投影已切「直写 ACT 业务列」：写失败由写入器 catch→warn 兜住，
     * 故 {@link #isFailOnException()} 默认 false（最终一致 C2），不因投影失败回滚引擎操作。
     *
     * <p>如需 C1 强一致（投影写失败回滚引擎），置 {@code blade.workflow.compat.strict-event-fail=true}；
     * 该开关<b>仅作用于本台账投影 listener</b>，不影响通知/回调 listener（恒 false）与
     * MI 空集合守卫（恒 true）。</p>
     *
     * <p>投影器对「查不到 wf_instance / wf_task 行」等数据性情况只记 warn 并返回，不抛异常——
     * 那属于数据问题，不应阻断用户操作。</p>
     */
    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event.getType() instanceof FlowableEngineEventType type)) {
            return;
        }
        // 统一从 FlowableEngineEvent 取 processInstanceId；任务事件 entity 为 Task，取其 taskId/assignee
        String procInstId = null;
        String taskId = null;
        String assignee = null;
        if (event instanceof FlowableEngineEvent ee) {
            procInstId = ee.getProcessInstanceId();
        }
        if (event instanceof FlowableEntityEvent ee) {
            Object entity = ee.getEntity();
            if (entity instanceof Task t) {
                taskId = t.getId();
                assignee = t.getAssignee();
                if (procInstId == null) {
                    procInstId = t.getProcessInstanceId();
                }
            }
        }
        switch (type) {
            case PROCESS_STARTED -> projector.onProcessStarted(procInstId);
            case PROCESS_COMPLETED -> projector.onProcessCompleted(procInstId);
            case PROCESS_CANCELLED -> projector.onProcessCancelled(procInstId);
            case ENTITY_SUSPENDED -> projector.onEntitySuspended(procInstId);
            case ENTITY_ACTIVATED -> projector.onEntityActivated(procInstId);
            case TASK_CREATED -> projector.onTaskCreated(taskId, procInstId, assignee);
            case TASK_ASSIGNED -> projector.onTaskAssigned(taskId, assignee);
            case TASK_COMPLETED -> projector.onTaskCompleted(taskId, procInstId);
            // 会签/或签下沉 MI 后，节点放行由引擎完成条件判定，引擎会删除同节点其余 MI 子任务。
            // Flowable 7.1 的 FlowableEngineEventType 没有 TASK_CANCELLED，删除任务以
            // ENTITY_DELETED + Task 实体派发（taskId 已在上方从实体解析）。
            // 不接这条会让被删者的 wf_task 永久停在待办(0)：形成幽灵待办，且自动测试每轮按 id 取
            // todos.get(0) 会优先抓到它而审批失败、卡死（见 WfStateProjector#onTaskDeleted 注释）。
            case ENTITY_DELETED -> {
                if (taskId != null) {
                    projector.onTaskDeleted(taskId, procInstId);
                }
            }
            default -> {
                // 其余事件（变量/作业/活动/序列流等）不参与台账反写
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        // 双轨开关 strict-event-fail（默认 false=旧行为）：
        // false=批次1 起的 ACT 直写最终一致（写失败 catch→warn，不回滚引擎）；
        // true =C1 强一致（投影写失败回滚引擎）。
        // ⚠️ 本开关只作用于台账投影 listener；通知/回调恒 false、MI 空集合守卫恒 true。
        return compat.isStrictEventFail();
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        // 不在事务提交/回滚生命周期额外触发，仅随引擎 Command 同步派发
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
