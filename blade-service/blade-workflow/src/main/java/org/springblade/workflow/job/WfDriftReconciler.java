package org.springblade.workflow.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * S5 前置兜底：常驻漂移对账（2026-11-18）。
 *
 * <p>方案 C §3.4 要求 AsyncExecutor 开启前必须具备三项兜底：C1 最终一致（strict-event-fail=false 已满足）、
 * 常驻对账修复（本类）、deadletter 告警（WfDeadLetterJobScanner 已实现）。</p>
 *
 * <p><b>与一次性脚本 HistoricalDriftFixer 的区别</b>：本对账器只扫<b>最近时间窗内</b>进入终态的实例，
 * 且逐个用 {@link IProcessService#isProcessInstanceEnded} 确认引擎确实仍活跃才动手——
 * 避免每轮全量扫描历史实例、对已清理的实例做无谓的 delete 调用。</p>
 *
 * <p><b>安全护栏</b>：独立开关 {@code blade.workflow.compat.drift-reconcile-enabled} 默认 false（bean 都不存在）；
 * is_test=0 隔离；单条失败不影响其余；只修「业务终态但引擎活跃」的确定性漂移，不碰运行中实例。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blade.workflow.compat.drift-reconcile-enabled", havingValue = "true", matchIfMissing = false)
public class WfDriftReconciler {

    /** 对账时间窗（分钟）：只扫最近进入终态的实例 */
    private static final int WINDOW_MINUTES = 30;
    private static final long ONE_MINUTE_MS = 60_000L;

    private final IProcessService processService;
    private final WfInstanceMapper instanceMapper;
    private final WfFlowtableCompatProperties compat;

    @Scheduled(initialDelayString = "${blade.workflow.drift-reconcile-initial-delay-ms:120000}",
        fixedDelayString = "${blade.workflow.drift-reconcile-scan-ms:600000}")
    public void scan() {
        if (!compat.isDriftReconcileEnabled()) {
            return;
        }
        doScan();
    }

    /** package-private 便于同包测试直接驱动 */
    void doScan() {
        Date since = new Date(System.currentTimeMillis() - WINDOW_MINUTES * ONE_MINUTE_MS);
        List<WfInstance> terminal;
        try {
            terminal = queryTerminal(since);
        } catch (Exception e) {
            log.warn("[s5-reconcile] 终态实例扫描失败，本轮跳过", e);
            return;
        }
        int fixed = 0;
        for (WfInstance inst : terminal) {
            try {
                // 逐个确认：引擎仍 active 才算漂移（已结束/已清理的跳过，避免无谓 delete 调用）
                if (processService.isProcessInstanceEnded(inst.getEngineInstId())) {
                    continue;
                }
                processService.deleteProcessInstance(inst.getEngineInstId(), "常驻漂移对账-业务已终态");
                fixed++;
                log.info("[s5-reconcile] 终态漂移已修复. instId={}, engineInstId={}, status={}",
                    inst.getId(), inst.getEngineInstId(), inst.getStatus());
            } catch (Exception e) {
                log.warn("[s5-reconcile] 单实例修复失败（跳过）. instId={}, engineInstId={}, err={}",
                    inst.getId(), inst.getEngineInstId(), e.getMessage());
            }
        }
        if (fixed > 0) {
            log.warn("[s5-reconcile] 本轮修复漂移实例数={}（窗口 {} 分钟，需关注事件反写是否异常）",
                fixed, WINDOW_MINUTES);
        }
    }

    /** 查询时间窗内进入终态的实例（package-private 便于测试替换，避开纯单测中 MyBatis-Plus lambda 缓存未初始化问题） */
    List<WfInstance> queryTerminal(Date since) {
        return instanceMapper.selectList(new LambdaQueryWrapper<WfInstance>()
            .in(WfInstance::getStatus, WfInstance.STATUS_APPROVED,
                WfInstance.STATUS_REJECTED, WfInstance.STATUS_CANCELED)
            .isNotNull(WfInstance::getEngineInstId)
            .ge(WfInstance::getEndTime, since)
            .eq(WfInstance::getIsTest, 0));
    }
}
