package org.springblade.workflow.job;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.DeadLetterJobVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S5 前置兜底：deadletter 死信作业告警扫描器（2026-11-18）。
 *
 * <p>方案C §3.4 三项兜底之一（作业积压/失败告警）：定时扫描 {@code ACT_RU_DEADLETTER_JOB}，
 * 非空即 {@code log.error} + 经 {@link INoticeClient} 投递管理员告警。死信非空 = 作业重试耗尽 =
 * 台账可能已漏更新，<b>需人工介入</b>——因此本扫描器<b>只告警不自动重投</b>（避免掩盖真实故障）。
 * 人工处置入口见 {@link IProcessService#retryDeadLetterJob(String, int)} / {@link IProcessService#deleteDeadLetterJob(String)}。</p>
 *
 * <p><b>安全护栏</b>：独立开关默认 false（bean 都不存在）；D9 选主；告警接收人由
 * {@code blade.workflow.deadletter-alarm-user-ids} 配置，为空则只记日志不投递通知（零外部依赖）；
 * 通知失败仅 warn，不影响引擎。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "blade.workflow.compat.deadletter-alarm-enabled", havingValue = "true", matchIfMissing = false)
public class WfDeadLetterJobScanner {

    private static final String LOCK_NAME = "blade_wf_deadletter_scan";

    private final JdbcTemplate jdbcTemplate;
    private final IProcessService processService;
    private final INoticeClient noticeClient;
    private final WfFlowtableCompatProperties compat;

    @Value("${blade.workflow.deadletter-alarm-user-ids:}")
    private String alarmUserIds;

    public WfDeadLetterJobScanner(JdbcTemplate jdbcTemplate, IProcessService processService,
                                  INoticeClient noticeClient, WfFlowtableCompatProperties compat) {
        this.jdbcTemplate = jdbcTemplate;
        this.processService = processService;
        this.noticeClient = noticeClient;
        this.compat = compat;
    }

    /** 定时扫描（默认 10 分钟一次，首次延迟 1 分钟） */
    @Scheduled(initialDelayString = "${blade.workflow.deadletter-scan-ms:600000}",
        fixedDelayString = "${blade.workflow.deadletter-scan-ms:600000}")
    public void scan() {
        if (!compat.isDeadletterAlarmEnabled()) {
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

    /** 实际告警逻辑；package-private 便于同包测试直驱（H2 无 GET_LOCK） */
    void doScan() {
        List<DeadLetterJobVO> deadLetters;
        try {
            deadLetters = processService.listDeadLetterJobs();
        } catch (Exception e) {
            log.warn("[deadletter-alarm] 死信作业查询失败，本轮跳过", e);
            return;
        }
        if (deadLetters.isEmpty()) {
            return;
        }
        // ① 日志告警（始终执行，是无外部依赖的兜底）
        log.error("[deadletter-alarm] 发现 {} 个死信作业，台账可能漏更新，需人工介入. jobs={}",
            deadLetters.size(), deadLetters.stream().map(DeadLetterJobVO::getJobId).collect(Collectors.toList()));
        // ② 通知告警（配置了接收人才投递）
        pushNotice(deadLetters);
    }

    private void pushNotice(List<DeadLetterJobVO> deadLetters) {
        if (alarmUserIds == null || alarmUserIds.isBlank()) {
            log.debug("[deadletter-alarm] 未配置告警接收人（deadletter-alarm-user-ids），仅日志告警");
            return;
        }
        List<Long> userIds;
        try {
            userIds = java.util.Arrays.stream(alarmUserIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("[deadletter-alarm] 告警接收人配置非法（应为逗号分隔的用户ID）. value={}", alarmUserIds);
            return;
        }
        if (userIds.isEmpty()) {
            return;
        }
        try {
            NoticeSendDTO dto = new NoticeSendDTO();
            dto.setTenantId("000000");
            dto.setUserIds(userIds);
            dto.setContentType(4);
            dto.setBizRefType("WF_DEADLETTER");
            dto.setBizRefId(String.valueOf(deadLetters.size()));
            dto.setContent("【流程告警】检测到 " + deadLetters.size() + " 个死信作业，流程台账可能漏更新，请及时处理");
            R<Boolean> r = noticeClient.sendToUsers(dto);
            if (r == null || !Boolean.TRUE.equals(r.getData())) {
                log.warn("[deadletter-alarm] 死信告警通知投递未成功. resp={}", r);
            }
        } catch (Exception e) {
            // 通知失败不影响引擎与告警日志
            log.warn("[deadletter-alarm] 死信告警通知投递异常（已记录日志告警）. err={}", e.getMessage());
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
            log.warn("[deadletter-alarm] 获取选主锁失败，本轮跳过. lock={}", LOCK_NAME, e);
            return false;
        }
    }

    private void releaseLock(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, LOCK_NAME);
            ps.executeQuery();
        } catch (SQLException e) {
            log.warn("[deadletter-alarm] 释放选主锁失败. lock={}", LOCK_NAME, e);
        }
    }
}
