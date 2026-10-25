package org.springblade.workflow.odoc.esign.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springblade.workflow.odoc.esign.service.EsignFlowService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * e签宝签署补偿任务。
 *
 * <p>e签宝官方明确「回调可能丢失」，必须由业务侧补偿：把「进行中」的签署流程与厂商对齐，
 * 全部签完的立即取签署件归档。补偿与回调复用 {@link EsignFlowService} 的同一套逻辑。</p>
 *
 * <p>多副本用 MySQL 命名锁选主（同 {@code WfTimeoutJob} 做法）：非阻塞获取，拿不到即跳过本轮。</p>
 *
 * <p>开关：{@code odoc.esign.enabled} + {@code odoc.esign.compensate.enabled}；
 * 周期：{@code odoc.esign.compensate.scan-ms}（默认 5 分钟）；单批上限 50 条。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsignCompensateJob {

	/** D9 选主锁名（MySQL 实例级命名锁） */
	private static final String LOCK_NAME = "blade_wf_esign_compensate";

	/** 单批扫描上限 */
	private static final int BATCH_LIMIT = 50;

	private final EsignProperties props;
	private final EsignFlowService flowService;
	private final JdbcTemplate jdbcTemplate;

	/** 定时补偿（默认 5 分钟一次） */
	@Scheduled(cron = "${odoc.esign.compensate.cron:0 */5 * * * ?}")
	public void scan() {
		if (!props.isEnabled()) {
			return;
		}
		if (props.getCompensate() != null && !props.getCompensate().isEnabled()) {
			return;
		}
		jdbcTemplate.execute((Connection conn) -> {
			boolean acquired;
			try (PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
				ps.setString(1, LOCK_NAME);
				ps.setInt(2, 0);
				try (ResultSet rs = ps.executeQuery()) {
					acquired = rs.next() && rs.getInt(1) == 1;
				}
			} catch (SQLException e) {
				log.warn("[esign] 获取补偿锁失败. lock={}", LOCK_NAME, e);
				return null;
			}
			if (!acquired) {
				log.debug("[esign] 补偿扫描由其它副本执行，本副本跳过. lock={}", LOCK_NAME);
				return null;
			}
			try {
				int count = flowService.compensate(BATCH_LIMIT);
				if (count > 0) {
					log.info("[esign] 签署补偿扫描完成，处理流程数={}", count);
				}
			} catch (Exception e) {
				log.warn("[esign] 补偿扫描异常", e);
			} finally {
				try (PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
					ps.setString(1, LOCK_NAME);
					ps.executeQuery();
				} catch (SQLException e) {
					log.warn("[esign] 释放补偿锁失败. lock={}", LOCK_NAME, e);
				}
			}
			return null;
		});
	}

}
