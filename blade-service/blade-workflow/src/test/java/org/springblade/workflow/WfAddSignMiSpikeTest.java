package org.springblade.workflow;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 加签方案 X / Y 选型的引擎能力 spike（H2，无生产代码改动）。
 *
 * <p>《去wf_表-任务读源翻源上线检查.md》§11.4.2 决策点 / §11.9.2 起步项 ①。
 * 目标不是重复实现已有实证，而是补齐 {@code WfEngineAdvancedSemanticsTest} 未覆盖的两个缺口：</p>
 *
 * <ul>
 *   <li><b>V3 既有结论</b>（{@code WfEngineAdvancedSemanticsTest:232-262}）：对<b>已部署为 MI</b> 的节点
 *       {@code runtimeService.addMultiInstanceExecution("miApprove", instId, Map.of("wfMiAssignee","1004"))}
 *       可用 —— 生成独立活跃引擎任务、{@code nrOfInstances} 变 4、完成条件按新基数求值。</li>
 *   <li><b>V6 既有结论</b>（同文件 {@code :335-369}）：动态加签<b>不会写回集合变量</b>
 *       {@code wfMiAssignees_<nodeKey>}，若按集合重建时该变量只含配置操作者，加签人即丢失。
 *       但注意：本类 {@code writeBackCollectionKeepsAddedAssigneeAfterReject} 用<b>空 vars</b>调
 *       {@code moveActivity}，并未触发「按配置覆盖」——属<b>假阳性</b>。真实 {@code reject} 会传
 *       非空（纯配置）vars，会覆盖集合变量丢人。真正的守护在生产的
 *       {@code WfTaskServiceImpl#mergeMiAssignees}（见 {@code WfTaskServiceImplMiMergeTest}）。</li>
 * </ul>
 *
 * <p>本类新增两个用例：</p>
 * <ol>
 *   <li>{@link #plainUserTaskRejectsDynamicAddMi()}：对<b>普通 UserTask（无 multiInstanceLoopCharacteristics）</b>
 *       调用同一 API。Flowable 8.1 的 {@code AddMultiInstanceExecutionCmd#searchForMultiInstanceActivity}
 *       只匹配 {@code isMultiInstanceRoot()} 的 execution，普通节点返回 null →
 *       {@code FlowableException("No multi instance execution found for activity id ...")}。
 *       <b>结论：方案 X 只对「部署期已注入 MI 的节点」有效</b>，不能给任意普通节点运行中动态加签。</li>
 *   <li>{@link #writeBackCollectionKeepsAddedAssigneeAfterReject()}：MI 节点动态加签后，
 *       <b>手动把加签人写回流程实例级集合变量</b>，再驳回重建，观察加签人是否保留。
 *       用于验证 V6 缺陷能否由生产侧补偿消除（若可行，方案 X 需附带该补偿）。</li>
 * </ol>
 *
 * <p><b>实测结论（2026-11-10，H2 / Flowable 8.1.0-SNAPSHOT，2/2 通过）</b>：</p>
 * <ul>
 *   <li>用例 1：普通 UserTask 调用 {@code addMultiInstanceExecution} 抛
 *       {@code FlowableException("No multi instance execution found for activity id approval")}，
 *       原任务不受影响 → <b>方案 X 只能用于部署期已注入 MI 的节点</b>。</li>
 *   <li>用例 2：加签后 {@code runtimeService.setVariable(instId, "wfMiAssignees_miApprove",
 *       [1001,1002,1003,1004])}，再<b>空 vars</b>驳回重建后办理人 =
 *       {@code [1001, 1002, 1003, 1004]}。⚠️ 此用例仅证明「引擎按集合变量重建实例」的契约，
 *       非生产安全证明——真实 reject 传非空（纯配置）vars 会覆盖丢人；生产守护是
 *       {@code mergeMiAssignees}（集合变量已存在时保留运行期加签人）。回归见
 *       {@code WfTaskServiceImplMiMergeTest}。</li>
 * </ul>
 *
 * <p><b>X / Y 选型结论</b>：项目已开启 {@code engine-multi-instance.enabled=true} 且定义已重新部署，
 * 目标审批节点为 MI 形态，故 <b>方案 X 可行（推荐）</b> —— 加签走
 * {@code addMultiInstanceExecution} + 集合变量写回，可生成独立 ACT 任务、N:1 守卫可删，
 * 无需补建 {@code wf_ext_task type=2} 办理链。
 * <b>边界</b>：若业务要求「给未部署为 MI 的普通节点运行中动态加签」，方案 X 不适用
 * （引擎直接拒绝），需先改造 BPMN/重新部署为该节点注入 MI，或退 §9.4 方案 Y。</p>
 *
 * <p>运行方式（JDK21、离线）：
 * {@code mvn -o -pl blade-service/blade-workflow -am test -Dtest=WfAddSignMiSpikeTest
 * -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false}</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfAddSignMiSpikeTest {

	private static final List<String> ASSIGNEES = List.of("1001", "1002", "1003");
	private static final String ADDED = "1004";

	@Autowired
	private TaskService taskService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private IProcessService processService;

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

	/** 普通 UserTask（无 MI 特性）：start → approval → end */
	private static String plainBpmn(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="Plain %s" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="approval" name="普通审批" flowable:assignee="2001"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="approval"/>
			        <sequenceFlow id="f2" sourceRef="approval" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, procKey);
	}

	/** 与 V3/V6 同形态的 MI 节点：start → miApprove(MI) → nodeC → end */
	private static String miThenCBpmn(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="MI %s" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="miApprove" name="会签节点" flowable:assignee="${wfMiAssignee}">
			            <multiInstanceLoopCharacteristics flowable:isSequential="false"
			                flowable:collection="wfMiAssignees_miApprove" flowable:elementVariable="wfMiAssignee">
			                <completionCondition><![CDATA[${nrOfCompletedInstances == nrOfInstances}]]></completionCondition>
			            </multiInstanceLoopCharacteristics>
			        </userTask>
			        <userTask id="nodeC" name="复核" flowable:assignee="2001"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="miApprove"/>
			        <sequenceFlow id="f2" sourceRef="miApprove" targetRef="nodeC"/>
			        <sequenceFlow id="f3" sourceRef="nodeC" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, procKey);
	}

	private String deploy(String procKey, String bpmn) {
		String depId = processService.deployProcess(procKey, bpmn);
		assertThat(depId).isNotNull();
		deployed.add(depId);
		return depId;
	}

	private String startMi(String procKey) {
		Map<String, Object> vars = new HashMap<>();
		vars.put("wfMiAssignees_miApprove", new ArrayList<>(ASSIGNEES));
		return processService.startInstance(procKey, "1:1", vars);
	}

	private Set<String> activeAssignees(String instId) {
		Set<String> set = new HashSet<>();
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			set.add(t.getAssignee());
		}
		return set;
	}

	@Test
	@DisplayName("缺口1：普通 UserTask 运行中动态加签 → Flowable 拒绝，方案 X 仅适用 MI 部署节点")
	void plainUserTaskRejectsDynamicAddMi() {
		String procKey = "spikePlainAddSign";
		deploy(procKey, plainBpmn(procKey));
		String instId = processService.startInstance(procKey, "1:1", new HashMap<>());
		assertThat(instId).isNotNull();
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(1);

		assertThatThrownBy(() -> runtimeService.addMultiInstanceExecution(
			"approval", instId, Map.of("wfMiAssignee", ADDED)))
			.isInstanceOf(FlowableException.class)
			.hasMessageContaining("No multi instance execution found");

		// 无副作用：原任务仍在，没有凭空多出加签任务
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(1);
		assertThat(activeAssignees(instId)).containsOnly("2001");
	}

	@Test
	@DisplayName("缺口2：动态加签后写回集合变量，驳回重建是否保留加签人")
	void writeBackCollectionKeepsAddedAssigneeAfterReject() {
		String procKey = "spikeMiWriteBack";
		deploy(procKey, miThenCBpmn(procKey));
		String instId = startMi(procKey);
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(3);

		// V3 已实证：MI 节点动态加签可用
		runtimeService.addMultiInstanceExecution("miApprove", instId, Map.of("wfMiAssignee", ADDED));
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(4);

		// V6 的生产侧补偿候选：把加签人并回流程实例级集合变量
		List<String> withAdded = new ArrayList<>(ASSIGNEES);
		withAdded.add(ADDED);
		runtimeService.setVariable(instId, "wfMiAssignees_miApprove", new ArrayList<>(withAdded));

		// 全部办结 → 到达 nodeC
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			taskService.complete(t.getId());
		}
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);

		// 驳回回 MI 节点：changeState 按集合变量重建实例。
		// ⚠️ 注意这里传的是空 vars（Map.of()），并未触发「按配置覆盖」——真实 reject 会传非空
		// （纯配置）vars，会覆盖集合变量丢人。此断言仅证明「引擎按集合变量重建」契约成立；
		// 生产侧的丢人守护见 WfTaskServiceImpl.mergeMiAssignees。
		processService.moveActivity(instId, "nodeC", "miApprove", Map.of());

		// 空 vars 下集合变量未被覆盖，加签人 1004 在驳回重建后仍保留
		Set<String> rebuilt = activeAssignees(instId);
		assertThat(rebuilt).containsExactlyInAnyOrder("1001", "1002", "1003", ADDED);
	}
}
