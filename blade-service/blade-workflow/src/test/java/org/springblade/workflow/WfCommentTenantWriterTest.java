package org.springblade.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.service.helper.WfCommentTenantWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D8 写侧单测（V22 配套）：{@link WfCommentTenantWriter#apply} 从
 * {@code ACT_HI_PROCINST.TENANT_ID_} 反查租户、幂等回填 {@code ACT_HI_COMMENT.TENANT_ID_}。
 *
 * <p>内存 H2 实建两张最小表，验证：① 正常回填；② 幂等（不覆盖既有值）；
 * ③ 实例无租户时跳过；④ 开关关闭时空操作。</p>
 */
class WfCommentTenantWriterTest {

	private JdbcTemplate jdbc;
	private WfCommentTenantWriter writer;

	@BeforeEach
	void setUp() {
		SimpleDriverDataSource ds = new SimpleDriverDataSource();
		ds.setDriverClass(org.h2.Driver.class);
		ds.setUrl("jdbc:h2:mem:commenttenant;DB_CLOSE_DELAY=-1");
		ds.setUsername("sa");
		ds.setPassword("");
		this.jdbc = new JdbcTemplate(ds);
		this.writer = new WfCommentTenantWriter(jdbc);
		ReflectionTestUtils.setField(writer, "enabled", true);

		jdbc.execute("CREATE TABLE ACT_HI_COMMENT ("
			+ "ID_ VARCHAR(64), TENANT_ID_ VARCHAR(64))");
		jdbc.execute("CREATE TABLE ACT_HI_PROCINST ("
			+ "PROC_INST_ID_ VARCHAR(64), TENANT_ID_ VARCHAR(64))");
	}

	@AfterEach
	void tearDown() {
		jdbc.execute("DROP TABLE IF EXISTS ACT_HI_COMMENT");
		jdbc.execute("DROP TABLE IF EXISTS ACT_HI_PROCINST");
	}

	@Test
	@DisplayName("D8-写侧回填: 意见写入后从实例反查租户回填 TENANT_ID_")
	void apply_fillsTenantFromProcinst() {
		jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES ('eng-1','000000')");
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES ('c1','')");

		writer.apply("c1", "eng-1");

		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_COMMENT WHERE ID_ = 'c1'", String.class);
		assertThat(tenant).isEqualTo("000000");
	}

	@Test
	@DisplayName("D8-幂等: 已有租户的评论不被覆盖")
	void apply_isIdempotent_neverOverwrites() {
		jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES ('eng-1','000000')");
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES ('c1','111111')");

		writer.apply("c1", "eng-1");

		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_COMMENT WHERE ID_ = 'c1'", String.class);
		assertThat(tenant).isEqualTo("111111");
	}

	@Test
	@DisplayName("D8-无租户实例: 实例 TENANT_ID_ 为空时跳过，列保持为空")
	void apply_skipsWhenProcinstTenantBlank() {
		jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES ('eng-1','')");
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES ('c1','')");

		writer.apply("c1", "eng-1");

		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_COMMENT WHERE ID_ = 'c1'", String.class);
		assertThat(tenant).isNullOrEmpty();
	}

	@Test
	@DisplayName("D8-实例不存在: 反查不到实例时安全跳过（不抛错）")
	void apply_skipsWhenProcinstMissing() {
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES ('c1','')");

		writer.apply("c1", "eng-missing");

		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_COMMENT WHERE ID_ = 'c1'", String.class);
		assertThat(tenant).isNullOrEmpty();
	}

	@Test
	@DisplayName("D8-开关关闭: 空操作，不触碰数据库")
	void apply_noopWhenDisabled() {
		ReflectionTestUtils.setField(writer, "enabled", false);
		jdbc.update("INSERT INTO ACT_HI_PROCINST VALUES ('eng-1','000000')");
		jdbc.update("INSERT INTO ACT_HI_COMMENT VALUES ('c1','')");

		writer.apply("c1", "eng-1");

		String tenant = jdbc.queryForObject(
			"SELECT TENANT_ID_ FROM ACT_HI_COMMENT WHERE ID_ = 'c1'", String.class);
		assertThat(tenant).isNullOrEmpty();
	}
}
