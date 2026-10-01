package org.springblade.workflow;

import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cmd.SetProcessInstanceBusinessStatusCmd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 翻源安全网：引擎侧一致性验证（对应方案文档 §17.2 的 V8 / V9 / V27）。
 *
 * <p><b>为什么要锁这三个点</b>：本项目已完成「实例/任务/审批日志读源 = act」与 P6 停写
 * {@code wf_approval_log}，运行期一律以 {@code ACT_*} 为事实源。以下三点直接决定该结论是否成立：</p>
 * <ul>
 *   <li><b>V8</b>：业务终态用原生 {@code BUSINESS_STATUS_} 承载，必须确认它落在哪张表 ——
 *       若只写 {@code ACT_HI_PROCINST}，运行期（{@code ACT_RU_EXECUTION}）查终态须回历史表；</li>
 *   <li><b>V9</b>：文档 §12.4 推荐「用 {@code managementService.executeCommand} 在引擎同事务写自定义列」
 *       以消除两阶段不一致。本项目实际双写器走 JdbcTemplate（引擎事务外），故该性质必须实测固化，
 *       否则「补写失败无补偿」（文档 E4）的兜底结论无依据；</li>
 *   <li><b>V27</b>：存量回填 SQL 依赖 {@code wf_instance.engine_inst_id = ACT_HI_PROCINST.PROC_INST_ID_}，
 *       且读源组装 WfInstance 桩时会用到 {@code ID_}；二者若不恒等会导致回填漏行/反查错位。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfActConsistencyVerifyTest {

	private static final String KEY = "actConsistencyVerify";

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="defs_actConsistencyVerify">
		    <process id="actConsistencyVerify" name="引擎一致性验证" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <userTask id="approval" name="审批"/>
		        <endEvent id="endEvent" name="归档"/>
		        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="approval"/>
		        <sequenceFlow id="f2" sourceRef="approval" targetRef="endEvent"/>
		    </process>
		</definitions>
		""";

	@Autowired
	private ManagementService managementService;
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
				// 用例内已清理
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

	/** V8：{@code SetProcessInstanceBusinessStatusCmd} 是否同时更新历史表与运行期表的 BUSINESS_STATUS_ */
	@Test
	void v8_setBusinessStatus_shouldUpdateHistoricProcinst() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		String inst = processService.startInstance(KEY, "v8:1", Map.of("k", "v"));
		assertThat(inst).isNotNull();

		managementService.executeCommand(new SetProcessInstanceBusinessStatusCmd(inst, "APPROVED"));

		String hi = jdbc().queryForObject(
			"SELECT BUSINESS_STATUS_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?",
			String.class, inst);
		assertThat(hi).as("历史表 ACT_HI_PROCINST.BUSINESS_STATUS_ 必须落库").isEqualTo("APPROVED");

		// 运行期侧：实测该命令会同步「根执行流」的 BUSINESS_STATUS_（子进程执行流为 null）
		// ⇒ 运行期查终态可直接用 ACT_RU_EXECUTION.PROC_INST_ID_ 命中根执行流，不必回历史表。
		List<String> ru = jdbc().queryForList(
			"SELECT BUSINESS_STATUS_ FROM ACT_RU_EXECUTION WHERE PROC_INST_ID_ = ?",
			String.class, inst);
		System.out.println("[V8] HI=" + hi + " RU_EXECUTION=" + ru);
		assertThat(ru).as("运行期根执行流 BUSINESS_STATUS_ 应同步写入")
			.contains("APPROVED");
	}

	/** V9：命令内写列后抛异常 —— 验证是否与引擎同事务（回滚一致性） */
	@Test
	void v9_writeInsideEngineCommand_shouldRollBackOnFailure() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		String inst = processService.startInstance(KEY, "v9:1", Map.of("k", "v"));

		// 先写基线值：否则 NAME_ 本就为 null 时「回滚后等于 before」是空断言，测不出是否真的回滚
		jdbc().update("UPDATE ACT_HI_PROCINST SET NAME_ = ? WHERE PROC_INST_ID_ = ?",
			"BASE_V9", inst);
		String before = jdbc().queryForObject(
			"SELECT NAME_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", String.class, inst);
		assertThat(before).isEqualTo("BASE_V9");

		assertThatThrownBy(() -> managementService.executeCommand(commandContext -> {
			// 在引擎命令内写列（JdbcTemplate 经 Spring 绑定取同一条连接），验证是否随命令回滚
			jdbc().update("UPDATE ACT_HI_PROCINST SET NAME_ = ? WHERE PROC_INST_ID_ = ?",
				"SHOULD_NOT_PERSIST", inst);
			throw new IllegalStateException("V9 故意失败，验证回滚");
		})).isInstanceOf(IllegalStateException.class);

		String after = jdbc().queryForObject(
			"SELECT NAME_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?", String.class, inst);
		System.out.println("[V9] before=" + before + " after=" + after);
		assertThat(after).as("命令失败时同事务写入必须回滚")
			.isEqualTo(before)
			.isNotEqualTo("SHOULD_NOT_PERSIST");
	}

	/** V27：ACT_HI_PROCINST.ID_ 与 PROC_INST_ID_ 是否恒等（回填 SQL 依赖 PROC_INST_ID_ 匹配） */
	@Test
	void v27_historicProcinstId_shouldEqualProcInstId() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		String first = processService.startInstance(KEY, "v27:1", Map.of());
		String second = processService.startInstance(KEY, "v27:2", Map.of());

		List<Map<String, Object>> rows = jdbc().queryForList(
			"SELECT ID_, PROC_INST_ID_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ IN (?, ?)",
			first, second);
		assertThat(rows).hasSize(2);
		for (Map<String, Object> row : rows) {
			assertThat(String.valueOf(row.get("ID_")))
				.as("ID_ 必须等于 PROC_INST_ID_，否则回填/反查会错位")
				.isEqualTo(String.valueOf(row.get("PROC_INST_ID_")));
		}
	}
}
