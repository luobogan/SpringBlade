package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.service.helper.WfTaskActDirectWriter;
import org.springblade.workflow.service.helper.WfTaskActReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【去 wf_ 表 · 批次 2】direct 模式「读源翻源」回归测试（H2，纯 JDBC，无 Spring 容器/Flowable）。
 *
 * <p><b>背景</b>：批次 1 把「写侧」单路径化（{@code blade.workflow.task-ledger-mode=direct} 时
 * 任务创建不再落 {@code wf_task}，业务列直写 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST}），
 * 但读侧/补偿侧仍有若干处把 {@code wf_task} 当权威源 → 该表为空即<b>静默失效</b>。
 * 本测试锁死本次修复的三条关键语义：</p>
 *
 * <ol>
 *   <li><b>事件反写</b>：{@code markStatusIfTodo} —— 引擎事件驱动把待办置已办/办结，
 *       direct 模式没有 wf_task 行作锚点，改为按引擎 taskId 条件反写 ACT；
 *       条件式保证协办/抄送等其它业务语义不被误改（等价旧实现的「仅待办态才动」）。</li>
 *   <li><b>残留收尾</b>：{@code closeResidualByProcInst} —— 撤销/撤回/归档时关闭残留待办。</li>
 *   <li><b>待办判重</b>：{@code existsTaskFromAct} —— 恒 false 会让每轮 advance 重新生成
 *       {@code BIZ_TASK_ID_}，用户待办 id 漂移（提交报「任务不存在」）。</li>
 * </ol>
 */
class WfLedgerDirectModeTest {

	private JdbcTemplate jdbc;
	private WfTaskActDirectWriter writer;
	private WfTaskActReader reader;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_ledger_direct_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);

		jdbc.execute("CREATE TABLE ACT_HI_PROCINST (ID_ VARCHAR(64) PRIMARY KEY, BUSINESS_ID_ BIGINT)");
		jdbc.execute("CREATE TABLE ACT_RU_TASK ("
			+ "ID_ VARCHAR(64) PRIMARY KEY, PROC_INST_ID_ VARCHAR(64), TASK_DEF_KEY_ VARCHAR(64), "
			+ "ASSIGNEE_ VARCHAR(64), BIZ_ASSIGNEE_ BIGINT, BUSINESS_STATUS_ VARCHAR(32), "
			+ "BIZ_TASK_ID_ BIGINT, IS_TEST_ TINYINT, TIMEOUT_HANDLED_ TINYINT, DUE_DATE_ TIMESTAMP, "
			+ "END_TIME_ TIMESTAMP, CREATE_TIME_ TIMESTAMP, ORIGINAL_USER_ VARCHAR(64), "
			+ "SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, TENANT_ID_ VARCHAR(32))");
		jdbc.execute("CREATE TABLE ACT_HI_TASKINST ("
			+ "ID_ VARCHAR(64) PRIMARY KEY, PROC_INST_ID_ VARCHAR(64), TASK_DEF_KEY_ VARCHAR(64), "
			+ "ASSIGNEE_ VARCHAR(64), BIZ_ASSIGNEE_ BIGINT, BUSINESS_STATUS_ VARCHAR(32), "
			+ "BIZ_TASK_ID_ BIGINT, IS_TEST_ TINYINT, TIMEOUT_HANDLED_ TINYINT, DUE_DATE_ TIMESTAMP, "
			+ "END_TIME_ TIMESTAMP, START_TIME_ TIMESTAMP, ORIGINAL_USER_ VARCHAR(64), "
			+ "SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, TENANT_ID_ VARCHAR(32))");

		writer = new WfTaskActDirectWriter(jdbc);
		reader = new WfTaskActReader(jdbc);
	}

	private void seedProcInst(String procInstId, Long businessId) {
		jdbc.update("INSERT INTO ACT_HI_PROCINST (ID_, BUSINESS_ID_) VALUES (?, ?)", procInstId, businessId);
	}

	private void seedRuTask(String id, String procInstId, String status, Long bizTaskId, Long assignee) {
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_, PROC_INST_ID_, BUSINESS_STATUS_, BIZ_TASK_ID_, BIZ_ASSIGNEE_) "
			+ "VALUES (?, ?, ?, ?, ?)", id, procInstId, status, bizTaskId, assignee);
	}

	private void seedHiTask(String id, String procInstId, String status, Long bizTaskId, boolean ended) {
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_, PROC_INST_ID_, BUSINESS_STATUS_, BIZ_TASK_ID_, END_TIME_) "
			+ "VALUES (?, ?, ?, ?, ?)", id, procInstId, status, bizTaskId, ended ? java.sql.Timestamp.valueOf("2026-11-11 10:00:00") : null);
	}

	private String hiStatus(String id) {
		return jdbc.queryForObject("SELECT BUSINESS_STATUS_ FROM ACT_HI_TASKINST WHERE ID_ = ?", String.class, id);
	}

	// ==================== 1. 事件反写 ====================

	@Test
	@DisplayName("事件反写：待办态 → 已办(DONE)；协办/抄送态不被误改")
	void markStatusIfTodo_onlyTouchesTodo() {
		seedHiTask("T1", "PI1", "TODO", 1001L, false);
		seedHiTask("T2", "PI1", "COADJUTANT", 1002L, false);
		seedHiTask("T3", "PI1", "CIRCULATE", 1003L, false);

		int n = writer.markStatusIfTodo("T1", WfTaskActDirectWriter.statusStr(WfTask.STATUS_DONE));
		assertEquals(1, n, "待办行应被命中 1 次（ACT_HI_TASKINST）");
		assertEquals("DONE", hiStatus("T1"));

		assertEquals(0, writer.markStatusIfTodo("T2", WfTaskActDirectWriter.statusStr(WfTask.STATUS_DONE)));
		assertEquals("COADJUTANT", hiStatus("T2"), "协办语义不得被改成已办");
		assertEquals("CIRCULATE", hiStatus("T3"), "抄送语义不得被改成已办");
	}

	@Test
	@DisplayName("事件反写：幂等——已是目标态再写命中 0 行")
	void markStatusIfTodo_isIdempotent() {
		seedHiTask("T1", "PI1", "DONE", 1001L, true);
		assertEquals(0, writer.markStatusIfTodo("T1", WfTaskActDirectWriter.statusStr(WfTask.STATUS_DONE)));
		assertEquals("DONE", hiStatus("T1"));
	}

	@Test
	@DisplayName("事件反写：任务被引擎删除 → 置办结(FINISHED)")
	void markStatusIfTodo_marksFinished() {
		seedHiTask("T1", "PI1", "TODO", 1001L, true);
		assertEquals(1, writer.markStatusIfTodo("T1", WfTaskActDirectWriter.statusStr(WfTask.STATUS_FINISHED)));
		assertEquals("FINISHED", hiStatus("T1"));
	}

	// ==================== 2. 残留待办收尾 ====================

	@Test
	@DisplayName("残留收尾：未结束的待办/协办 → 办结；已办不动")
	void closeResidualByProcInst_closesUnfinished() {
		seedHiTask("T1", "PI1", "TODO", 1001L, false);
		seedHiTask("T2", "PI1", "COADJUTANT", 1002L, false);
		seedHiTask("T3", "PI1", "DONE", 1003L, true);
		seedHiTask("T4", "PI2", "TODO", 1004L, false);

		int n = writer.closeResidualByProcInst("PI1", WfTaskActDirectWriter.statusStr(WfTask.STATUS_FINISHED));
		assertEquals(2, n);
		assertEquals("FINISHED", hiStatus("T1"));
		assertEquals("FINISHED", hiStatus("T2"));
		assertEquals("DONE", hiStatus("T3"), "已办行不应被残留收尾改动");
		assertEquals("TODO", hiStatus("T4"), "别的实例不受影响");
	}

	// ==================== 3. 待办判重 ====================

	@Test
	@DisplayName("待办判重：同一引擎任务+同一办理人 → 已存在（不再重复生成 BIZ_TASK_ID_）")
	void existsTaskFromAct_findsExisting() {
		seedProcInst("PI1", 777L);
		seedRuTask("T1", "PI1", "TODO", 9001L, 279L);

		assertTrue(reader.existsTaskFromAct(777L, "T1", 279L, WfTask.STATUS_TODO));
	}

	@Test
	@DisplayName("待办判重：引擎任务存在但业务待办未生成 → 必须未命中（否则 insertTask 被跳过）")
	void existsTaskFromAct_requiresBusinessTodo() {
		seedProcInst("PI1", 777L);
		// 引擎任务行已存在（引擎先建），但 BIZ_TASK_ID_ 为空 = 业务待办尚未生成
		seedRuTask("T1", "PI1", "TODO", null, 279L);

		assertFalse(reader.existsTaskFromAct(777L, "T1", 279L, null),
			"无业务待办时不得判为已存在，否则 insertTask 被跳过、业务列永不写入");
	}

	@Test
	@DisplayName("待办判重：办理人不同 / 实例不同 / 任务不存在 → 均未命中")
	void existsTaskFromAct_misses() {
		seedProcInst("PI1", 777L);
		seedRuTask("T1", "PI1", "TODO", 9001L, 279L);

		assertFalse(reader.existsTaskFromAct(777L, "T1", 676L, WfTask.STATUS_TODO), "办理人不同不应命中");
		assertFalse(reader.existsTaskFromAct(888L, "T1", 279L, WfTask.STATUS_TODO), "实例不同不应命中");
		assertFalse(reader.existsTaskFromAct(777L, "T9", 279L, WfTask.STATUS_TODO), "引擎任务不存在不应命中");
	}

	@Test
	@DisplayName("待办判重：不按状态过滤时命中；按非待办状态过滤时不命中")
	void existsTaskFromAct_statusFilter() {
		seedProcInst("PI1", 777L);
		seedRuTask("T1", "PI1", "TODO", 9001L, 279L);

		assertTrue(reader.existsTaskFromAct(777L, "T1", null, null));
		assertFalse(reader.existsTaskFromAct(777L, "T1", null, WfTask.STATUS_COADJUTANT));
	}

	// ==================== 4. 漂移判据 ====================

	@Test
	@DisplayName("漂移判据：只把已绑定业务待办（BIZ_TASK_ID_ 非空）的引擎任务算作已绑定")
	void boundEngineTaskIdsFromAct_onlyBound() {
		seedProcInst("PI1", 777L);
		seedRuTask("T1", "PI1", "TODO", 9001L, 279L);
		seedRuTask("T2", "PI1", "TODO", null, 279L);
		seedRuTask("T3", "PI2", "TODO", 9003L, 279L);

		Set<String> bound = reader.boundEngineTaskIdsFromAct(777L);
		assertTrue(bound.contains("T1"));
		assertFalse(bound.contains("T2"), "无业务待办的引擎任务属 A 类漂移，不算已绑定");
		assertFalse(bound.contains("T3"), "别的实例不算已绑定");
	}

	// ==================== 5. 批次 2 通用读源（10 个读点翻源的公共能力） ====================

	private void seedRuFull(String id, String procInstId, String nodeKey, String status,
							long bizTaskId, long assignee, int isTest) {
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_, PROC_INST_ID_, TASK_DEF_KEY_, BUSINESS_STATUS_, "
				+ "BIZ_TASK_ID_, BIZ_ASSIGNEE_, ASSIGNEE_, IS_TEST_, CREATE_TIME_) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			id, procInstId, nodeKey, status, bizTaskId, assignee, String.valueOf(assignee), isTest,
			java.sql.Timestamp.valueOf("2026-11-11 10:00:00"));
	}

	private void seedHiFull(String id, String procInstId, String nodeKey, String status,
							long bizTaskId, long assignee, boolean ended) {
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_, PROC_INST_ID_, TASK_DEF_KEY_, BUSINESS_STATUS_, "
				+ "BIZ_TASK_ID_, BIZ_ASSIGNEE_, ASSIGNEE_, START_TIME_, END_TIME_) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
			id, procInstId, nodeKey, status, bizTaskId, assignee, String.valueOf(assignee),
			java.sql.Timestamp.valueOf("2026-11-11 10:00:00"),
			ended ? java.sql.Timestamp.valueOf("2026-11-11 11:00:00") : null);
	}

	@Test
	@DisplayName("通用读源：activeTasksFromAct 按 实例/节点/办理人/测试态 精确过滤")
	void activeTasksFromAct_filters() {
		seedProcInst("PI1", 777L);
		seedProcInst("PI2", 888L);
		seedRuFull("T1", "PI1", "NodeA", "TODO", 9001L, 279L, 1);
		seedRuFull("T2", "PI1", "NodeA", "TODO", 9002L, 676L, 0);
		seedRuFull("T3", "PI1", "NodeB", "TODO", 9003L, 279L, 1);
		seedRuFull("T4", "PI1", "NodeA", "DONE", 9004L, 279L, 1);
		seedRuFull("T5", "PI2", "NodeA", "TODO", 9005L, 279L, 1);

		assertEquals(3, reader.activeTasksFromAct(777L, null, null, null).size(), "只取该实例的活跃待办");
		assertEquals(2, reader.activeTasksFromAct(777L, "NodeA", null, null).size());
		assertEquals(1, reader.activeTasksFromAct(777L, "NodeA", 279L, null).size());
		assertEquals(2, reader.activeTasksFromAct(777L, null, null, true).size(), "只取测试态");
		assertEquals(1, reader.activeTasksFromAct(888L, null, null, null).size());
		assertEquals(279L, reader.activeTasksFromAct(777L, "NodeA", 279L, null).get(0).getAssignee().longValue());
	}

	@Test
	@DisplayName("通用读源：activeTaskFromAct 按业务待办 id 重读；已办结返回 null")
	void activeTaskFromAct_reread() {
		seedProcInst("PI1", 777L);
		seedRuFull("T1", "PI1", "NodeA", "TODO", 9001L, 279L, 1);

		WfTask fresh = reader.activeTaskFromAct(9001L);
		assertNotNull(fresh, "活跃待办必须能重读到（旧实现恒 null → 自动测试一步都走不了）");
		assertEquals("T1", fresh.getEngineTaskId());
		assertEquals(WfTask.STATUS_TODO, fresh.getStatus());
		assertEquals(Long.valueOf(777L), fresh.getInstId());
		assertEquals(null, reader.activeTaskFromAct(9999L), "已办结/不存在的待办返回 null（跳过办理）");
	}

	@Test
	@DisplayName("通用读源：countDoneByInstanceFromAct 统计已办任务（判「是否首次提交」）")
	void countDoneByInstanceFromAct_counts() {
		seedProcInst("PI1", 777L);
		seedHiFull("H1", "PI1", "NodeA", "DONE", 9001L, 279L, true);
		seedHiFull("H2", "PI1", "NodeA", "TODO", 9002L, 676L, false);

		assertEquals(1L, reader.countDoneByInstanceFromAct(777L));
		assertEquals(0L, reader.countDoneByInstanceFromAct(888L));
	}

	@Test
	@DisplayName("通用读源：allTasksByInstanceFromAct 含已办行（操作者面板能显示走过的人）")
	void allTasksByInstanceFromAct_includesDone() {
		seedProcInst("PI1", 777L);
		seedHiFull("H1", "PI1", "NodeA", "DONE", 9001L, 279L, true);
		seedHiFull("H2", "PI1", "NodeA", "TODO", 9002L, 676L, false);
		seedHiFull("H3", "PI1", "NodeB", "DONE", 9003L, 888L, true);

		assertEquals(3, reader.allTasksByInstanceFromAct(777L, null).size());
		assertEquals(2, reader.allTasksByInstanceFromAct(777L, "NodeA").size());
	}

	@Test
	@DisplayName("通用读源：residualTasksFromAct 只取未结束的待办/协办（关待办循环）")
	void residualTasksFromAct_onlyUnfinished() {
		seedProcInst("PI1", 777L);
		seedHiFull("H1", "PI1", "NodeA", "TODO", 9001L, 279L, false);
		seedHiFull("H2", "PI1", "NodeA", "COADJUTANT", 9002L, 676L, false);
		seedHiFull("H3", "PI1", "NodeA", "DONE", 9003L, 888L, true);
		seedHiFull("H4", "PI1", "NodeA", "TODO", 9004L, 999L, true);

		assertEquals(2, reader.residualTasksFromAct(777L).size(), "待办+协办且未结束");
	}

	@Test
	@DisplayName("通用读源：lastDoneOperatorFromAct 取最近一位实际办理人（归档人兜底）")
	void lastDoneOperatorFromAct_picksLatest() {
		seedProcInst("PI1", 777L);
		seedHiFull("H1", "PI1", "NodeA", "DONE", 9001L, 279L, true);
		seedHiFull("H2", "PI1", "NodeB", "DONE", 9002L, 676L, true);
		seedHiFull("H3", "PI1", "NodeC", "TODO", 9003L, 888L, false);

		assertNotNull(reader.lastDoneOperatorFromAct(777L), "direct 模式必须取到实际办理人，不能回退发起人");
		assertEquals(null, reader.lastDoneOperatorFromAct(888L));
	}
}
