package org.springblade.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运行期生命周期验证（对应方案文档 §12.6「测试场景清单（必测）」中可在引擎层固化的部分）。
 *
 * <p>本类锁定「读源 = act」后的运行期事实源行为：实例一旦出生，其后所有生命周期动作
 * （暂停 / 恢复 / 终止 / 跳转）都**只能**以 {@code ACT_*} 为准，因此必须把它们的落库表现固定下来，
 * 否则翻源后「引擎动了、读到的状态没动」这类问题无从回归。</p>
 *
 * <ul>
 *   <li>暂停 / 恢复 → {@code ACT_RU_EXECUTION.SUSPENSION_STATE_}；</li>
 *   <li>终止 → {@code ACT_RU_*} 清空 + {@code ACT_HI_PROCINST.END_TIME_ / DELETE_REASON_}；</li>
 *   <li>跳转等待态 → 停住并生成新待办；跳转<b>非</b>等待态 → 不停住、直接流出（印证 §9.4-1
 *       「节点类型过滤必须保留」这一业务层防护不能删）；</li>
 *   <li>{@code is_test} 隔离 → 测试态行必须被 {@code IS_TEST_=0} 过滤排除。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfRuntimeLifecycleVerifyTest {

	private static final String KEY = "runtimeLifecycleVerify";

	/** start → approval(等待) → secondApproval(等待) → end */
	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="defs_runtimeLifecycleVerify">
		    <process id="runtimeLifecycleVerify" name="运行期生命周期验证" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <userTask id="approval" name="审批"/>
		        <userTask id="secondApproval" name="二次审批"/>
		        <endEvent id="endEvent" name="归档"/>
		        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="approval"/>
		        <sequenceFlow id="f2" sourceRef="approval" targetRef="secondApproval"/>
		        <sequenceFlow id="f3" sourceRef="secondApproval" targetRef="endEvent"/>
		    </process>
		</definitions>
		""";

	@Autowired
	private IProcessService processService;
	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbc;
	private String deploymentId;

	@AfterEach
	void cleanup() {
		if (deploymentId != null) {
			try {
				processService.deleteDeployment(deploymentId);
			} catch (Exception ignore) {
				// 终止类用例可能已无部署可删
			}
			deploymentId = null;
		}
	}

	private JdbcTemplate jdbc() {
		if (jdbc == null) {
			jdbc = new JdbcTemplate(dataSource);
		}
		return jdbc;
	}

	private String deploy() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		assertThat(deploymentId).isNotNull();
		return deploymentId;
	}

	private String start(String bizKey) {
		String inst = processService.startInstance(KEY, bizKey, Map.of());
		assertThat(inst).isNotNull();
		return inst;
	}

	/** 暂停 / 恢复：SUSPENSION_STATE_ 在引擎侧的正确落库与回收 */
	@Test
	void suspendAndActivate_shouldFlipSuspensionState() {
		deploy();
		String inst = start("life:suspend");

		processService.suspendProcessInstance(inst);
		List<Integer> suspended = jdbc().queryForList(
			"SELECT SUSPENSION_STATE_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ?",
			Integer.class, inst);
		assertThat(suspended).as("暂停后执行流必须全部为挂起态(2)").isNotEmpty()
			.allMatch(s -> s == 2);

		processService.activateProcessInstance(inst);
		List<Integer> actives = jdbc().queryForList(
			"SELECT SUSPENSION_STATE_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ?",
			Integer.class, inst);
		assertThat(actives).as("恢复后执行流必须回到激活态(1)").isNotEmpty()
			.allMatch(s -> s == 1);
	}

	/** 终止：运行态清空 + 历史表留下终态与原因（撤回 / 撤销 / 终止共用此路径） */
	@Test
	void deleteProcessInstance_shouldClearRuntimeAndWriteHistoricEnd() {
		deploy();
		String inst = start("life:terminate");

		processService.deleteProcessInstance(inst, "V-TERM");

		Integer ruExec = jdbc().queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ?", Integer.class, inst);
		Integer ruTask = jdbc().queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE PROC_INST_ID_ = ?", Integer.class, inst);
		assertThat(ruExec).as("终止后 ACT_RU_EXECUTION 必须清空").isZero();
		assertThat(ruTask).as("终止后 ACT_RU_TASK 必须清空").isZero();

		Map<String, Object> hi = jdbc().queryForMap(
			"SELECT END_TIME_, DELETE_REASON_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", inst);
		assertThat((Date) hi.get("END_TIME_")).as("历史表必须落结束时间").isNotNull();
		assertThat(String.valueOf(hi.get("DELETE_REASON_")))
			.as("历史表必须记录终结原因（撤回/撤销可区分）").contains("V-TERM");
	}

	/** 自由流跳到【等待态】节点：应停住并在目标节点生成待办 */
	@Test
	void moveToWaitState_shouldStopAndCreateTask() {
		deploy();
		String inst = start("life:move-wait");

		processService.moveActivity(inst, "approval", "secondApproval", null);

		List<String> defKeys = jdbc().queryForList(
			"SELECT TASK_DEF_KEY_ FROM ACT_RU_TASK WHERE PROC_INST_ID_ = ?", String.class, inst);
		assertThat(defKeys).as("跳转后必须停在目标等待态节点").containsExactly("secondApproval");
		assertThat(jdbc().queryForObject(
			"SELECT END_TIME_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", Date.class, inst))
			.as("等待态跳转不应结束实例").isNull();
	}

	/** ⚠️ 跳到【非等待态】（结束事件）：token 不停住、直接流出结束 —— 业务层节点类型过滤不可删 */
	@Test
	void moveToNonWaitState_shouldNotStop_businessFilterRequired() {
		deploy();
		String inst = start("life:move-nonwait");

		processService.moveActivity(inst, "approval", "endEvent", null);

		assertThat(jdbc().queryForObject(
			"SELECT END_TIME_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", Date.class, inst))
			.as("跳到结束事件会立刻流出并结束（不会停住）").isNotNull();
		assertThat(jdbc().queryForObject(
			"SELECT COUNT(*) FROM ACT_RU_TASK WHERE PROC_INST_ID_ = ?", Integer.class, inst))
			.as("非等待态不会产出待办").isZero();
		// 这正是 §9.4-1「退回了但没动」的根因：归档(3)/等待(5)/自动处理(6)/网关(7) 都不是等待态，
		// 故 WfRejectManager 的「节点类型过滤」必须保留在 changeState/moveActivity 之前。
	}

	/**
	 * 补 §11 定义的自定义列 {@code IS_TEST_}（引擎自带 DDL 没有它 —— 这正是「列化」的含义：
	 * 引擎永不写该列，必须由业务侧补写，且必须「可空 + DEFAULT」否则引擎 INSERT 会失败，见 §11.1.5）。
	 * 幂等：H2 在同 JVM 内被多个测试类共享，重复建列会报错。
	 */
	private void ensureIsTestColumn() {
		Integer exists = jdbc().queryForObject(
			"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
				+ "WHERE TABLE_NAME = 'ACT_HI_PROCINST' AND COLUMN_NAME = 'IS_TEST_'",
			Integer.class);
		if (exists == null || exists == 0) {
			jdbc().execute("ALTER TABLE ACT_HI_PROCINST ADD COLUMN IS_TEST_ TINYINT DEFAULT 0");
		}
	}

	/** is_test 隔离：测试态行必须被 IS_TEST_=0 过滤排除（翻源后隔离不能丢） */
	@Test
	void isTestInstance_shouldBeExcludedByIsTestFilter() {
		deploy();
		ensureIsTestColumn();
		String inst = start("life:istest");

		jdbc().update("UPDATE ACT_HI_PROCINST SET IS_TEST_ = 1 WHERE PROC_INST_ID_ = ?", inst);

		Integer visible = jdbc().queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ? AND IS_TEST_ = 0",
			Integer.class, inst);
		Integer all = jdbc().queryForObject(
			"SELECT COUNT(*) FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", Integer.class, inst);
		assertThat(all).as("数据本身存在").isEqualTo(1);
		assertThat(visible).as("测试态实例必须被 IS_TEST_=0 过滤排除").isZero();
	}
}
