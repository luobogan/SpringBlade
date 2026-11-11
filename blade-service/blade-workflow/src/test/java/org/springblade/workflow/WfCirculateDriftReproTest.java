package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfExtTask;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 「抄送后审批」双写漂移的 H2 最小复现（《去wf_表-任务读源翻源上线检查.md》
 * §11.4.2 附带发现 / §11.9.2 起步项 ②）。纯 JDBC、无 Spring 容器、不触碰 dev 库。
 *
 * <p><b>为什么不用 dev 库只读复现</b>：{@code seed_wf_ext_task.py:197-206} 的
 * {@code insert_ext(..., engine_task_id='')} 默认空串，造出的扩展行与 A 类任务
 * 不共享 {@code engineTaskId}，守卫的扩展段统计不会触发。真实抄送才会
 * {@code setEngineTaskId(宿主)}（{@code WfTaskServiceImpl.circulate():841}）。
 * 故用 H2 精确构造「同一 engineTaskId + 1 条 A 类行 + 1 条抄送行」。</p>
 *
 * <p><b>被测机制</b>：{@code WfTaskActWriter.sync():112-134} 的 N:1 守卫合并统计
 * {@code wf_task} 与 {@code wf_ext_task}（排除 {@code type=7} 装饰行），
 * {@code siblings > 1} 即跳过 ACT 业务列双写。但 {@code wf_ext_task} 行
 * <b>从不调用 sync</b>（{@code WfExtTask extends TenantEntity}，非 {@code WfTask} 子类；
 * {@code circulate():837} 亦明写「不再 sync ACT」），不会踩踏 ACT 单行 ——
 * 把它计入 {@code siblings} 属过度保守，会让被抄送过的任务在审批时
 * {@code BUSINESS_STATUS_} 停在 {@code TODO}。</p>
 *
 * <p><b>可重跑 SQL 判据</b>（对齐 {@code doc/sql/migration/_verify_task_1to1.sql} 第 ⑥ 项
 * 双写漂移；在真实库上把 T1 换成实际 engine_task_id）：</p>
 * <pre>{@code
 * SELECT h.ID_, h.BUSINESS_STATUS_, t.status
 * FROM wf_task t JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
 * WHERE t.is_deleted = 0
 *   AND h.BUSINESS_STATUS_ <> CASE t.status
 *       WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
 *       WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
 *       WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' END;
 * }</pre>
 *
 * <p><b>实测结论（2026-11-10 上午，H2，3/3 通过）</b>：漂移<b>可复现</b>。
 * 同一 {@code engineTaskId} 下 1 条 A 类行 + 1 条抄送行使 {@code siblings = 2 > 1}，
 * 审批时的 {@code sync} 被守卫跳过，{@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 的
 * {@code BUSINESS_STATUS_} 停在 {@code TODO}（已办显示成待办）；
 * 对照组（无抄送行）与 {@code type=7} 装饰行均能正常写回 {@code DONE}。
 * ⇒ 守卫的 {@code wf_ext_task} 统计属过度保守（扩展行不调用 {@code sync}），见 <b>D18</b>。</p>
 *
 * <p><b>批次 1 后（2026-11-10 下午）</b>：守卫已按 D18 删除，本类用例 1 断言反转为
 * 「正常写回 {@code DONE}」，作为<b>修复后的回归基线</b>留存；原漂移形态见上方结论与
 * {@code git} 历史。H2 结论明确，<b>无需启用 dev 受控流转兜底路径</b>。</p>
 *
 * <p>运行方式（JDK21、离线）：
 * {@code mvn -o -pl blade-service/blade-workflow -am test -Dtest=WfCirculateDriftReproTest
 * -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false}</p>
 */
class WfCirculateDriftReproTest {

	private JdbcTemplate jdbc;
	private WfTaskActWriter writer;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_circulate_drift_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);

		// 表结构与 WfTaskActWriterTest 完全一致（含 DUE_DATE_，缺列会被 sync 的 catch 静默吞掉）
		jdbc.execute("CREATE TABLE wf_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64))");
		jdbc.execute("CREATE TABLE wf_ext_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64), "
			+ "type INT, is_deleted TINYINT DEFAULT 0)");
		for (String t : new String[]{"ACT_RU_TASK", "ACT_HI_TASKINST"}) {
			jdbc.execute("CREATE TABLE " + t + " ("
				+ "ID_ VARCHAR(64) PRIMARY KEY, BUSINESS_STATUS_ VARCHAR(32), IS_TEST_ TINYINT, "
				+ "ORIGINAL_USER_ VARCHAR(64), SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, "
				+ "TIMEOUT_HANDLED_ TINYINT, BIZ_TASK_ID_ BIGINT, BIZ_ASSIGNEE_ BIGINT, TENANT_ID_ VARCHAR(32), "
				+ "DUE_DATE_ TIMESTAMP)");
		}
		writer = new WfTaskActWriter(jdbc);
		ReflectionTestUtils.setField(writer, "enabled", true);
	}

	private void seedTask(String engineTaskId) {
		jdbc.update("INSERT INTO wf_task (id, engine_task_id) VALUES (?, ?)", 1L, engineTaskId);
		jdbc.update("INSERT INTO ACT_RU_TASK (ID_) VALUES (?)", engineTaskId);
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_) VALUES (?)", engineTaskId);
	}

	private WfTask task(String engineTaskId, int status) {
		WfTask t = new WfTask();
		t.setId(2104055356658364418L);
		t.setAssignee(1123598821738675201L);
		t.setEngineTaskId(engineTaskId);
		t.setStatus(status);
		t.setIsTest(0);
		t.setOriginalUser(99L);
		t.setSignOrder(1);
		t.setViewTime(new Date());
		t.setTimeoutHandled(0);
		return t;
	}

	private String statusOf(String table, String engineTaskId) {
		return jdbc.queryForObject(
			"SELECT BUSINESS_STATUS_ FROM " + table + " WHERE ID_=?", String.class, engineTaskId);
	}

	@Test
	@DisplayName("D18 回归基线：守卫删除后，抄送后审批正常写回 DONE（不再漂移）")
	void noDriftAfterGuardRemovalWhenCirculateSharesEngineTask() {
		seedTask("T1");
		// 任务创建：writeFull / sync 直写 ACT，业务列 TODO
		writer.sync(task("T1", WfTask.STATUS_TODO));
		assertEquals("TODO", statusOf("ACT_RU_TASK", "T1"));
		assertEquals("TODO", statusOf("ACT_HI_TASKINST", "T1"));

		// 真实抄送：只写 wf_ext_task、沿用宿主 engineTaskId、不调用 sync
		jdbc.update("INSERT INTO wf_ext_task (id, engine_task_id, type, is_deleted) VALUES (?, ?, ?, ?)",
			9001L, "T1", WfExtTask.TYPE_CIRCULATE, 0);

		// 审批：置 DONE 后 sync
		writer.sync(task("T1", WfTask.STATUS_DONE));

		// 旧守卫下会停在 TODO（已复现）；D18 删除守卫后统一委托直写器 → 正常写回 DONE
		assertEquals("DONE", statusOf("ACT_RU_TASK", "T1"),
			"守卫删除后 ACT_RU_TASK 应写回 DONE");
		assertEquals("DONE", statusOf("ACT_HI_TASKINST", "T1"),
			"守卫删除后 ACT_HI_TASKINST 应写回 DONE（已办列表状态正确）");
	}

	@Test
	@DisplayName("对照组：无抄送行时审批能正常写回 DONE")
	void noDriftWithoutCirculate() {
		seedTask("T2");
		writer.sync(task("T2", WfTask.STATUS_TODO));
		writer.sync(task("T2", WfTask.STATUS_DONE));
		assertEquals("DONE", statusOf("ACT_RU_TASK", "T2"));
		assertEquals("DONE", statusOf("ACT_HI_TASKINST", "T2"));
	}

	@Test
	@DisplayName("type=7 自由流程步骤名装饰行被守卫 SQL 排除，不触发漂移")
	void decorationRowDoesNotTriggerGuard() {
		seedTask("T3");
		writer.sync(task("T3", WfTask.STATUS_TODO));
		jdbc.update("INSERT INTO wf_ext_task (id, engine_task_id, type, is_deleted) VALUES (?, ?, ?, ?)",
			9003L, "T3", WfExtTask.TYPE_FREE_FLOW, 0);
		writer.sync(task("T3", WfTask.STATUS_DONE));
		assertEquals("DONE", statusOf("ACT_RU_TASK", "T3"));
		assertEquals("DONE", statusOf("ACT_HI_TASKINST", "T3"));
	}
}
