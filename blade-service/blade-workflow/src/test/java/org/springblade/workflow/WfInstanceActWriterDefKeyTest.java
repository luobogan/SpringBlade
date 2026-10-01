package org.springblade.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.helper.WfInstanceActWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R1 / D6 / M1 桥接列 {@code DEF_KEY_} 的写入回归。
 *
 * <p>业务定义 {@code wf_process_definition}（defId）与引擎 {@code ACT_RE_PROCDEF} 是 <b>1:N（版本）</b>：
 * 每次部署生成新的 {@code ID_}，而 {@code KEY_} 不变。仅凭 {@code DEF_ID_} 无法从引擎
 * {@code PROC_DEF_ID_} 反查业务定义与其版本组 —— 故在 {@code ACT_HI_PROCINST} 冗余引擎 {@code KEY_}
 * （配合既有桥接表 {@code flow_def_bridge} 即可双向反查）。详见方案文档 §15.1 M1 / §17.1 D6 / §17.3 R1。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
	FlowableTestConfig.class,
	WfInstanceActWriter.class,
	WfInstanceActWriterDefKeyTest.JdbcCfg.class
})
class WfInstanceActWriterDefKeyTest {

	private static final String KEY = "defKeyBridgeProc";

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="defs_defKeyBridgeProc">
		    <process id="defKeyBridgeProc" name="DEF_KEY 桥接" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <userTask id="approval" name="审批"/>
		        <endEvent id="endEvent" name="归档"/>
		        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="approval"/>
		        <sequenceFlow id="f2" sourceRef="approval" targetRef="endEvent"/>
		    </process>
		</definitions>
		""";

	/** 本测试只需一个 JdbcTemplate（FlowableTestConfig 只提供 DataSource） */
	@Configuration
	static class JdbcCfg {
		@Bean
		public JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}
	}

	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private WfInstanceActWriter writer;
	@Autowired
	private IProcessService processService;

	private String deploymentId;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(writer, "enabled", true);
		ensureCustomColumns();
	}

	@AfterEach
	void cleanup() {
		if (deploymentId != null) {
			try {
				processService.deleteDeployment(deploymentId);
			} catch (Exception ignore) {
				// 已清理
			}
			deploymentId = null;
		}
	}

	/**
	 * 复用 {@link org.springblade.workflow.concurrency.H2ActColumnExtender} 补齐业务列 —— 与并发测试
	 * 共用同一份列定义，避免两处各写一份导致漂移。
	 *
	 * <p>引擎自带 DDL 只有原生列，业务列（BUSINESS_ID_/DEF_ID_/DEF_KEY_…）全是我们后加的，
	 * 这正是「列化」的含义：引擎永不写这些列，必须业务侧补写（§11）。</p>
	 */
	private void ensureCustomColumns() {
		new org.springblade.workflow.concurrency.H2ActColumnExtender(jdbc);
	}

	/** 发起写回必须把引擎 KEY_ 冗余进 DEF_KEY_（否则无法反查业务定义/版本组） */
	@Test
	void writeOnStart_shouldWriteDefKeyDerivedFromProcDefId() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		String engineInstId = processService.startInstance(KEY, "defkey:1", Map.of());
		String procDefId = jdbc.queryForObject(
			"SELECT PROC_DEF_ID_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?",
			String.class, engineInstId);
		assertThat(procDefId).isNotNull();

		WfInstance inst = new WfInstance();
		inst.setId(2104055356658364418L);
		inst.setDefId(2104055356658364419L);
		inst.setProcDefId(procDefId);
		inst.setIsTest(0);
		inst.setStarter(1123598821738675201L);

		writer.writeOnStart(inst, engineInstId, WfInstance.STATUS_RUNNING);

		String defKey = jdbc.queryForObject(
			"SELECT DEF_KEY_ FROM ACT_HI_PROCINST WHERE ID_ = ?", String.class, engineInstId);
		// 同一次 UPDATE 的其它业务列也应落库（证明不是「只写了 DEF_KEY_ 或整体没写」）
		Long businessId = jdbc.queryForObject(
			"SELECT BUSINESS_ID_ FROM ACT_HI_PROCINST WHERE ID_ = ?", Long.class, engineInstId);
		assertThat(businessId).as("同批业务列必须一并写回（BUSINESS_ID_ = 业务实例ID）")
			.isEqualTo(inst.getId());
		assertThat(defKey).as("DEF_KEY_ 必须等于引擎 ACT_RE_PROCDEF.KEY_")
			.isEqualTo(KEY);
	}

	/** procDefId 为空（存量/未部署）时不得报错，DEF_KEY_ 保持 NULL */
	@Test
	void writeOnStart_withoutProcDefId_shouldLeaveDefKeyNull() {
		deploymentId = processService.deployProcess(KEY, BPMN);
		String engineInstId = processService.startInstance(KEY, "defkey:2", Map.of());

		WfInstance inst = new WfInstance();
		inst.setId(2104055356658364420L);
		inst.setProcDefId(null);
		inst.setIsTest(0);

		writer.writeOnStart(inst, engineInstId, WfInstance.STATUS_RUNNING);

		String defKey = jdbc.queryForObject(
			"SELECT DEF_KEY_ FROM ACT_HI_PROCINST WHERE ID_ = ?", String.class, engineInstId);
		assertThat(defKey).isNull();
	}
}
