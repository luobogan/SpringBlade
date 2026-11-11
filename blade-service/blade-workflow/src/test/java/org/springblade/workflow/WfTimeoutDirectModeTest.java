package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.WfLedgerModeProperties;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.job.WfTimeoutJob;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 超时扫描在 direct 模式下的回归测试（H2，纯 JDBC，无 Spring 容器/Flowable）。
 *
 * <p><b>缺陷背景（去 wf_ 表 · 批次 1 遗留）</b>：blade.workflow.task-ledger-mode=direct 时
 * 任务创建不再落 wf_task，而超时扫描原实现是「先从 ACT_RU_TASK 捞到期行，
 * 再回 wf_task 反查业务任务」—— wf_task 为空 → 反查必然查不到 →
 * <b>逐条跳过</b> → 超时自动通过 / 转办 / 催办 100% 不触发，且 TIMEOUT_HANDLED_ 永不置位
 * （每轮重复扫描 + 重复告警）。</p>
 *
 * <p>修复：direct 模式改为直接用扫描 SQL 已取到的 ACT 列在内存组装 WfTask。</p>
 */
class WfTimeoutDirectModeTest {

	private JdbcTemplate jdbc;

	private WfTimeoutJob job(String mode) {
		WfLedgerModeProperties p = new WfLedgerModeProperties();
		ReflectionTestUtils.setField(p, "mode", mode);
		// legacy 模式会回读 wf_task：给一个「查不到」的 mock，模拟台账为空
		WfTaskMapper taskMapper = mock(WfTaskMapper.class);
		when(taskMapper.selectById(anyLong())).thenReturn(null);
		when(taskMapper.selectOne(any())).thenReturn(null);
		return new WfTimeoutJob(taskMapper, null, null, null, jdbc, p);
	}

	private void seedDueTask(String id, String procInstId, String nodeKey, String assignee, Date due) {
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_, PROC_INST_ID_, TASK_DEF_KEY_, ASSIGNEE_, BIZ_ASSIGNEE_, "
				+ "BUSINESS_STATUS_, BIZ_TASK_ID_, IS_TEST_, TIMEOUT_HANDLED_, DUE_DATE_, SUSPENSION_STATE_) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
			id, procInstId, nodeKey, assignee, Long.valueOf(assignee), "TODO", 9001L, 0, 0,
			new Timestamp(due.getTime()), 1);
	}

	@SuppressWarnings("unchecked")
	private List<WfTask> scan(WfTimeoutJob job, Date now) throws Exception {
		Method m = WfTimeoutJob.class.getDeclaredMethod("scanDueFromAct", Date.class);
		m.setAccessible(true);
		return (List<WfTask>) m.invoke(job, now);
	}

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_timeout_direct_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);
		jdbc.execute("CREATE TABLE ACT_HI_PROCINST (ID_ VARCHAR(64) PRIMARY KEY, BUSINESS_ID_ BIGINT)");
		jdbc.execute("CREATE TABLE ACT_RU_TASK ("
			+ "ID_ VARCHAR(64) PRIMARY KEY, PROC_INST_ID_ VARCHAR(64), TASK_DEF_KEY_ VARCHAR(64), "
			+ "ASSIGNEE_ VARCHAR(64), BIZ_ASSIGNEE_ BIGINT, BUSINESS_STATUS_ VARCHAR(32), "
			+ "BIZ_TASK_ID_ BIGINT, IS_TEST_ TINYINT, TIMEOUT_HANDLED_ TINYINT, DUE_DATE_ TIMESTAMP, "
			+ "SUSPENSION_STATE_ INT)");
	}

	@Test
	@DisplayName("direct 模式：到期任务必须被解析出来（旧实现因 wf_task 为空而全部跳过）")
	void directModeScansDueTasks() throws Exception {
		jdbc.update("INSERT INTO ACT_HI_PROCINST (ID_, BUSINESS_ID_) VALUES (?, ?)", "PI1", 777L);
		Date now = new Date();
		seedDueTask("T1", "PI1", "Task_Department", "279", new Date(now.getTime() - 3600000L));

		List<WfTask> due = scan(job("direct"), now);

		assertEquals(1, due.size(), "direct 模式必须扫出到期任务（旧实现恒 0 条）");
		WfTask t = due.get(0);
		assertEquals("T1", t.getEngineTaskId());
		assertEquals("Task_Department", t.getNodeKey());
		assertEquals(Long.valueOf(279L), t.getAssignee());
		assertEquals(Long.valueOf(777L), t.getInstId(), "instId 必须是业务实例 id");
		assertEquals(Long.valueOf(9001L), t.getId());
		assertEquals(WfTask.STATUS_TODO, t.getStatus());
		assertNotNull(t.getDueTime());
		assertTrue(t.getDueTime().before(now), "dueTime 必须已到期");
	}

	@Test
	@DisplayName("legacy 模式：仍走 wf_task 反查（表空 → 扫不出，与修复前行为一致）")
	void legacyModeStillDependsOnWfTask() throws Exception {
		jdbc.update("INSERT INTO ACT_HI_PROCINST (ID_, BUSINESS_ID_) VALUES (?, ?)", "PI1", 777L);
		Date now = new Date();
		seedDueTask("T1", "PI1", "Task_Department", "279", new Date(now.getTime() - 3600000L));

		List<WfTask> due = scan(job("legacy"), now);

		assertEquals(0, due.size(), "legacy 模式依赖 wf_task，台账为空时扫不出（回退链路行为不变）");
	}

	@Test
	@DisplayName("未到期 / 已处理 / 测试态 的任务不进入扫描结果")
	void filtersNonDueTasks() throws Exception {
		jdbc.update("INSERT INTO ACT_HI_PROCINST (ID_, BUSINESS_ID_) VALUES (?, ?)", "PI1", 777L);
		Date now = new Date();
		seedDueTask("T1", "PI1", "NodeA", "279", new Date(now.getTime() + 3600000L));
		seedDueTask("T2", "PI1", "NodeB", "279", new Date(now.getTime() - 3600000L));
		jdbc.update("UPDATE ACT_RU_TASK SET TIMEOUT_HANDLED_ = 1 WHERE ID_ = 'T2'");

		List<WfTask> due = scan(job("direct"), now);

		assertTrue(due.isEmpty(), "未到期与已处理的任务都不得进入扫描结果");
	}
}
