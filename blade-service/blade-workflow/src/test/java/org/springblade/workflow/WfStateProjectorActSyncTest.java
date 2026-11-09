package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.helper.WfStateProjector;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WfStateProjector} 的 ACT 业务列回写回归（H2 + Mockito，纯单元无 Spring 容器）。
 *
 * <p>回归目标：引擎事件驱动的状态反写（方案C 阶段3）<b>必须同时回写 ACT_*</b>。
 * {@code ACT_*.BUSINESS_STATUS_} 是 {@code wf_task.status} 的投影，读源=act 的已办列表
 * 直接用它反解状态列；只更新 wf_task 而不回写 ACT 会产生漂移
 * （实测 68 条，单一形态 {@code wf_status=4(办结) vs ACT=TODO}），后果：
 * ① 已办列表状态标签显示「待办」；② 按「办结」筛选漏掉这批行。</p>
 */
class WfStateProjectorActSyncTest {

	private JdbcTemplate jdbc;
	private WfTaskMapper taskMapper;
	private WfInstanceMapper instanceMapper;
	private WfStateProjector projector;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:wf_proj_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(ds);
		jdbc.execute("CREATE TABLE wf_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64))");
		// N:1 守卫合并统计两表（去 wf_ 表 · 2026-11-09）
		jdbc.execute("CREATE TABLE wf_ext_task (id BIGINT PRIMARY KEY, engine_task_id VARCHAR(64), "
			+ "type INT, is_deleted TINYINT DEFAULT 0)");
		jdbc.execute("CREATE TABLE ACT_HI_TASKINST (ID_ VARCHAR(64) PRIMARY KEY, "
			+ "BUSINESS_STATUS_ VARCHAR(32), IS_TEST_ TINYINT, ORIGINAL_USER_ VARCHAR(64), "
			+ "SIGN_ORDER_ INT, VIEW_TIME_ TIMESTAMP, TIMEOUT_HANDLED_ TINYINT, DUE_DATE_ TIMESTAMP, "
			+ "BIZ_TASK_ID_ BIGINT, BIZ_ASSIGNEE_ BIGINT, TENANT_ID_ VARCHAR(32))");
		jdbc.update("INSERT INTO wf_task (id, engine_task_id) VALUES (?, ?)", 1L, "T1");
		jdbc.update("INSERT INTO ACT_HI_TASKINST (ID_) VALUES (?)", "T1");

		WfTask row = new WfTask();
		row.setId(1L);
		row.setEngineTaskId("T1");
		row.setNodeKey("Task_DeptLeader");
		row.setStatus(WfTask.STATUS_TODO);
		row.setIsTest(0);

		taskMapper = mock(WfTaskMapper.class);
		instanceMapper = mock(WfInstanceMapper.class);
		when(taskMapper.selectList(any())).thenReturn(List.of(row));
		// BaseMapper#updateById 有 (T) 与 (Collection<T>) 两个重载，必须显式给类型消歧
		when(taskMapper.updateById(any(WfTask.class))).thenReturn(1);
		when(taskMapper.update(isNull(), any())).thenReturn(1);

		WfInstance inst = new WfInstance();
		inst.setId(100L);
		inst.setEngineInstId("P1");
		inst.setStatus(WfInstance.STATUS_RUNNING);
		when(instanceMapper.selectOne(any())).thenReturn(inst);

		WfTaskActWriter writer = new WfTaskActWriter(jdbc);
		ReflectionTestUtils.setField(writer, "enabled", true);
		projector = new WfStateProjector(instanceMapper, taskMapper, writer);
	}

	private String actStatus() {
		return jdbc.queryForObject("SELECT BUSINESS_STATUS_ FROM ACT_HI_TASKINST WHERE ID_='T1'", String.class);
	}

	@Test
	@DisplayName("onTaskCompleted：置已办(2) 后回写 ACT.BUSINESS_STATUS_=DONE")
	void onTaskCompletedSyncsActStatus() {
		projector.onTaskCompleted("T1", "P1");
		assertEquals("DONE", actStatus(), "wf_task 置已办后必须回写 ACT 业务列");
	}

	@Test
	@DisplayName("onTaskDeleted：置办结(4) 后回写 ACT.BUSINESS_STATUS_=FINISHED")
	void onTaskDeletedSyncsActStatus() {
		projector.onTaskDeleted("T1", "P1");
		assertEquals("FINISHED", actStatus(), "wf_task 置办结后必须回写 ACT 业务列");
	}
}
