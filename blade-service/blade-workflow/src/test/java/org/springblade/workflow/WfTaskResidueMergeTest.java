package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.WfTaskResidueProperties;
import org.springblade.workflow.service.helper.WfTaskActReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「wf_task 兜底残留合并」开关（去 wf_ 表 · P6 退役第一台阶）的回归测试。
 *
 * <p>纯 JDBC + H2，无 Spring 容器 / 无 Flowable。</p>
 *
 * <p>覆盖四条关键行为：
 * <ol>
 *   <li>开关默认开启（零行为变化）：角标 = ACT 计数 + wf_task 残留计数；</li>
 *   <li>开关关闭（纯 ACT 单源）：角标只数 ACT_*，残留行不计入 —— <b>与列表侧同口径</b>，
 *       避免「角标 25、列表只有 24」的割裂；</li>
 *   <li>两源有交集（wf_task.id 恰为 ACT 的 BIZ_TASK_ID_）：残留 SQL 用 {@code NOT IN} 排除，
 *       <b>不重复计数</b>；</li>
 *   <li>开关类未被 Spring 装配时（手动 new）默认值仍为 true —— 否则「合并」会被静默关闭，
 *       与本开关「默认零行为变化」的语义相反。</li>
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
		ds.setUrl("jdbc:h2:mem:wf_residue_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);

		jdbc.execute("CREATE TABLE ACT_RU_TASK (ID_ VARCHAR(64) PRIMARY KEY, BIZ_ASSIGNEE_ BIGINT, "
			+ "IS_TEST_ TINYINT, TENANT_ID_ VARCHAR(32), BIZ_TASK_ID_ BIGINT)");
		jdbc.execute("CREATE TABLE ACT_HI_TASKINST (ID_ VARCHAR(64) PRIMARY KEY, BIZ_ASSIGNEE_ BIGINT, "
			+ "IS_TEST_ TINYINT, TENANT_ID_ VARCHAR(32), BIZ_TASK_ID_ BIGINT, END_TIME_ TIMESTAMP)");
		jdbc.execute("CREATE TABLE wf_task (id BIGINT PRIMARY KEY, assignee BIGINT, status INT, "
			+ "is_test TINYINT, tenant_id VARCHAR(32), inst_id BIGINT)");
		jdbc.execute("CREATE TABLE wf_instance (id BIGINT PRIMARY KEY, status INT, is_deleted TINYINT)");

		reader = new WfTaskActReader(jdbc);
		props = new WfTaskResidueProperties();
		ReflectionTestUtils.setField(reader, "residueMerge", props);
	}

	/** ACT 待办行（业务任务ID = bizTaskId） */
	private void actTodo(String engineId, long bizTaskId, long assignee) {
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_, BIZ_ASSIGNEE_, IS_TEST_, BIZ_TASK_ID_) VALUES (?,?,0,?)",
			engineId, assignee, bizTaskId);
	}

	/** ACT 已办行（END_TIME_ 非空即已办结） */
	private void actDone(String engineId, long bizTaskId, long assignee) {
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_, BIZ_ASSIGNEE_, IS_TEST_, BIZ_TASK_ID_, END_TIME_) "
			+ "VALUES (?,?,0,?,?)", engineId, assignee, bizTaskId, new Timestamp(System.currentTimeMillis()));
	}

	/** wf_task 残留行（未被 ACT 覆盖） */
	private void wfTask(long id, long assignee, int status) {
		jdbc.update("INSERT INTO wf_task (id, assignee, status, is_test) VALUES (?,?,?,0)", id, assignee, status);
	}

	/** 挂在某实例上的 wf_task 行（合成待办：engineTaskId 为空） */
	private void wfTaskWithInst(long id, long assignee, int status, long instId) {
		jdbc.update("INSERT INTO wf_task (id, assignee, status, is_test, inst_id) VALUES (?,?,?,0,?)",
			id, assignee, status, instId);
	}

	/** wf_instance 行 */
	private void wfInstance(long id, int status) {
		jdbc.update("INSERT INTO wf_instance (id, status, is_deleted) VALUES (?,?,0)", id, status);
	}

	private void setMerge(boolean enabled) {
		ReflectionTestUtils.setField(props, "enabled", enabled);
	}

	@Test
	@DisplayName("开关默认开启：角标 = ACT 计数 + wf_task 残留计数（零行为变化）")
	void mergesResidueWhenEnabled() {
		actTodo("T1", 10L, USER);
		wfTask(99L, USER, 0);
		setMerge(true);

		Map<String, Long> r = reader.countFromAct(USER);

		assertEquals(2L, r.get("todo"), "待办角标应为 ACT 1 条 + 残留 1 条");
		assertEquals(0L, r.get("done"), "已办角标应为 0");
	}

	@Test
	@DisplayName("开关关闭（纯 ACT 单源）：残留行不计入，角标只数 ACT_*")
	void skipsResidueWhenDisabled() {
		actTodo("T1", 10L, USER);
		wfTask(99L, USER, 0);
		setMerge(false);

		Map<String, Long> r = reader.countFromAct(USER);

		assertEquals(1L, r.get("todo"), "纯 ACT 单源：待办角标只数 ACT 的 1 条");
	}

	@Test
	@DisplayName("两源有交集（wf_task.id = ACT 的 BIZ_TASK_ID_）：NOT IN 排除，不重复计数")
	void doesNotDoubleCountOnIntersection() {
		// ACT 的 BIZ_TASK_ID_=10，wf_task 也有一行 id=10（同一逻辑任务两源都有）
		actTodo("T1", 10L, USER);
		wfTask(10L, USER, 0);
		setMerge(true);

		Map<String, Long> r = reader.countFromAct(USER);

		assertEquals(1L, r.get("todo"), "交集行已被 NOT IN 排除，不应重复计数");
	}

	@Test
	@DisplayName("已办口径：开关开启含残留、关闭只数 ACT")
	void doneCountFollowsSwitch() {
		actDone("T2", 11L, USER);
		wfTask(98L, USER, 2);
		setMerge(true);
		assertEquals(2L, reader.countFromAct(USER).get("done"), "合并：ACT 1 条 + 残留 1 条");

		setMerge(false);
		assertEquals(1L, reader.countFromAct(USER).get("done"), "纯 ACT 单源：只数 ACT 的 1 条");
	}

	@Test
	@DisplayName("草稿合成待办（实例 status=5）：被排除，不计入角标（草稿移出待办 · 方案 A）")
	void excludesDraftSyntheticTodo() {
		// 草稿实例（status=5）下的合成待办：engineTaskId 为空、不在 ACT
		wfInstance(1000L, 5);
		wfTaskWithInst(99L, USER, 0, 1000L);
		setMerge(true);

		assertEquals(0L, reader.countFromAct(USER).get("todo"),
			"草稿待办已移出「待办」，角标不应再计入（否则角标与列表割裂）");
	}

	@Test
	@DisplayName("运行中实例的合成待办（如退回发起人重提交）：不得误伤，仍计入角标")
	void keepsRunningSyntheticTodo() {
		// 运行中实例（status=0）下的合成待办：engineTaskId 为空但属正常待办，必须保留
		wfInstance(2000L, 0);
		wfTaskWithInst(98L, USER, 0, 2000L);
		setMerge(true);

		assertEquals(1L, reader.countFromAct(USER).get("todo"),
			"运行中实例的合成待办是正常待办，草稿排除判据不得误伤它");
	}

	@Test
	@DisplayName("开关类未被 Spring 装配时（手动 new）默认值仍为 true，避免静默关闭合并")
	void defaultIsMergeOn() {
		assertTrue(new WfTaskResidueProperties().isEnabled(),
			"默认必须为 true（合并），否则未装配场景下残留合并会被静默关闭");
	}
}
