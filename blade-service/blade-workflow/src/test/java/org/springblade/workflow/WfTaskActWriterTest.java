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
 *   <li>D18（2026-11-10 起）：原 N:1 守卫已删除 —— 同一 engineTaskId 多条台账也<b>照常写回 ACT</b>
 *       （扩展行从不调用 sync，旧守卫只会造成「抄送后审批」状态漂移，见 WfCirculateDriftReproTest）；</li>
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
		// N:1 守卫合并统计两表（去 wf_ 表 · 2026-11-09）
		jdbc.execute("CREATE TABLE wf_ext_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64), "
			+ "type INT, is_deleted TINYINT DEFAULT 0)");
		for (String t : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			jdbc.execute("CREATE TABLE " + t + " ("
				+ "ID_ VARCHAR(64) PRIMARY KEY, BUSINESS_STATUS_ VARCHAR(32), IS_TEST_ TINYINT, "
				+ "ORIGINAL_USER_ VARCHAR(64), SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, "
				+ "TIMEOUT_HANDLED_ TINYINT, BIZ_TASK_ID_ BIGINT, BIZ_ASSIGNEE_ BIGINT, TENANT_ID_ VARCHAR(32), "
				// DUE_DATE_ 必须与生产 ACT_* 表结构一致：WfTaskActWriter 的 UPDATE 会写该列，
				// 缺列 → SQLException → 被写入器 catch 吞掉 → 两张表都没写（列于 4724fd1 加入写入、此处补齐建表）
				+ "DUE_DATE_ TIMESTAMP)");
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
	@DisplayName("D18：守卫删除后，同一引擎任务多条台账也照常写回 ACT 业务列")
	void syncsWhenCountersignSharesEngineTaskAfterGuardRemoval() {
		seedTask("T2", 1L);
		jdbc.update("INSERT INTO wf_task (id, engine_task_id) VALUES (?, ?)", 2L, "T2");
		writer.sync(task("T2"));
		// 旧守卫会因 siblings>1 跳过；D18 删除后统一委托 WfTaskActDirectWriter，仍写回两表
		assertSynced("T2");
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

	/**
	 * 批次 2 / 转办语义护栏：转办只换办理人，<b>不得改 BIZ_TASK_ID_</b>。
	 *
	 * <p>旧实现是「原任务置 DONE + 新建同 engineTaskId 的 TODO 行」，在 ACT 单行模型下
	 * 等于原地覆盖，会把 BIZ_TASK_ID_ 换成新值 → 前端已打开的办理页失效。批次 2 改为
	 * 只更新 BIZ_ASSIGNEE_/ORIGINAL_USER_，bizTaskId 保持不变。</p>
	 */
	@Test
	@DisplayName("转办：writePartial 只换办理人，BIZ_TASK_ID_ 与业务状态保持不变")
	void partialUpdateKeepsBizTaskIdWhenForwarding() {
		org.springblade.workflow.service.helper.WfTaskActDirectWriter direct =
			new org.springblade.workflow.service.helper.WfTaskActDirectWriter(jdbc);
		seedTask("T4", 1L);

		// 建任务：全量写入（TODO + 原办理人）
		org.springblade.workflow.service.helper.TaskBizRecord rec =
			new org.springblade.workflow.service.helper.TaskBizRecord();
		rec.setBizTaskId(2104055356658364418L);
		rec.setEngineTaskId("T4");
		rec.setAssignee(1123598821738675201L);
		rec.setStatus(WfTask.STATUS_TODO);
		rec.setIsTest(0);
		direct.writeFull(rec);

		// 转办：只改办理人两列
		java.util.LinkedHashMap<String, Object> cols = new java.util.LinkedHashMap<>();
		cols.put("BIZ_ASSIGNEE_", 998877665544332211L);
		cols.put("ORIGINAL_USER_", "1123598821738675201");
		direct.writePartial("T4", cols);

		for (String table : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			assertEquals(Long.valueOf(2104055356658364418L), jdbc.queryForObject(
				"SELECT BIZ_TASK_ID_ FROM " + table + " WHERE ID_=?", Long.class, "T4"),
				table + " BIZ_TASK_ID_ 不得被转办覆盖");
			assertEquals(Long.valueOf(998877665544332211L), jdbc.queryForObject(
				"SELECT BIZ_ASSIGNEE_ FROM " + table + " WHERE ID_=?", Long.class, "T4"),
				table + " BIZ_ASSIGNEE_ 应为转办目标人");
			assertEquals("TODO", jdbc.queryForObject(
				"SELECT BUSINESS_STATUS_ FROM " + table + " WHERE ID_=?", String.class, "T4"),
				table + " 业务状态不应被转办改动");
		}
	}
}
