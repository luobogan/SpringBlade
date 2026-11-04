package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 「归档实例复活」的引擎能力回归：{@link IProcessService#startInstanceAtActivity}。
 *
 * <p>背景：归档走 {@code terminate} → {@code deleteProcessInstance}，引擎运行态已被销毁，
 * Flowable 无法 resume 已结束的流程实例，故归档回退只能「新建运行态 + 把 token 停在目标节点」，
 * 再把 {@code wf_instance.engine_inst_id} 重定向过去（见 WfInstanceServiceImpl#reviveArchivedAt）。
 * 本测试直接驱动<b>真实 Flowable 引擎</b>验证这条引擎能力本身。</p>
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>正常发起 → token 落在首个等待节点（创建节点）；</li>
 *   <li>{@code startInstanceAtActivity} → 新运行态的 token 落在<b>指定目标节点</b>；</li>
 *   <li>目标即首个等待节点时不做多余跳转；</li>
 *   <li>旧运行态被删除（模拟归档）后仍可复活，且旧历史保留（审计不断链）；</li>
 *   <li>入参非法（流程定义ID / 目标节点为空）时快速失败。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfArchiveReviveEngineTest {

	/** 独立 process id，避免污染 classpath:processes 自动部署的定义 */
	private static final String PROC_KEY = "archiveReviveApproval";

	/** 线性流程：创建 → 审批 → 结束（两个可停留 UserTask） */
	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="definitions_archiveReviveApproval">
		    <process id="archiveReviveApproval" name="归档复活验证流程" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <userTask id="createTask" name="填单" flowable:assignee="${approver}"/>
		        <userTask id="approveTask" name="审批" flowable:assignee="${approver}"/>
		        <endEvent id="endEvent" name="结束"/>

		        <sequenceFlow id="flow_start_create" sourceRef="startEvent" targetRef="createTask"/>
		        <sequenceFlow id="flow_create_approve" sourceRef="createTask" targetRef="approveTask"/>
		        <sequenceFlow id="flow_approve_end" sourceRef="approveTask" targetRef="endEvent"/>
		    </process>
		</definitions>
		""";

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private IProcessService processService;

	private String deploymentId;

	@BeforeEach
	void deploy() {
		deploymentId = processService.deployProcess(PROC_KEY, BPMN);
		assertThat(deploymentId).isNotNull();
	}

	@AfterEach
	void cleanup() {
		if (deploymentId != null) {
			processService.deleteDeployment(deploymentId);
			deploymentId = null;
		}
	}

	/** 取当前流程定义ID（最新部署版本） */
	private String latestProcDefId() {
		return processService.latestProcDefId(PROC_KEY);
	}

	/** 取实例当前活动任务所在节点 */
	private String currentNodeKeyOf(String engineInstId) {
		List<TaskVO> tasks = processService.currentTasks(engineInstId);
		assertThat(tasks).hasSize(1);
		return tasks.get(0).getTaskDefinitionKey();
	}

	/** 正常发起 → token 应落在首个等待节点（填单） */
	@Test
	void startInstance_shouldLandOnFirstWaitNode() {
		String instId = processService.startInstance(PROC_KEY, "1:1", Map.of("approver", "tester"));
		assertThat(instId).isNotNull();
		assertThat(currentNodeKeyOf(instId)).isEqualTo("createTask");
	}

	/** 复活到指定节点 → 新运行态的 token 落在目标节点 */
	@Test
	void startInstanceAtActivity_shouldLandOnTargetNode() {
		String newInstId = processService.startInstanceAtActivity(
			latestProcDefId(), "1:1", "approveTask", Map.of("approver", "tester"));

		assertThat(newInstId).isNotNull();
		assertThat(currentNodeKeyOf(newInstId)).isEqualTo("approveTask");
	}

	/** 目标即首个等待节点 → 不做多余跳转，直接停在该节点 */
	@Test
	void startInstanceAtActivity_targetIsFirstNode_shouldStayThere() {
		String newInstId = processService.startInstanceAtActivity(
			latestProcDefId(), "1:1", "createTask", Map.of("approver", "tester"));

		assertThat(currentNodeKeyOf(newInstId)).isEqualTo("createTask");
	}

	/**
	 * 模拟「归档」：删除运行态后仍可复活，且<b>旧历史保留</b>（审计不断链）。
	 *
	 * <p>这正是归档回退的落地路径：归档 = 运行态被删 → 收回 = 新建运行态 + 重定向 engineInstId。
	 * 旧实例的历史（APPROVED）保留，新实例为新的运行态，两者同属一个 bizKey（同一请求）。</p>
	 */
	@Test
	void reviveAfterRuntimeDeleted_shouldWorkAndKeepOldHistory() {
		// ① 正常发起并归档（删除运行态）
		String oldInstId = processService.startInstance(PROC_KEY, "1:1", Map.of("approver", "tester"));
		runtimeService.deleteProcessInstance(oldInstId, "forceArchive");
		assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(oldInstId).count()).isZero();
		// 旧历史仍在（归档审计不丢）
		assertThat(processService.historicProcess(oldInstId)).isNotNull();

		// ② 复活：在目标节点新建运行态（bizKey 沿用 → 同一请求）
		String newInstId = processService.startInstanceAtActivity(
			latestProcDefId(), "1:1", "approveTask", Map.of("approver", "tester"));

		// ③ 新运行态可用：token 停在目标节点
		assertThat(currentNodeKeyOf(newInstId)).isEqualTo("approveTask");
		// ④ 旧实例历史未受影响
		assertThat(processService.historicProcess(oldInstId)).isNotNull();
		assertThat(processService.historicProcess(oldInstId).getEndTime()).isNotNull();
	}

	/** 入参非法 → 快速失败（不静默产出坏实例） */
	@Test
	void startInstanceAtActivity_withBlankArgs_shouldFailFast() {
		assertThatThrownBy(() -> processService.startInstanceAtActivity(null, "1:1", "approveTask", Map.of()))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> processService.startInstanceAtActivity(latestProcDefId(), "1:1", " ", Map.of()))
			.isInstanceOf(IllegalArgumentException.class);
	}

	/** 目标节点不存在 → 引擎跳转失败，不应产出「看似成功」的实例 */
	@Test
	void startInstanceAtActivity_withUnknownNode_shouldFail() {
		assertThatThrownBy(() -> processService.startInstanceAtActivity(
			latestProcDefId(), "1:1", "noSuchNode", Map.of("approver", "tester")))
			.isInstanceOf(Exception.class);
	}

	/** 复活后的实例可继续正常办理（走到结束） */
	@Test
	void revivedInstance_shouldBeAbleToComplete() {
		String newInstId = processService.startInstanceAtActivity(
			latestProcDefId(), "1:1", "approveTask", Map.of("approver", "tester"));

		List<TaskVO> tasks = processService.currentTasks(newInstId);
		assertThat(tasks).hasSize(1);
		processService.completeTask(tasks.get(0).getTaskId(), Map.of());

		// 走完审批 → 流程结束（历史有结束时间）
		assertThat(processService.historicProcess(newInstId).getEndTime()).isNotNull();
	}
}
