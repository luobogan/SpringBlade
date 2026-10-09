package org.springblade.workflow.listener;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.user.feign.IUserStatusFlowClient;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 业务回调监听（P3-2：人员状态流转审批完成后回调 blade-system）。
 *
 * <p>为什么<b>不复用</b> {@link WfEngineEventListener}：后者是「方案C 台账影子模式」——
 * 默认关闭、只把引擎事件投影到 {@code wf_instance/wf_task}，且其注释明确「不注入任何引擎 Service」。
 * 本监听与其同构（同样只依赖 {@code wf_*} Mapper，走 {@code setEventListeners} 直接装配以避免
 * 与 {@code processEngineConfiguration} 构造期循环依赖），但职责不同：把「流程完成」翻译成业务回调。</p>
 *
 * <p><b>粗筛策略</b>：先按 {@code engine_inst_id} 查到 {@code wf_instance}，仅当标题以
 * {@code PSF:} 开头（人员状态流转流程的约定前缀，由 blade-system 发起时写入）才跨服务回调，
 * 避免每条流程完成都产生一次 Feign 调用。</p>
 *
 * <p>开关 {@code blade.workflow.biz-callback.enabled}，默认 <b>true</b>（与台账影子监听不同：
 * 本监听是 P3 审批闭环的必要路径，且失败只记日志不影响引擎）。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "blade.workflow.biz-callback.enabled", havingValue = "true", matchIfMissing = true)
public class WfBizCallbackListener implements FlowableEventListener {

    /** 人员状态流转流程标题前缀（与 blade-system PersonStatusFlowServiceImpl.TITLE_PREFIX 一致） */
    private static final String PSF_PREFIX = "PSF:";

    private final WfInstanceMapper instanceMapper;
    private final IUserStatusFlowClient statusFlowClient;

    public WfBizCallbackListener(WfInstanceMapper instanceMapper, IUserStatusFlowClient statusFlowClient) {
        this.instanceMapper = instanceMapper;
        this.statusFlowClient = statusFlowClient;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event.getType() instanceof FlowableEngineEventType type)) {
            return;
        }
        // 仅「审批通过走完流程」触发；驳回在本流程里是退回发起节点重新提交，不会走到完成事件
        if (type != FlowableEngineEventType.PROCESS_COMPLETED) {
            return;
        }
        String procInstId = event instanceof FlowableEngineEvent ee ? ee.getProcessInstanceId() : null;
        if (procInstId == null) {
            return;
        }
        WfInstance instance = instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getEngineInstId, procInstId)
            .last("LIMIT 1"));
        if (instance == null) {
            log.debug("[WfBizCallback] 未找到 engine_inst_id={} 的 wf_instance，跳过", procInstId);
            return;
        }
        if (!Func.toStr(instance.getTitle(), "").startsWith(PSF_PREFIX)) {
            return;
        }
        try {
            // 回调 blade-system：按 instanceId 反查流转记录并落库 person_status（服务端幂等）
            // 注意：opinion 在 Feign 契约里是必填 @RequestParam，OpenFeign 会跳过 null 参数，
            // 传 null 会导致对端 400（缺少必要参数）而整调用降级；无意见时传空串。
            R<Boolean> result = statusFlowClient.statusFlowCallback(String.valueOf(instance.getId()), Boolean.TRUE, "");
            if (result == null || !result.isSuccess()) {
                log.warn("[WfBizCallback] 状态流转回调失败. instanceId={}, msg={}",
                    instance.getId(), result == null ? "null" : result.getMsg());
                return;
            }
            log.info("[WfBizCallback] 状态流转回调成功. instanceId={}", instance.getId());
        } catch (Exception e) {
            // 业务回调失败不得回滚引擎事务（审批已完成，状态变更可补偿重试）
            log.error("[WfBizCallback] 状态流转回调异常. instanceId={}, {}", instance.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean isFailOnException() {
        // 审批本身已由引擎完成，业务回调属「通知」语义：失败只记日志，不回滚引擎
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
