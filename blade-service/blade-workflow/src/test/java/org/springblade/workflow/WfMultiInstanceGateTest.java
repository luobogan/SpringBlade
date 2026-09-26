package org.springblade.workflow;

import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会签/或签/依次「引擎多实例」门禁的<b>引擎侧</b>验证（对应《下沉迁移方案》§6.5 验证清单的引擎内核部分）。
 *
 * <p>本测试不依赖 Nacos / MySQL / Redis，复用 {@link FlowableTestConfig} 的内存 H2 引擎，
 * 直接驱动<b>真实 Flowable7 引擎</b>，部署与 {@code WfDefinitionServiceImpl#applyMultiInstanceIfEnabled}
 * <b>完全相同形态</b>的多实例 BPMN（assignee=${wfMiAssignee} + 三种完成条件），
 * 验证引擎按人拆分任务、并按完成条件驱动放行/取消兄弟任务。</p>
 *
 * <p>覆盖 §6.5 清单的引擎内核项：</p>
 * <ul>
 *   <li>会签（parallel-all）：全部办完才推进；</li>
 *   <li>或签（parallel-or）：首办即过，引擎取消其余 MI 任务；</li>
 *   <li>依次（sequential）：逐人激活，最后一人办完才推进；</li>
 *   <li>集合变量 {@code wfMiAssignees_<nodeKey>} 在进入节点前写入即可逐人展开（对应 injectMiCollectionVars）。</li>
 * </ul>
 *
 * <p><b>wf_task 侧</b>（doApprove 跳过自研计数、advance 按「每条引擎任务=一人」生成、兄弟待办对账）
 * 依赖 wf_* 表（MySQL），请复用 {@code WfTestServiceImpl#run} 跑一条配置了会签/或签/依次节点的真实定义，
 * 按 §6.5 第 1/2/3/4/5/6 项断言 wf_task 行数，本测试不重复覆盖。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfMultiInstanceGateTest {

	/** 三种会签语义的完成条件（与 applyMultiInstanceIfEnabled 注入口径一致） */
	private static final String CC_ALL = "${nrOfCompletedInstances == nrOfInstances}";
	private static final String CC_OR = "${nrOfCompletedInstances >= 1}";
	private static final String CC_SEQ = "${nrOfCompletedInstances == nrOfInstances}";

	/** 测试办理人（字符串形态，对齐运行期 operatorResolver 解析出的 Long 经 toString 落库的形态） */
	private static final List<String> ASSIGNEES = List.of("1001", "1002", "1003");

	@Autowired
	private TaskService taskService;
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

	// ───────────────────────── 工具 ─────────────────────────

	/** 构建与 applyMultiInstanceIfEnabled 同形态的多实例 BPMN（nodeKey 固定为 miApprove） */
	private static String bpmn(String procKey, boolean sequential, String completionCondition) {
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
			            <multiInstanceLoopCharacteristics flowable:isSequential="%s"
			                flowable:collection="wfMiAssignees_miApprove" flowable:elementVariable="wfMiAssignee">
			                <completionCondition><![CDATA[%s]]></completionCondition>
			            </multiInstanceLoopCharacteristics>
			        </userTask>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="flow_start" sourceRef="startEvent" targetRef="miApprove"/>
			        <sequenceFlow id="flow_end" sourceRef="miApprove" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, procKey, sequential ? "true" : "false", completionCondition);
	}

	/** 部署 + 进入节点（集合变量在进入前写入，模拟 injectMiCollectionVars），返回流程实例ID */
	private String deployAndEnter(String procKey, boolean sequential, String completionCondition) {
		String depId = processService.deployProcess(procKey, bpmn(procKey, sequential, completionCondition));
		assertThat(depId).isNotNull();
		deployed.add(depId);

		java.util.Map<String, Object> vars = new java.util.HashMap<>();
		vars.put("wfMiAssignees_miApprove", new ArrayList<>(ASSIGNEES));
		String instId = processService.startInstance(procKey, "1:1", vars);
		assertThat(instId).isNotNull();
		return instId;
	}

	/** 当前活跃引擎任务（MI 节点）的办理人集合 */
	private Set<String> activeAssignees(String instId) {
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(instId).list();
		Set<String> set = new HashSet<>();
		for (Task t : tasks) {
			set.add(t.getAssignee());
		}
		return set;
	}

	private boolean isEnded(String instId) {
		HistoricProcessInstance hp = processService.historicProcess(instId);
		return hp != null && hp.getEndTime() != null;
	}

	// ───────────────────────── 会签（parallel-all） ─────────────────────────

	@Test
	void counterSignAll_shouldExpandPerPersonAndWaitForAll() {
		String instId = deployAndEnter("miCounterAll", false, CC_ALL);

		// 进入节点后应为每人各一条引擎任务（逐人拆分）
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(ASSIGNEES.size());
		assertThat(activeAssignees(instId)).containsExactlyInAnyOrderElementsOf(ASSIGNEES);
		assertThat(isEnded(instId)).isFalse();

		// 只办完 2/3，流程不应结束（会签未达成）
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(instId).list();
		taskService.complete(tasks.get(0).getId());
		taskService.complete(tasks.get(1).getId());
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(1);
		assertThat(isEnded(instId)).isFalse();

		// 第 3 人办结 → 完成条件达成 → 流程结束
		List<Task> last = taskService.createTaskQuery().processInstanceId(instId).list();
		taskService.complete(last.get(0).getId());
		assertThat(isEnded(instId)).isTrue();
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isZero();
	}

	// ───────────────────────── 或签（parallel-or） ─────────────────────────

	@Test
	void orSign_shouldCancelSiblingsOnFirstCompletion() {
		String instId = deployAndEnter("miOrSign", false, CC_OR);

		// 进入节点后应为每人各一条引擎任务
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(ASSIGNEES.size());
		assertThat(activeAssignees(instId)).containsExactlyInAnyOrderElementsOf(ASSIGNEES);

		// 首办即过 → 完成条件达成（>=1）→ 引擎取消其余 MI 任务并结束
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(instId).list();
		taskService.complete(tasks.get(0).getId());

		assertThat(isEnded(instId)).isTrue();
		// 兄弟任务被引擎取消：不应残留活跃任务
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isZero();
	}

	// ───────────────────────── 依次（sequential） ─────────────────────────

	@Test
	void sequentialSign_shouldActivateOneAtATime() {
		String instId = deployAndEnter("miSequential", true, CC_SEQ);

		// 依次：进入节点后仅一条引擎任务（第一个人）
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(1);
		assertThat(isEnded(instId)).isFalse();

		// 逐人办理：每次办结后仅激活下一人，直到最后一人办结才结束
		for (int i = 0; i < ASSIGNEES.size(); i++) {
			List<Task> tasks = taskService.createTaskQuery().processInstanceId(instId).list();
			assertThat(tasks).hasSize(1);
			taskService.complete(tasks.get(0).getId());
		}
		assertThat(isEnded(instId)).isTrue();
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isZero();
	}
}
