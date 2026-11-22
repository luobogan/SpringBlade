package org.springblade.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * E7 引擎→业务状态桥（2026-11-18）。
 *
 * <p>解决：引擎自动节点（ServiceTask/ScriptTask/CallActivity 等）跑完后，
 * token 已流到下一个 UserTask，但 SB 台账（ACT 待办业务列 + wf_instance 当前节点）无人同步——
 * 因为现有链路全靠业务层显式调 {@code advance()}，而自动节点由引擎驱动、不经过业务方法。</p>
 *
 * <p><b>工作方式</b>：监听 {@code ACTIVITY_COMPLETED}，仅对<b>自动节点类型</b>生效（见 {@link #AUTO_ACTIVITY_TYPES}），
 * 调用公开 {@link IWfInstanceService#advance(Long, Long, AdvanceSrc)} 复用完整建待办链路
 * （解析办理人 → 判重 → 生成 BIZ_TASK_ID_ → 写 ACT 10 列 → 超时 → 实例状态同步），
 * 不另起一套"建待办"实现，避免待办重复与 BIZ_TASK_ID_ 漂移。</p>
 *
 * <p><b>安全护栏</b>：</p>
 * <ul>
 *   <li>总开关 {@code blade.workflow.compat.engine-auto-todo-bridge} 默认 false，零行为变化；</li>
 *   <li>defId 灰度白名单（可选）；</li>
 *   <li>ThreadLocal 防重入 + 幂等键，避免"事件→advance→新事件"循环；</li>
 *   <li>只处理自动节点，UserTask 完成由业务链路 own，不重复 advance；</li>
 *   <li>失败捕获+告警，绝不回滚引擎操作（与 strict-event-fail 无关）。</li>
 * </ul>
 *
 * <p>注意：MI 节点完成派发的是 {@code MULTI_INSTANCE_ACTIVITY_*}，普通 ACTIVITY_COMPLETED 不覆盖父节点；
 * 本 listener 首版显式不处理 MI 完成事件（避免或签首人过后重复建待办），在注释中标注该边界。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blade.workflow.compat.engine-auto-todo-bridge", havingValue = "true", matchIfMissing = false)
public class WfEngineAutoTodoBridgeListener implements FlowableEventListener {

    /** 自动节点 activityType（Flowable ActivityType 常量值），仅这些完成后需要桥接。 */
    private static final Set<String> AUTO_ACTIVITY_TYPES = Set.of(
        "serviceTask", "scriptTask", "businessRuleTask", "callActivity"
    );

    /** ThreadLocal 防重入：一条线程内同一 procInstId 的 E7 同步只进一次。 */
    private static final ThreadLocal<Set<String>> ACTIVE = ThreadLocal.withInitial(ConcurrentHashMap::newKeySet);

    private final WfFlowtableCompatProperties compat;
    private final WfInstanceMapper instanceMapper;
    /**
     * 延迟解析 instanceService：本 listener 由 FlowableConfig#processEngineConfiguration 在引擎装配期
     * 经 ObjectProvider 取用，若此处直接强注入 IWfInstanceService（其内部依赖 ProcessService→processEngine）
     * 会形成 processEngineConfiguration→listener→instanceService→processEngine 的构造器循环依赖，启动失败。
     * 改为运行期（事件回调时，上下文已就绪）再 getObject，打断该环。
     */
    private final ObjectProvider<IWfInstanceService> instanceServiceProvider;

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableActivityEvent ae)) {
            return;
        }
        if (!(event.getType() instanceof FlowableEngineEventType type)
            || type != FlowableEngineEventType.ACTIVITY_COMPLETED) {
            return;
        }
        String activityType = ae.getActivityType();
        if (activityType == null || !AUTO_ACTIVITY_TYPES.contains(activityType)) {
            return; // UserTask / SubProcess / MI 父节点均不在白名单，交给业务链路或显式不处理
        }
        String procInstId = ae.getProcessInstanceId();
        if (procInstId == null || procInstId.isEmpty()) {
            return;
        }
        // 防重入：同实例同线程只同步一次
        if (!ACTIVE.get().add(procInstId)) {
            log.debug("[e7-bridge] 防重入跳过. procInstId={}, activity={}", procInstId, ae.getActivityId());
            return;
        }
        try {
            WfInstance inst = instanceMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<WfInstance>lambdaQuery()
                    .eq(WfInstance::getEngineInstId, procInstId).last("LIMIT 1"));
            if (inst == null) {
                log.warn("[e7-bridge] 找不到业务实例，无法同步. procInstId={}, activity={}",
                    procInstId, ae.getActivityId());
                return;
            }
            if (!inGray(inst)) {
                return;
            }
            // 自动节点没有"办理人"，用发起人作 currentOperator（OP_DEPT_SELF 解析为发起人部门，合理降级）。
            // 运行期才解析 instanceService（打破构造期循环依赖）。
            instanceServiceProvider.getObject().advance(inst.getId(), inst.getStarter(), null, null, AdvanceSrc.SUBMIT);
            log.info("[e7-bridge] 自动节点完成后同步成功. instId={}, procInstId={}, activity={}",
                inst.getId(), procInstId, ae.getActivityId());
        } catch (Exception e) {
            // 建待办失败：告警+补偿入口预留，绝不回滚引擎（否则自动节点白跑了）
            log.error("[e7-bridge] 同步失败（不回滚引擎）. procInstId={}, activity={}, err={}",
                procInstId, ae.getActivityId(), e.getMessage(), e);
        } finally {
            ACTIVE.get().remove(procInstId);
        }
    }

    /** 灰度：白名单空=全部生效；非空=仅名单内 defId。 */
    private boolean inGray(WfInstance inst) {
        List<Long> gray = compat.getEngineAutoTodoGrayDefIds();
        return gray == null || gray.isEmpty()
            || (inst.getDefId() != null && gray.contains(inst.getDefId()));
    }

    @Override
    public boolean isFailOnException() {
        // E7 自身已 catch 全部异常并告警，不向引擎传播；与 strict-event-fail 无关。
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
