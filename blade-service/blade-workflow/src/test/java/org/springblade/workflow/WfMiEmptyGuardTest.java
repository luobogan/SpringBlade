package org.springblade.workflow;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.config.FlowableConfig;
import org.springblade.workflow.listener.WfMiEmptyGuardListener;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MI 空集合守卫（{@link WfMiEmptyGuardListener}）的引擎级验证。
 *
 * <p>复用 {@link FlowableTestConfig} 的内存 H2 引擎，驱动真实 Flowable 引擎验证三件事：</p>
 * <ul>
 *   <li>空集合（注入空 List）→ 发起/流转被拦截，错误消息点名节点（不再静默跳过）；</li>
 *   <li>集合变量未注入（null）→ 同样拦截（0 实例跳过是同一故障）；</li>
 *   <li>节点配置「流程异常处理」兜底（wf:node extJson exceptionHandle way=1）→ 放行自动跳过；
 *       集合非空 → 正常逐人展开，互不干扰。</li>
 * </ul>
 *
 * <p>不依赖 Nacos / MySQL / Redis；守卫的 wf_* 留痕在测试上下文中无 Mapper，走「尽力而为」分支。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FlowableTestConfig.class, WfMiEmptyGuardTest.GuardConfig.class})
class WfMiEmptyGuardTest {

	/** 测试装配：显式声明守卫监听器（测试上下文不组件扫描，Mapper 依赖为空走尽力而为分支） */
	@Configuration
	static class GuardConfig {
		@Bean
		public WfMiEmptyGuardListener wfMiEmptyGuardListener() {
			return new WfMiEmptyGuardListener(null, null);
		}
	}

	private static final String PROC_EMPTY = "miGuardEmpty";
	private static final String PROC_MISSING = "miGuardMissing";
	private static final String PROC_FALLBACK = "miGuardFallback";
	private static final String PROC_NONEMPTY = "miGuardNonEmpty";

	@Autowired
	private IProcessService processService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private HistoryService historyService;

	private final Set<String> deployed = new HashSet<>();

	@BeforeEach
	void reset() {
		deployed.clear();
	}

	@AfterEach
	void cleanup() {
		for (String depId : deployed) {
			processService.deleteDeployment(depId);
		}
		deployed.clear();
	}

	// ───────────────────────── 用例 ─────────────────────────

	@Test
	void emptyCollectionBlocksStartAndNamesTheNode() {
		deploy(PROC_EMPTY, miBpmn(PROC_EMPTY, null));
		Map<String, Object> vars = Map.of("wfMiAssignees_miApprove", new ArrayList<String>());
		assertThatThrownBy(() -> processService.startInstance(PROC_EMPTY, "1:1", vars))
			.isInstanceOf(Exception.class)
			.hasMessageContaining("miApprove");
	}

	@Test
	void missingCollectionVariableAlsoBlocks() {
		deploy(PROC_MISSING, miBpmn(PROC_MISSING, null));
		// 完全不注入集合变量 → 引擎同样 0 实例静默跳过 → 守卫拦截
		assertThatThrownBy(() -> processService.startInstance(PROC_MISSING, "1:1", Map.of()))
			.isInstanceOf(Exception.class)
			.hasMessageContaining("未解析到任何办理人");
	}

	@Test
	void fallbackConfiguredAllowsAutoSkip() {
		deploy(PROC_FALLBACK, miBpmnWithFallback(PROC_FALLBACK));
		String instId = processService.startInstance(PROC_FALLBACK, "1:1", Map.of("wfMiAssignees_miApprove", new ArrayList<String>()));
		assertThat(instId).isNotNull();
		// 放行自动跳过：实例直接走完
		HistoricProcessInstance his = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(instId).singleResult();
		assertThat(his).isNotNull();
		assertThat(his.getEndTime()).isNotNull();
	}

	@Test
	void nonEmptyCollectionStillExpandsNormally() {
		deploy(PROC_NONEMPTY, miBpmn(PROC_NONEMPTY, null));
		List<String> assignees = List.of("1001", "1002", "1003");
		String instId = processService.startInstance(PROC_NONEMPTY, "1:1", Map.of("wfMiAssignees_miApprove", new ArrayList<>(assignees)));
		assertThat(instId).isNotNull();
		// 非空集合正常逐人展开：实例停在会签节点等待（未结束），且 MI 子执行已创建
		HistoricProcessInstance his = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(instId).singleResult();
		assertThat(his).isNotNull();
		assertThat(his.getEndTime()).isNull();
		// 1 个 MI 根执行 + 3 个子执行（每人一个）≥ 4
		assertThat(runtimeService.createExecutionQuery().processInstanceId(instId).list().size()).isGreaterThanOrEqualTo(4);
	}

	// ───────────────────────── 工具 ─────────────────────────

	private void deploy(String procKey, String xml) {
		String depId = processService.deployProcess(procKey, xml);
		assertThat(depId).isNotNull();
		deployed.add(depId);
	}

	/** 与 applyMultiInstanceIfEnabled 同形态的 MI BPMN；fallbackExtJson 非空时挂在 wf:node extJson 上 */
	private static String miBpmn(String procKey, String unused) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			             xmlns:flowable="http://flowable.org/bpmn"
			             xmlns:wf="http://www.springblade.org/workflow"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="MI守卫 %s" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="miApprove" name="会签节点" flowable:assignee="${wfMiAssignee}">
			            <multiInstanceLoopCharacteristics flowable:isSequential="false"
			                flowable:collection="wfMiAssignees_miApprove" flowable:elementVariable="wfMiAssignee">
			                <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
			            </multiInstanceLoopCharacteristics>
			        </userTask>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="flow_start" sourceRef="startEvent" targetRef="miApprove"/>
			        <sequenceFlow id="flow_end" sourceRef="miApprove" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, procKey);
	}

	private static String miBpmnWithFallback(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			             xmlns:flowable="http://flowable.org/bpmn"
			             xmlns:wf="http://www.springblade.org/workflow"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="MI兜底 %s" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="miApprove" name="会签节点" flowable:assignee="${wfMiAssignee}">
			            <extensionElements>
			                <wf:node nodeType="1">
			                    <wf:extJson>{"settings":{"exceptionHandle":{"enabled":true,"way":1}}}</wf:extJson>
			                </wf:node>
			            </extensionElements>
			            <multiInstanceLoopCharacteristics flowable:isSequential="false"
			                flowable:collection="wfMiAssignees_miApprove" flowable:elementVariable="wfMiAssignee">
			                <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
			            </multiInstanceLoopCharacteristics>
			        </userTask>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="flow_start" sourceRef="startEvent" targetRef="miApprove"/>
			        <sequenceFlow id="flow_end" sourceRef="miApprove" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, procKey);
	}
}
