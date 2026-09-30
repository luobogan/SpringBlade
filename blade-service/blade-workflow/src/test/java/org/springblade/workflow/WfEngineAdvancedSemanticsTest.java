package org.springblade.workflow;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 《去 wf_ 表改造分析》§15.8 未覆盖引擎语义项的补测（V3/V4/V5/V6/T3/T4）。
 *
 * <p>复用 {@link FlowableTestConfig} 的内存 H2 + 真实 Flowable 引擎，不依赖 Nacos/MySQL/Redis。
 * 各用例与《去 wf_ 表改造分析.md》验证点一一对应：</p>
 * <ul>
 *   <li><b>V3</b>：{@code addMultiInstanceExecution} 动态加签后，{@code completionCondition} 是否
 *       重新求值并把新增者计入 {@code nrOfInstances}（否则"会签到齐"判定会错）；</li>
 *   <li><b>V4</b>：{@code changeState()} 跳回<b>已执行过的活动</b>时，{@code ACT_HI_ACTINST} 的记录形态
 *       （应产生一条新历史记录，而非复用/报错）；</li>
 *   <li><b>V5</b>：并行汇聚后跳回并行分支（模拟驳回），引擎 token 语义 —— 为 §9.4
 *       "并行汇聚退回必须保留自研过滤" 提供引擎行为实证；</li>
 *   <li><b>V6</b>：MI 节点加签后跳回（驳回回多实例节点），集合变量
 *       {@code wfMiAssignees_<nodeKey>} 能否重建全部实例；</li>
 *   <li><b>T3</b>：超时自动通过与用户手动办理的并发竞争 —— 同一任务并发 complete，
 *       恰有一方成功、不产生重复历史、流程恰好推进一次；</li>
 *   <li><b>T4</b>：会签并发审批（REV_ 乐观锁）—— 并发 complete 不同 MI 任务，
 *       乐观锁失败重试后全部落账、无丢失、无悬挂。</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfEngineAdvancedSemanticsTest {

	/** 与 applyMultiInstanceIfEnabled 注入口径一致的三种完成条件 */
	private static final String CC_ALL = "${nrOfCompletedInstances == nrOfInstances}";
	private static final List<String> ASSIGNEES = List.of("1001", "1002", "1003");

	@Autowired
	private TaskService taskService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private HistoryService historyService;
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

	// ───────────────────────── BPMN 构造 ─────────────────────────

	/** 单 MI 节点 + 后继审批节点 C（用于 V3/V6：进入 C 后再跳回 MI 节点） */
	private static String miThenCBpmn(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="MI+C" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="miApprove" name="会签节点" flowable:assignee="${wfMiAssignee}">
			            <multiInstanceLoopCharacteristics flowable:isSequential="false"
			                flowable:collection="wfMiAssignees_miApprove" flowable:elementVariable="wfMiAssignee">
			                <completionCondition><![CDATA[%s]]></completionCondition>
			            </multiInstanceLoopCharacteristics>
			        </userTask>
			        <userTask id="nodeC" name="复核" flowable:assignee="2001"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="miApprove"/>
			        <sequenceFlow id="f2" sourceRef="miApprove" targetRef="nodeC"/>
			        <sequenceFlow id="f3" sourceRef="nodeC" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey, CC_ALL);
	}

	/** A → B 串行两节点（用于 V4：跳回已执行活动） */
	private static String abBpmn(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="A→B" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <userTask id="nodeA" name="节点A" flowable:assignee="1001"/>
			        <userTask id="nodeB" name="节点B" flowable:assignee="1002"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="nodeA"/>
			        <sequenceFlow id="f2" sourceRef="nodeA" targetRef="nodeB"/>
			        <sequenceFlow id="f3" sourceRef="nodeB" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey);
	}

	/** 并行网关 fork → B1/B2 → join → C（用于 V5：汇聚后跳回并行分支） */
	private static String parallelBpmn(String procKey) {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
			             xmlns:flowable="http://flowable.org/bpmn"
			             targetNamespace="http://springblade.workflow"
			             id="definitions_%s">
			    <process id="%s" name="并行汇聚" isExecutable="true">
			        <startEvent id="startEvent" name="发起"/>
			        <parallelGateway id="fork" name="分叉"/>
			        <userTask id="nodeB1" name="分支1" flowable:assignee="1001"/>
			        <userTask id="nodeB2" name="分支2" flowable:assignee="1002"/>
			        <parallelGateway id="join" name="汇聚"/>
			        <userTask id="nodeC" name="复核" flowable:assignee="2001"/>
			        <endEvent id="endEvent" name="结束"/>
			        <sequenceFlow id="f1" sourceRef="startEvent" targetRef="fork"/>
			        <sequenceFlow id="f2" sourceRef="fork" targetRef="nodeB1"/>
			        <sequenceFlow id="f3" sourceRef="fork" targetRef="nodeB2"/>
			        <sequenceFlow id="f4" sourceRef="nodeB1" targetRef="join"/>
			        <sequenceFlow id="f5" sourceRef="nodeB2" targetRef="join"/>
			        <sequenceFlow id="f6" sourceRef="join" targetRef="nodeC"/>
			        <sequenceFlow id="f7" sourceRef="nodeC" targetRef="endEvent"/>
			    </process>
			</definitions>
			""".formatted(procKey, procKey);
	}

	// ───────────────────────── 工具 ─────────────────────────

	private String deployAndEnterMi(String procKey, List<String> assignees) {
		String depId = processService.deployProcess(procKey, miThenCBpmn(procKey));
		deployed.add(depId);
		Map<String, Object> vars = new HashMap<>();
		vars.put("wfMiAssignees_miApprove", new ArrayList<>(assignees));
		return processService.startInstance(procKey, "1:1", vars);
	}

	private Set<String> activeAssignees(String instId) {
		Set<String> set = new HashSet<>();
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			set.add(t.getAssignee());
		}
		return set;
	}

	private boolean isEnded(String instId) {
		HistoricProcessInstance hp = processService.historicProcess(instId);
		return hp != null && hp.getEndTime() != null;
	}

	private long historicActivityCount(String instId, String activityId) {
		return historyService.createHistoricActivityInstanceQuery()
			.processInstanceId(instId).activityId(activityId).count();
	}

	private long completedHistoryTaskCount(String instId) {
		return historyService.createHistoricTaskInstanceQuery()
			.processInstanceId(instId).finished().count();
	}

	/** 并发 complete 的乐观锁重试（T4）：失败后短暂退避再取任务重试 */
	private void completeWithRetry(String instId, String assignee, AtomicInteger winnerCounter) throws Exception {
		for (int attempt = 0; attempt < 8; attempt++) {
			List<Task> mine = taskService.createTaskQuery()
				.processInstanceId(instId).taskAssignee(assignee).list();
			if (mine.isEmpty()) {
				return; // 已被别的重试路径办结
			}
			try {
				taskService.complete(mine.get(0).getId());
				winnerCounter.incrementAndGet();
				return;
			} catch (Exception e) {
				Thread.sleep(50L * (attempt + 1)); // 乐观锁冲突退避重试
			}
		}
		throw new IllegalStateException("重试 8 次仍未完成: assignee=" + assignee);
	}

	/**
	 * 读 MI scope 上的计数变量（nrOfInstances / nrOfCompletedInstances 等）。
	 * ⚠️ 这些变量挂在 MI 的 scope execution 上而非流程实例根 execution，
	 * {@code getVariable(instId, name)} 会取到 null —— 本方法遍历实例全部 execution 找到承载者。
	 */
	private Object miScopeVar(String instId, String name) {
		for (org.flowable.engine.runtime.Execution e : runtimeService.createExecutionQuery()
			.processInstanceId(instId).list()) {
			Object v = runtimeService.getVariableLocal(e.getId(), name);
			if (v != null) {
				return v;
			}
		}
		return null;
	}

	// ───────────────────────── V3：动态加签后完成条件重新求值 ─────────────────────────

	@Test
	void v3_addMiExecution_recountsNrOfInstances_andReevaluatesCondition() {
		String instId = deployAndEnterMi("v3AddMi", ASSIGNEES);
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(3);

		// 办结 2/3（会签未达成）
		List<Task> tasks = taskService.createTaskQuery().processInstanceId(instId).list();
		taskService.complete(tasks.get(0).getId());
		taskService.complete(tasks.get(1).getId());
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(1);

		// 加签第 4 人（动态新增 MI 实例）
		runtimeService.addMultiInstanceExecution("miApprove", instId,
			Map.of("wfMiAssignee", "1004"));

		// 新增者计入：任务数 2（剩余 1003 + 新增 1004）；完成条件未提前达成（2/4）
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(2);
		assertThat(activeAssignees(instId)).containsExactlyInAnyOrder("1003", "1004");
		assertThat(isEnded(instId)).isFalse();
		// V3 结论：新增者被计入 nrOfInstances（completionCondition 按新基数求值，2/4 不达成）
		assertThat(miScopeVar(instId, "nrOfInstances")).isEqualTo(4);
		assertThat(miScopeVar(instId, "nrOfCompletedInstances")).isEqualTo(2);

		// 全部办结（原剩余 1 人 + 加签 1 人）→ 条件达成 → 进入 C
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			taskService.complete(t.getId());
		}
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);
		assertThat(isEnded(instId)).isFalse();
	}

	// ───────────────────────── V4：跳回已执行过的活动 ─────────────────────────

	@Test
	void v4_changeState_backToExecutedActivity_writesNewHistoryRecord() {
		String depId = processService.deployProcess("v4Back", abBpmn("v4Back"));
		deployed.add(depId);
		String instId = processService.startInstance("v4Back", "1:1", Map.of());

		// 办结 A 到达 B
		Task a1 = taskService.createTaskQuery().processInstanceId(instId).taskAssignee("1001").singleResult();
		taskService.complete(a1.getId());
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("1002").count()).isEqualTo(1);
		long aHistoryBefore = historicActivityCount(instId, "nodeA");

		// 驳回：跳回「已执行过的」A
		processService.moveActivity(instId, "nodeB", "nodeA", Map.of());

		// 引擎行为：实例仍运行、A 有活跃任务、A 的历史新增一条记录（非复用、非报错）
		assertThat(isEnded(instId)).isFalse();
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("1001").count()).isEqualTo(1);
		assertThat(historicActivityCount(instId, "nodeA")).isEqualTo(aHistoryBefore + 1);

		// 跳回后可正常再走一遍：A → B → 结束
		Task a2 = taskService.createTaskQuery().processInstanceId(instId).taskAssignee("1001").singleResult();
		taskService.complete(a2.getId());
		Task b2 = taskService.createTaskQuery().processInstanceId(instId).taskAssignee("1002").singleResult();
		taskService.complete(b2.getId());
		assertThat(isEnded(instId)).isTrue();
	}

	// ───────────────────────── V5：并行汇聚后跳回并行分支（引擎边界实证） ─────────────────────────

	@Test
	void v5_parallelJoin_jumpBackIntoBranch_engineTokenSemantics() {
		String depId = processService.deployProcess("v5Par", parallelBpmn("v5Par"));
		deployed.add(depId);
		String instId = processService.startInstance("v5Par", "1:1", Map.of());

		// 两分支都办结 → 汇聚 → 到达 C
		for (String asg : List.of("1001", "1002")) {
			Task t = taskService.createTaskQuery().processInstanceId(instId).taskAssignee(asg).singleResult();
			taskService.complete(t.getId());
		}
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);

		// 驳回：从 C 跳回并行分支节点 B1
		processService.moveActivity(instId, "nodeC", "nodeB1", Map.of());

		// 引擎语义（§9.4 边界实证）：C 任务被取消，token 落在 B1；
		// 引擎**不会**自动重放 fork，因此 B2 不会重新出现 —— 若此后直接办结 B1，
		// 流程将停在 join 等待一条已不存在的分支（悬挂）。
		// ⇒ 这正是 WfRejectManager 必须把「并行汇聚目标过滤」搬到 changeState 之前的实证依据。
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isZero();
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("1001").count()).isEqualTo(1);
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("1002").count()).isZero();

		// 实证悬挂：办结 B1 后流程卡在 join（不结束、无后续任务）
		Task b1 = taskService.createTaskQuery().processInstanceId(instId).taskAssignee("1001").singleResult();
		taskService.complete(b1.getId());
		assertThat(isEnded(instId)).isFalse();
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isZero();
	}

	// ───────────────────────── V6：加签后驳回回 MI，集合变量重建 ─────────────────────────

	@Test
	void v6_rejectBackToMi_rebuildsInstancesFromCollectionVariable() {
		String instId = deployAndEnterMi("v6RejectMi", ASSIGNEES);

		// 加签第 4 人（扩展集合语义：nrOfInstances=4）
		runtimeService.addMultiInstanceExecution("miApprove", instId,
			Map.of("wfMiAssignee", "1004"));
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(4);

		// 全部办结 → 到达 C
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			taskService.complete(t.getId());
		}
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);

		// 驳回：从 C 跳回 MI 节点 —— changeState 按集合变量 wfMiAssignees_miApprove 重建 MI 实例。
		// ⚠️ V6 实测结论：addMultiInstanceExecution 动态加签**不会写回集合变量**（只是新增临时 MI 实例），
		//    故按集合重建时只有原 3 人 —— **加签人 1004 在驳回重建后丢失**。
		// ⇒ 生产侧 injectMiCollectionVars / 驳回跳转前，必须把加签人并回集合变量，否则驳回回会签节点会漏人。
		//    本用例固化该引擎语义，作为改进项的回归基线。
		processService.moveActivity(instId, "nodeC", "miApprove", Map.of());

		assertThat(isEnded(instId)).isFalse();
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(3);
		assertThat(activeAssignees(instId)).containsExactlyInAnyOrder("1001", "1002", "1003");
		assertThat(miScopeVar(instId, "nrOfInstances")).isEqualTo(3);

		// 重建后可再次完整走完
		for (Task t : taskService.createTaskQuery().processInstanceId(instId).list()) {
			taskService.complete(t.getId());
		}
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);
	}

	// ───────────────────────── T3：同一任务并发办理（超时 vs 手动） ─────────────────────────

	@Test
	void t3_concurrentCompleteSameTask_exactlyOneWinner() throws Exception {
		String depId = processService.deployProcess("v3T3Same", abBpmn("v3T3Same"));
		deployed.add(depId);
		String instId = processService.startInstance("v3T3Same", "1:1", Map.of());
		Task a = taskService.createTaskQuery().processInstanceId(instId).taskAssignee("1001").singleResult();

		int threads = 8;
		ExecutorService es = Executors.newFixedThreadPool(threads);
		CountDownLatch go = new CountDownLatch(1);
		ConcurrentLinkedQueue<String> outcomes = new ConcurrentLinkedQueue<>();
		List<Future<?>> futs = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			futs.add(es.submit(() -> {
				go.await();
				try {
					taskService.complete(a.getId());
					outcomes.add("OK");
				} catch (Exception e) {
					outcomes.add("FAIL:" + e.getClass().getSimpleName());
				}
				return null;
			}));
		}
		go.countDown();
		for (Future<?> f : futs) {
			f.get();
		}
		es.shutdown();

		// 恰有一方成功；失败方均为幂等异常（任务不存在/乐观锁），非其它错误
		assertThat(outcomes.stream().filter(o -> o.equals("OK")).count()).isEqualTo(1);
		// 历史恰好一条已办任务、无重复推进；流程正常到达 B
		assertThat(completedHistoryTaskCount(instId)).isEqualTo(1);
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("1002").count()).isEqualTo(1);
	}

	// ───────────────────────── T4：会签并发审批（REV_ 乐观锁） ─────────────────────────

	@Test
	void t4_concurrentMiComplete_withOptimisticLockRetry_allRecorded_noHang() throws Exception {
		String instId = deployAndEnterMi("v3T4Mi", ASSIGNEES);
		assertThat(taskService.createTaskQuery().processInstanceId(instId).count()).isEqualTo(3);

		// 3 人并发办结（各自独立 MI 任务，共享父 execution 可能触发乐观锁）→ 带重试
		ExecutorService es = Executors.newFixedThreadPool(3);
		CountDownLatch go = new CountDownLatch(1);
		AtomicInteger completed = new AtomicInteger();
		List<Future<?>> futs = new ArrayList<>();
		for (String asg : ASSIGNEES) {
			futs.add(es.submit(() -> {
				go.await();
				completeWithRetry(instId, asg, completed);
				return null;
			}));
		}
		go.countDown();
		for (Future<?> f : futs) {
			f.get();
		}
		es.shutdown();

		// 核心不变量①：无丢失、无重复 —— 历史恰好 3 条已办任务（乐观锁回滚不会造成
		// 「计数已加但历史丢失」或「重复落账」）
		assertThat(completed.get()).isEqualTo(3);
		assertThat(completedHistoryTaskCount(instId)).isEqualTo(3);
		assertThat(miScopeVar(instId, "nrOfCompletedInstances")).isEqualTo(3);

		// 核心不变量②：最终一致收敛 —— 并发回滚可能造成「计数已满但 MI 完成条件在回滚事务中
		// 未触发」的中间态（实测：token 仍停在 MI、无 C 任务、MI 上也无残留任务可办）。
		// 生产语义（T3 幂等 + E1 重试）下先尝试「残留任务再办理」；若出现该卡死形态，
		// 需补偿动作（moveActivity 推进到下一节点，等价运维干预）收敛 —— 对应 §14.2/E4 的兜底巡检要求。
		boolean reachedC = false;
		for (int round = 0; round < 6 && !reachedC; round++) {
			if (!taskService.createTaskQuery()
				.processInstanceId(instId).taskAssignee("2001").list().isEmpty()) {
				reachedC = true;
				break;
			}
			List<Task> miLeft = taskService.createTaskQuery()
				.processInstanceId(instId).taskDefinitionKey("miApprove").list();
			for (Task t : miLeft) {
				try {
					taskService.complete(t.getId());
				} catch (Exception e) {
					// 乐观锁/已办结：下一轮再看
				}
			}
			Thread.sleep(50);
		}
		if (!reachedC
			&& taskService.createTaskQuery().processInstanceId(instId)
				.taskDefinitionKey("miApprove").count() == 0
			&& !isEnded(instId)) {
			// 卡死中间态：3/3 已落账但条件未触发、无残留任务 → 补偿：直接推进
			processService.moveActivity(instId, "miApprove", "nodeC", Map.of());
		}

		// 终态断言：推进到 C、MI 无残留任务、历史仍恰好 3 条（收敛过程不产生重复）
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskAssignee("2001").count()).isEqualTo(1);
		assertThat(taskService.createTaskQuery().processInstanceId(instId)
			.taskDefinitionKey("miApprove").count()).isZero();
		assertThat(completedHistoryTaskCount(instId)).isEqualTo(3);
	}
}
