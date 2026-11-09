package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.WfTaskResidueProperties;
import org.springblade.workflow.entity.WfExtTask;
import org.springblade.workflow.service.helper.WfTaskActReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「读 wf_ext_task 扩展任务段」开关（去 wf_ 表 · P6 退役第一台阶）的回归测试。
 *
 * <p>纯 JDBC + H2，无 Spring 容器 / 无 Flowable。</p>
 *
 * <p><b>【2026-11-09 改造】</b>扩展任务已从 {@code wf_task} 迁至 {@code wf_ext_task}，
 * 判据由 {@code status IN (7,8,11) OR engine_task_id 为空} 改为
 * 不可变的 {@code type} 集合 + 自身 {@code status}。本测试覆盖：</p>
 * <ol>
 *   <li>开关默认开启：角标 = ACT 计数 + wf_ext_task 扩展任务计数；</li>
 *   <li>开关关闭（纯 ACT 单源）：扩展任务不计入 —— 与列表侧同口径；</li>
 *   <li>待办段判据 {@code type IN (2,6) AND status=0}；</li>
 *   <li>已办段判据 {@code type IN (1,3,4,5)}（协办/抄送/传阅/已读，未读也进已办侧）；</li>
 *   <li>{@code type=7} 自由流程装饰行不进列表、不计数；</li>
 *   <li>A 类 wf_task 台账行（引擎任务本体）不再被扩展段读取；</li>
 *   <li>逻辑删除 / 测试态 / 草稿实例三类排除；</li>
 *   <li>开关类手动 new 时默认值仍为 true。</li>
 * </ol>
 */
class WfTaskResidueMergeTest {

	private JdbcTemplate jdbc;
	private WfTaskActReader reader;
	private WfTaskResidueProperties props;

	private static final Long USER = 100L;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_ext_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);

		jdbc.execute("CREATE TABLE ACT_RU_TASK (ID_ VARCHAR(64) PRIMARY KEY, BIZ_ASSIGNEE_ BIGINT, "
			+ "IS_TEST_ TINYINT, TENANT_ID_ VARCHAR(32), BIZ_TASK_ID_ BIGINT)");
		jdbc.execute("CREATE TABLE ACT_HI_TASKINST (ID_ VARCHAR(64) PRIMARY KEY, BIZ_ASSIGNEE_ BIGINT, "
			+ "IS_TEST_ TINYINT, TENANT_ID_ VARCHAR(32), BIZ_TASK_ID_ BIGINT, END_TIME_ TIMESTAMP)");
		// A 类台账：仅引擎任务本体，扩展段不应再读它（造几行验证不串味）
		jdbc.execute("CREATE TABLE wf_task (id BIGINT PRIMARY KEY, assignee BIGINT, status INT, "
			+ "is_test TINYINT, tenant_id VARCHAR(32), inst_id BIGINT, engine_task_id VARCHAR(64), "
			+ "is_deleted TINYINT DEFAULT 0)");
		jdbc.execute("CREATE TABLE wf_ext_task (id BIGINT PRIMARY KEY, assignee BIGINT, type INT, "
			+ "status INT, is_test TINYINT, tenant_id VARCHAR(32), inst_id BIGINT, "
			+ "biz_task_id BIGINT, engine_task_id VARCHAR(64), is_deleted TINYINT DEFAULT 0)");
		jdbc.execute("CREATE TABLE wf_instance (id BIGINT PRIMARY KEY, status INT, is_deleted TINYINT)");

		reader = new WfTaskActReader(jdbc);
		props = new WfTaskResidueProperties();
		ReflectionTestUtils.setField(reader, "residueMerge", props);
	}

	private void actTodo(String engineId, long bizTaskId, long assignee) {
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_, BIZ_ASSIGNEE_, IS_TEST_, BIZ_TASK_ID_) VALUES (?,?,0,?)",
			engineId, assignee, bizTaskId);
	}

	private void actDone(String engineId, long bizTaskId, long assignee) {
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_, BIZ_ASSIGNEE_, IS_TEST_, BIZ_TASK_ID_, END_TIME_) "
			+ "VALUES (?,?,0,?,?)", engineId, assignee, bizTaskId, new Timestamp(System.currentTimeMillis()));
	}

	/** 扩展任务行（默认待处理 status=0、未逻辑删除、无宿主实例） */
	private void extTask(long id, long assignee, int type) {
		jdbc.update("INSERT INTO wf_ext_task (id, assignee, type, status, is_test, engine_task_id) "
			+ "VALUES (?,?,?,0,0,'')", id, assignee, type);
	}

	private void extTaskWithStatus(long id, long assignee, int type, int status) {
		jdbc.update("INSERT INTO wf_ext_task (id, assignee, type, status, is_test, engine_task_id) "
			+ "VALUES (?,?,?,?,0,'')", id, assignee, type, status);
	}

	private void extTaskWithInst(long id, long assignee, int type, int status, long instId) {
		jdbc.update("INSERT INTO wf_ext_task (id, assignee, type, status, is_test, inst_id, engine_task_id) "
			+ "VALUES (?,?,?,?,0,?,'')", id, assignee, type, status, instId);
	}

	/** A 类台账行（引擎任务本体），扩展段不应读它 */
	private void wfTaskLedger(long id, long assignee, int status) {
		jdbc.update("INSERT INTO wf_task (id, assignee, status, is_test, engine_task_id) VALUES (?,?,?,0,'T-ENGINE')",
			id, assignee, status);
	}

	private void wfInstance(long id, int status) {
		jdbc.update("INSERT INTO wf_instance (id, status, is_deleted) VALUES (?,?,0)", id, status);
	}

	private void setMerge(boolean enabled) {
		ReflectionTestUtils.setField(props, "enabled", enabled);
	}

	@Test
	@DisplayName("开关默认开启：角标 = ACT 计数 + wf_ext_task 扩展任务计数")
	void mergesExtTasksWhenEnabled() {
		actTodo("T1", 10L, USER);
		extTask(99L, USER, WfExtTask.TYPE_RESUBMIT); // 待办段：退回重提交
		setMerge(true);

		Map<String, Long> r = reader.countFromAct(USER);

		assertEquals(2L, r.get("todo"), "待办角标应为 ACT 1 条 + 扩展任务 1 条");
		assertEquals(0L, r.get("done"), "已办角标应为 0");
	}

	@Test
	@DisplayName("开关关闭（纯 ACT 单源）：扩展任务不计入，角标只数 ACT_*")
	void skipsExtTasksWhenDisabled() {
		actTodo("T1", 10L, USER);
		extTask(99L, USER, WfExtTask.TYPE_RESUBMIT);
		setMerge(false);

		Map<String, Long> r = reader.countFromAct(USER);

		assertEquals(1L, r.get("todo"), "纯 ACT 单源：待办角标只数 ACT 的 1 条");
	}

	@Test
	@DisplayName("待办段判据：type IN (2,6) AND status=0")
	void todoSegmentTypePredicate() {
		extTask(1L, USER, WfExtTask.TYPE_ADD_SIGN);   // 命中
		extTask(2L, USER, WfExtTask.TYPE_RESUBMIT);   // 命中
		extTask(3L, USER, WfExtTask.TYPE_COADJUTANT); // 已办段，不计待办
		extTaskWithStatus(4L, USER, WfExtTask.TYPE_RESUBMIT, WfExtTask.STATUS_DONE); // 已处理，不计待办
		setMerge(true);

		assertEquals(2L, reader.countFromAct(USER).get("todo"),
			"仅 type=2/6 且 status=0 的行进入待办段");
	}

	@Test
	@DisplayName("已办段判据：type IN (1,3,4,5)，协办/抄送/传阅/已读未读也进已办侧")
	void doneSegmentTypePredicate() {
		actDone("T2", 11L, USER);
		extTask(1L, USER, WfExtTask.TYPE_COADJUTANT); // 协办 → 原 status=7
		extTask(2L, USER, WfExtTask.TYPE_CIRCULATE);  // 抄送 → 原 status=8
		extTask(3L, USER, WfExtTask.TYPE_READ);       // 传阅 → 原 status=11
		extTask(4L, USER, WfExtTask.TYPE_READED);     // 已读 → 原 status=11
		setMerge(true);

		assertEquals(5L, reader.countFromAct(USER).get("done"),
			"ACT 1 条 + 4 条扩展任务（未读也进已办侧，与原 status=7/8/11 行为一致）");
		assertEquals(0L, reader.countFromAct(USER).get("todo"),
			"协办/抄送/传阅/已读不属待办段");
	}

	@Test
	@DisplayName("type=7 自由流程步骤名装饰行：不进列表、不计数（否则多算行）")
	void excludesFreeFlowDecorationRows() {
		extTask(1L, USER, WfExtTask.TYPE_FREE_FLOW);
		extTaskWithStatus(2L, USER, WfExtTask.TYPE_FREE_FLOW, WfExtTask.STATUS_DONE);
		setMerge(true);

		Map<String, Long> r = reader.countFromAct(USER);
		assertEquals(0L, r.get("todo"), "装饰行不是列表条目，不计待办");
		assertEquals(0L, r.get("done"), "装饰行不是列表条目，不计已办");
	}

	@Test
	@DisplayName("A 类 wf_task 台账行（引擎任务本体）：扩展段不再读取")
	void ignoresWfTaskLedgerRows() {
		wfTaskLedger(50L, USER, 0);
		wfTaskLedger(51L, USER, 7);
		setMerge(true);

		Map<String, Long> r = reader.countFromAct(USER);
		assertEquals(0L, r.get("todo"), "wf_task 的 A 类台账行不应被扩展段读到");
		assertEquals(0L, r.get("done"), "wf_task 的 status=7 行也不应再被扩展段读到");
	}

	@Test
	@DisplayName("逻辑删除的行不计入角标（裸 SQL 不会自动过滤 is_deleted）")
	void excludesLogicallyDeletedRows() {
		jdbc.update("INSERT INTO wf_ext_task (id, assignee, type, status, is_test, engine_task_id, is_deleted) "
			+ "VALUES (?,?,?,0,0,'',1)", 70L, USER, WfExtTask.TYPE_RESUBMIT);
		setMerge(true);

		assertEquals(0L, reader.countFromAct(USER).get("todo"),
			"已逻辑删除的行不得计入角标（否则「角标 12、列表 11」割裂）");
	}

	@Test
	@DisplayName("测试态扩展任务不计入角标")
	void excludesTestRows() {
		jdbc.update("INSERT INTO wf_ext_task (id, assignee, type, status, is_test, engine_task_id) "
			+ "VALUES (?,?,?,0,1,'')", 71L, USER, WfExtTask.TYPE_RESUBMIT);
		setMerge(true);

		assertEquals(0L, reader.countFromAct(USER).get("todo"), "is_test=1 的扩展任务不进生产角标");
	}

	@Test
	@DisplayName("草稿实例（status=5）下的扩展任务：被排除，不计入角标")
	void excludesDraftSyntheticTodo() {
		wfInstance(1000L, 5);
		extTaskWithInst(99L, USER, WfExtTask.TYPE_RESUBMIT, WfExtTask.STATUS_PENDING, 1000L);
		setMerge(true);

		assertEquals(0L, reader.countFromAct(USER).get("todo"),
			"草稿待办已移出「待办」，角标不应再计入");
	}

	@Test
	@DisplayName("运行中实例的合成待办（退回发起人重提交）：不得误伤，仍计入角标")
	void keepsRunningSyntheticTodo() {
		wfInstance(2000L, 0);
		extTaskWithInst(98L, USER, WfExtTask.TYPE_RESUBMIT, WfExtTask.STATUS_PENDING, 2000L);
		setMerge(true);

		assertEquals(1L, reader.countFromAct(USER).get("todo"),
			"运行中实例的合成待办是正常待办，草稿排除判据不得误伤它");
	}

	@Test
	@DisplayName("开关类未被 Spring 装配时（手动 new）默认值仍为 true")
	void defaultIsMergeOn() {
		assertTrue(new WfTaskResidueProperties().isEnabled(),
			"默认必须为 true（读扩展任务），否则未装配场景下协办/抄送/传阅会被静默丢弃");
	}
}
