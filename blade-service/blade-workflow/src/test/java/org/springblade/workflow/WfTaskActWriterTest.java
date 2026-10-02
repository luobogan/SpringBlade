package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * P4/P5 方案 A1：任务业务列双写 {@link WfTaskActWriter} 的回归测试（H2，纯 JDBC，无 Spring 容器/Flowable）。
 *
 * <p>覆盖三条关键行为：
 * <ol>
 *   <li>1:1（单办理人）：业务列同步写回 {@code ACT_RU_TASK} 与 {@code ACT_HI_TASKINST} 两张表；</li>
 *   <li>N:1（会签/或签，同一 engineTaskId 多条 wf_task）：<b>跳过</b>写入，避免多人互相踩踏；</li>
 *   <li>开关关闭：完全空操作。</li>
 * </ol>
 */
class WfTaskActWriterTest {

	private JdbcTemplate jdbc;
	private WfTaskActWriter writer;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_task_act_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);

		jdbc.execute("CREATE TABLE wf_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64))");
		for (String t : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			jdbc.execute("CREATE TABLE " + t + " ("
				+ "ID_ VARCHAR(64) PRIMARY KEY, BUSINESS_STATUS_ VARCHAR(32), IS_TEST_ TINYINT, "
				+ "ORIGINAL_USER_ VARCHAR(64), SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, "
				+ "TIMEOUT_HANDLED_ TINYINT, BIZ_TASK_ID_ BIGINT, BIZ_ASSIGNEE_ BIGINT, TENANT_ID_ VARCHAR(32))");
		}

		writer = new WfTaskActWriter(jdbc);
		ReflectionTestUtils.setField(writer, "enabled", true);
	}

	private void seedTask(String engineTaskId, long wfTaskId) {
		jdbc.update("INSERT INTO wf_task (id, engine_task_id) VALUES (?, ?)", wfTaskId, engineTaskId);
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_) VALUES (?)", engineTaskId);
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_) VALUES (?)", engineTaskId);
	}

	private WfTask task(String engineTaskId) {
		WfTask t = new WfTask();
		t.setId(2104055356658364418L);
		t.setAssignee(1123598821738675201L);
		t.setEngineTaskId(engineTaskId);
		t.setStatus(WfTask.STATUS_DONE);
		t.setIsTest(0);
		t.setOriginalUser(99L);
		t.setSignOrder(1);
		t.setViewTime(new Date());
		t.setTimeoutHandled(0);
		return t;
	}

	private void assertSynced(String engineTaskId) {
		for (String table : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			assertEquals("DONE", jdbc.queryForObject(
				"SELECT BUSINESS_STATUS_ FROM " + table + " WHERE ID_=?", String.class, engineTaskId),
				table + " 子状态应为 DONE");
			assertEquals("99", jdbc.queryForObject(
				"SELECT ORIGINAL_USER_ FROM " + table + " WHERE ID_=?", String.class, engineTaskId),
				table + " 原处理人应回写");
			assertEquals(Integer.valueOf(1), jdbc.queryForObject(
				"SELECT SIGN_ORDER_ FROM " + table + " WHERE ID_=?", Integer.class, engineTaskId),
				table + " 会签关系应回写");
			assertNotNull(jdbc.queryForObject(
				"SELECT VIEW_TIME_ FROM " + table + " WHERE ID_=?", java.sql.Timestamp.class, engineTaskId),
				table + " 查看时间应回写");
			assertEquals(Integer.valueOf(0), jdbc.queryForObject(
				"SELECT IS_TEST_ FROM " + table + " WHERE ID_=?", Integer.class, engineTaskId));
			// 业务任务ID：翻源后列表仍吐此 ID，操作接口（approve/转办/退回）链路不变
			assertEquals(Long.valueOf(2104055356658364418L), jdbc.queryForObject(
				"SELECT BIZ_TASK_ID_ FROM " + table + " WHERE ID_=?", Long.class, engineTaskId),
				table + " 业务任务ID(wf_task.id)应回写");
			assertEquals(Long.valueOf(1123598821738675201L), jdbc.queryForObject(
				"SELECT BIZ_ASSIGNEE_ FROM " + table + " WHERE ID_=?", Long.class, engineTaskId),
				table + " 办理人(wf_task.assignee)应回写");
		}
	}

	private void assertUntouched(String engineTaskId) {
		for (String table : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			assertNull(jdbc.queryForObject(
				"SELECT BUSINESS_STATUS_ FROM " + table + " WHERE ID_=?", String.class, engineTaskId),
				table + " 应保持未写入");
		}
	}

	@Test
	@DisplayName("1:1 单办理人：业务列同步写回 ACT_RU_TASK 与 ACT_HI_TASKINST")
	void syncsBothTablesWhenOneToOne() {
		seedTask("T1", 1L);
		writer.sync(task("T1"));
		assertSynced("T1");
	}

	@Test
	@DisplayName("N:1 会签（同一引擎任务多条 wf_task）：跳过写入，避免多人互相踩踏")
	void skipsWhenCountersignSharesEngineTask() {
		seedTask("T2", 1L);
		jdbc.update("INSERT INTO wf_task (id, engine_task_id) VALUES (?, ?)", 2L, "T2");
		writer.sync(task("T2"));
		assertUntouched("T2");
	}

	@Test
	@DisplayName("开关关闭：完全空操作")
	void noopWhenDisabled() {
		ReflectionTestUtils.setField(writer, "enabled", false);
		seedTask("T3", 1L);
		writer.sync(task("T3"));
		assertUntouched("T3");
	}

	@Test
	@DisplayName("engineTaskId 为空（合成待办，如退回发起人）：跳过")
	void skipsWhenNoEngineTask() {
		WfTask t = task(null);
		writer.sync(t);
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ACT_RU_TASK", Integer.class));
	}
}
