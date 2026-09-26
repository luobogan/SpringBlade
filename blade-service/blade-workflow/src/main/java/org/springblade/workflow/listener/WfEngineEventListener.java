package org.springblade.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.task.api.Task;
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

    public WfEngineEventListener(WfStateProjector projector) {
        this.projector = projector;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        try {
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
                default -> {
                    // 其余事件（变量/作业/活动/序列流等）暂不在影子范围内
                }
            }
        } catch (Exception e) {
            // 影子模式：绝不抛异常影响引擎事务，仅记录，避免引入漂移或阻断流程
            log.warn("[WfEngineEventListener] 事件处理异常（影子模式已忽略）: type={}, msg={}",
                event != null ? event.getType() : null, e.getMessage());
        }
    }

    @Override
    public boolean isFailOnException() {
        // 影子模式不回滚引擎操作；真实反写阶段（C1 强一致）再按需改为 true
        return false;
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
