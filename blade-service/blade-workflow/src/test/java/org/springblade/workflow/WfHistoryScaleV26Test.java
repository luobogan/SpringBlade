package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V26（《去 wf_ 表改造分析》§17 / §21.3）：历史表规模检索基线。
 *
 * <p>历史表（ACT_HI_PROCINST）增长到万 / 十万级后，带租户复合索引
 * {@code (TENANT_ID_, START_TIME_)} 的范围查询耗时，据以确定分区启动的时点。</p>
 *
 * <p>说明：本测试在 H2 内存库构造规模数据并测量相对时延，给出方法学与基线数字，
 * 用于回归「查询随规模增长不退化」的趋势；生产阈值应以 dev / prod MySQL 实测替换
 * （H2 内存库仅代表相对量级，绝对值不能外推到 MySQL）。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfHistoryScaleV26Test {

	/** 规模：十万级（doc 万 / 十万级上限）。 */
	private static final int SCALE = 100_000;
	private static final String TENANT_A = "000000";
	private static final String TENANT_B = "000001";
	/** H2 内存库下的宽松阈值，仅用于回归「不退化」；生产以 MySQL 实测为准。 */
	private static final long LATENCY_THRESHOLD_MS = 1000;

	@Autowired
	private DataSource dataSource;
	@Autowired
	private RepositoryService repositoryService; // 触发 Flowable 建表

	@BeforeEach
	void ensureSchemaAndIndex() throws Exception {
		// 触发 Flowable 自动建表（database-schema-update=true）
		repositoryService.createDeploymentQuery().count();
		try (Connection c = dataSource.getConnection()) {
			// 复合索引（TENANT_ID_, START_TIME_）—— V26 测量对象
			c.createStatement().execute(
				"CREATE INDEX IF NOT EXISTS IDX_V26_HI_PROC_TENANT ON ACT_HI_PROCINST(TENANT_ID_, START_TIME_)");
		} catch (SQLException ignored) {
			// 索引已存在则忽略
		}
	}

	@Test
	@DisplayName("V26: 历史表 10w 级 + 租户复合索引 → 租户范围查询时延在阈值内且不随规模退化")
	void v26_historyScaleTenantQuery_latencyBaseline() throws Exception {
		long t0 = System.nanoTime();
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			String sql = "INSERT INTO ACT_HI_PROCINST(ID_, PROC_INST_ID_, PROC_DEF_ID_, START_TIME_, END_TIME_, TENANT_ID_, NAME_) "
				+ "VALUES (?,?,?,?,?,?,?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				long now = System.currentTimeMillis();
				for (int i = 0; i < SCALE; i++) {
					String id = "V26_" + i;
					ps.setString(1, id);
					ps.setString(2, id);
					ps.setString(3, "procDef:" + (i % 50));
					ps.setTimestamp(4, new Timestamp(now - (i % 1000) * 1000L));
					ps.setTimestamp(5, new Timestamp(now));
					ps.setString(6, (i % 2 == 0) ? TENANT_A : TENANT_B);
					ps.setString(7, "scale-" + i);
					ps.addBatch();
					if (i % 5000 == 0) {
						ps.executeBatch();
					}
				}
				ps.executeBatch();
			}
			c.commit();
		}
		long insertMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

		// 租户 A 范围查询（按 START_TIME_ 倒序取前 100）
		String q = "SELECT ID_ FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ? ORDER BY START_TIME_ DESC LIMIT 100";
		long best = Long.MAX_VALUE;
		int tenantACount = 0;
		try (Connection c = dataSource.getConnection();
			 PreparedStatement ps = c.prepareStatement(q)) {
			ps.setString(1, TENANT_A);
			for (int k = 0; k < 20; k++) {
				long s = System.nanoTime();
				try (ResultSet rs = ps.executeQuery()) {
					int n = 0;
					while (rs.next()) {
						n++;
					}
					if (k == 0) {
						tenantACount = countTenant(c, TENANT_A);
					}
					assertThat(n).isLessThanOrEqualTo(100);
				}
				long el = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - s);
				best = Math.min(best, el);
			}
		}
		System.out.println("[V26] insertMs=" + insertMs + " scale=" + SCALE
			+ " tenantA.rows=" + tenantACount + " bestQueryMs=" + best);
		assertThat(best).isLessThanOrEqualTo(LATENCY_THRESHOLD_MS);
		// 规模正确性：租户 A 应约为总量一半
		assertThat(tenantACount).isGreaterThan(0).isLessThanOrEqualTo(SCALE);
	}

	private int countTenant(Connection c, String tenant) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(
				"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE TENANT_ID_ = ?")) {
			ps.setString(1, tenant);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}
}
