package org.springblade.workflow.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ManagementService;
import org.flowable.job.api.Job;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * S3 timer 业务侧轮询器（2026-11-18）：扫描引擎已到期的 timer job 并手动执行，<b>绕开 AsyncExecutor</b>。
 *
 * <p><b>技术依据</b>（{@code S3TimerJobProbeTest} 实测）：AsyncExecutor 关闭时，Flowable 仍会为
 * timer 边界/捕获事件创建 timer job（写入 {@code ACT_RU_TIMER_JOB}），只是没有执行器消费。
 * 业务侧用 {@link ManagementService#moveTimerToExecutableJob(String)} 把 timer job 迁到
 * {@code ACT_RU_JOB}，再 {@link ManagementService#executeJob(String)} 执行，即可驱动 token 沿 timer 出口流转。</p>
 *
 * <p><b>安全护栏</b>：</p>
 * <ul>
 *   <li>独立开关 {@code blade.workflow.compat.s3-timer-enabled} 默认 false，本类 bean 都不存在；</li>
 *   <li>与 AsyncExecutor <b>互斥</b>：开启前须确认 FlowableConfig 仍设 asyncExecutorActivate=false，
 *       否则两侧会重复消费同一 job（Flowable 靠锁避免并发，但重复触发风险由配置纪律保证）；</li>
 *   <li>D9 选主：MySQL 命名锁 {@code blade_wf_timer_scan}，多副本仅一个扫描；</li>
 *   <li>is_test 隔离：JOIN ACT_HI_PROCINST 过滤 {@code IS_TEST_=0}，测试态 timer 不被业务扫描触发；</li>
 *   <li>单条失败不影响其余；不主动重试（Flowable 的 RETRIES_ 递减由引擎自身管理）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blade.workflow.compat.s3-timer-enabled", havingValue = "true", matchIfMissing = false)
public class WfTimerJobScanner {

    /** 单批扫描上限（与 WfTimeoutJob 同口径） */
    private static final int BATCH_LIMIT = 200;
    /** D9 选主锁名（与超时扫描锁区分） */
    private static final String LOCK_NAME = "blade_wf_timer_scan";

    private final ManagementService managementService;
    private final JdbcTemplate jdbcTemplate;
    private final WfFlowtableCompatProperties compat;

    /** 定时扫描（默认 10 分钟一次，首次延迟 1 分钟；与超时扫描错峰由部署方配置） */
    @Scheduled(initialDelayString = "${blade.workflow.timer-initial-delay-ms:60000}",
        fixedDelayString = "${blade.workflow.timer-scan-ms:600000}")
    public void scan() {
        if (!compat.isS3TimerEnabled()) {
            return;
        }
        jdbcTemplate.execute((Connection conn) -> {
            if (!acquireLock(conn)) {
                return null;
            }
            try {
                doScan();
            } finally {
                releaseLock(conn);
            }
            return null;
        });
    }

    /** 实际扫描逻辑（已持有选主锁）；package-private 便于同包测试直接驱动（H2 无 GET_LOCK） */
    void doScan() {
        Date now = new Date();
        // 到期 job：DUEDATE_ <= now 且未被锁定（LOCK_EXP_TIME_ 为空或已过期）。
        // is_test 过滤：生产库 ACT_HI_PROCINST 有业务双写列 IS_TEST_，用它隔离测试态；
        // 纯净引擎库（H2/新部署）无此列时回退到不带过滤的 SQL —— 测试态 timer 由部署期
        // neutralizeForTest 降级为 manualTask，本就不产生 timer job，回退不构成隔离缺口。
        String baseSql = "SELECT t.ID_ FROM ACT_RU_TIMER_JOB t "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = t.PROCESS_INSTANCE_ID_ "
            + "WHERE t.DUEDATE_ <= ? "
            + "AND (t.LOCK_EXP_TIME_ IS NULL OR t.LOCK_EXP_TIME_ < ?) ";
        String withTestFilter = baseSql + "AND COALESCE(p.IS_TEST_, 0) = 0 ";
        String order = "ORDER BY t.DUEDATE_ LIMIT " + BATCH_LIMIT;
        List<String> jobIds;
        try {
            jobIds = jdbcTemplate.queryForList(withTestFilter + order, String.class, now, now);
        } catch (Exception e) {
            log.debug("[s3-timer] IS_TEST_ 列不可用（纯净引擎库），回退无测试态过滤扫描. err={}", e.getMessage());
            try {
                jobIds = jdbcTemplate.queryForList(baseSql + order, String.class, now, now);
            } catch (Exception e2) {
                log.warn("[s3-timer] 到期 timer job 扫描失败，本轮跳过", e2);
                return;
            }
        }
        if (jobIds.isEmpty()) {
            return;
        }
        for (String timerJobId : jobIds) {
            try {
                Job executable = managementService.moveTimerToExecutableJob(timerJobId);
                managementService.executeJob(executable.getId());
                log.info("[s3-timer] timer job 已执行. timerJobId={}, procInstId={}",
                    timerJobId, executable.getProcessInstanceId());
            } catch (Exception e) {
                // 常见：job 已被其它执行器消费（并发/AsyncExecutor 误开）→ warn 而非 error，单条失败不阻断
                log.warn("[s3-timer] timer job 执行失败（跳过，下轮可重试）. timerJobId={}, err={}",
                    timerJobId, e.getMessage());
            }
        }
    }

    private boolean acquireLock(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            ps.setString(1, LOCK_NAME);
            ps.setInt(2, 0);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            log.warn("[s3-timer] 获取选主锁失败，本轮跳过. lock={}", LOCK_NAME, e);
            return false;
        }
    }

    private void releaseLock(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, LOCK_NAME);
            ps.executeQuery();
        } catch (SQLException e) {
            log.warn("[s3-timer] 释放选主锁失败. lock={}", LOCK_NAME, e);
        }
    }
}
