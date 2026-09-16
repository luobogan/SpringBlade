package org.springblade.workflow;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实引擎「假数据」流程测试（对应报告《Flowable7真实引擎假数据流程测试落地分析报告》）。
 *
 * <p>本测试不依赖 Nacos / MySQL / Redis，复用 {@link FlowableTestConfig} 的内存 H2 引擎，
 * 直接驱动<b>真实 Flowable7 引擎</b>跑完一套「条件分支」流程，验证：</p>
 * <ul>
 *   <li>报告 §3：真实引擎 + 假数据（表单变量驱动条件分支）的结合方式；</li>
 *   <li>报告 §4.1/§4.2：用 HistoryService 采集「节点覆盖率」，并以多份表单数据枚举排他网关分支；</li>
 *   <li>报告 §7（关键风险）：条件表达式含 {@code ||} 时，真实引擎 UEL 求值与
 *       {@code WfConditionUtil}（仅 split("&&")）行为不一致——真引擎测试可暴露该偏差；</li>
 *   <li>报告 §3.4：测试部署走独立 key，跑完即 {@code deleteDeployment} 卸载，零污染。</li>
 * </ul>
 *
 * <p>注：本测试聚焦「引擎内核 + 条件注入 + 覆盖率」这一层（H2 即可跑）。
 * 语义层（WfInstanceServiceImpl.start / WfTaskServiceImpl.autoApprove 的会签/附加操作/子流程）
 * 需 wf_* 表（MySQL），属报告 §3.4 定义的下一步扩展，见改造说明文档。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfEngineSmokeTest {

	/** 测试部署使用独立 process id（不污染生产 simpleApproval 等定义） */
	private static final String PROC_KEY = "conditionalApproval";

	/** 含排他网关 + 条件分支的 BPMN（内联，避免污染 classpath:processes 自动部署计数）。
	 *  条件 {@code ${amount > 1000 || type == 'A'}} 刻意使用 {@code ||}，用于验证真实引擎 UEL 求值。 */
	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             targetNamespace="http://springblade.workflow"
		             id="definitions_conditionalApproval">
		    <process id="conditionalApproval" name="Conditional Approval" isExecutable="true">
		        <startEvent id="startEvent" name="发起"/>
		        <exclusiveGateway id="amountGateway" name="金额/类型判断" default="flow_default"/>
		        <userTask id="highApproval" name="大额/特殊审批" flowable:assignee="${approver}"/>
		        <userTask id="normalApproval" name="普通审批" flowable:assignee="${approver}"/>
		        <endEvent id="endEvent" name="结束"/>

		        <sequenceFlow id="flow_start_gw" sourceRef="startEvent" targetRef="amountGateway"/>
		        <sequenceFlow id="flow_cond" sourceRef="amountGateway" targetRef="highApproval">
		            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${amount > 1000 || type == 'A'}]]></conditionExpression>
		        </sequenceFlow>
		        <sequenceFlow id="flow_default" sourceRef="amountGateway" targetRef="normalApproval"/>
		        <sequenceFlow id="flow_high_end" sourceRef="highApproval" targetRef="endEvent"/>
		        <sequenceFlow id="flow_normal_end" sourceRef="normalApproval" targetRef="endEvent"/>
		    </process>
		</definitions>
		""";

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private IProcessService processService;

	/** 每个用例独立部署，跑完即卸载 */
	private String deploymentId;

	@BeforeEach
	void deploy() {
		deploymentId = processService.deployProcess(PROC_KEY, BPMN);
		assertThat(deploymentId).isNotNull();
		long defCount = repositoryService.createProcessDefinitionQuery()
			.processDefinitionKey(PROC_KEY).count();
		assertThat(defCount).isGreaterThanOrEqualTo(1L);
	}

	@AfterEach
	void cleanup() {
		if (deploymentId != null) {
			processService.deleteDeployment(deploymentId);
			deploymentId = null;
			// 卸载后定义应不可再发起
			assertThat(repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey(PROC_KEY).count()).isZero();
		}
	}

	/** 报告 §3：用一份表单变量驱动真实引擎跑到归档，返回历史活动 + 流程实例 */
	private RunResult runToEnd(Map<String, Object> vars) {
		String instId = processService.startInstance(PROC_KEY, "1:1", vars);
		assertThat(instId).isNotNull();

		// 排他网关已即时求值，当前应仅有一个用户任务（highApproval 或 normalApproval）
		List<org.springblade.workflow.vo.TaskVO> tasks = processService.currentTasks(instId);
		assertThat(tasks).hasSize(1);

		// 完成该用户任务 → 流程抵达结束节点并归档
		processService.completeTask(tasks.get(0).getTaskId(), Map.of());

		HistoricProcessInstance hp = processService.historicProcess(instId);
		assertThat(hp).isNotNull();
		assertThat(hp.getEndTime()).isNotNull(); // 已结束

		List<HistoricActivityInstance> acts = processService.historicActivities(instId);
		assertThat(acts).isNotEmpty();
		return new RunResult(acts);
	}

	/** 报告 §4.1：覆盖率——断言本次运行必经的节点 */
	private void assertVisited(RunResult r, String... activityIds) {
		Set<String> visited = r.activityIds();
		for (String id : activityIds) {
			assertThat(visited).contains(id);
		}
	}

	/** 报告 §4.2：条件成立（大额）→ 走条件分支 highApproval */
	@Test
	void shouldTakeConditionalBranchWhenConditionTrue() {
		RunResult r = runToEnd(Map.of("amount", 2000, "type", "B", "approver", "tester"));
		assertVisited(r, "startEvent", "amountGateway", "highApproval", "endEvent");
		assertThat(r.activityIds()).doesNotContain("normalApproval");
		// 报告 §3.4：正常流转后不应残留引擎作业
		assertThat(processService.pendingJobCount()).isZero();
	}

	/** 报告 §4.2：条件不成立（小额且非A）→ 走默认分支 normalApproval */
	@Test
	void shouldTakeDefaultBranchWhenConditionFalse() {
		RunResult r = runToEnd(Map.of("amount", 50, "type", "B", "approver", "tester"));
		assertVisited(r, "startEvent", "amountGateway", "normalApproval", "endEvent");
		assertThat(r.activityIds()).doesNotContain("highApproval");
		assertThat(processService.pendingJobCount()).isZero();
	}

	/** 报告 §7（关键风险）：含 {@code ||} 的条件——首个 operand 假、第二个真。
	 *  真实引擎 UEL 判真 → 走条件分支；而 WfConditionUtil 无法解析 {@code ||} 会恒判假 → 走默认。
	 *  本测试断言真实引擎走条件分支，证明配置走查在此会"假阳性"。 */
	@Test
	void shouldHonorOrOperatorUnlikeConfigWalk() {
		RunResult r = runToEnd(Map.of("amount", 50, "type", "A", "approver", "tester"));
		assertVisited(r, "highApproval"); // amount=50(假) || type=='A'(真) → UEL 真
		assertThat(r.activityIds()).doesNotContain("normalApproval");
	}

	/** 报告 §4.1/§4.2：多份数据枚举后，全部节点（含两条分支）均被覆盖 */
	@Test
	void coverageShouldCoverAllNodesAcrossRuns() {
		Set<String> all = new HashSet<>();
		all.addAll(runToEnd(Map.of("amount", 2000, "type", "B", "approver", "tester")).activityIds());
		all.addAll(runToEnd(Map.of("amount", 50, "type", "B", "approver", "tester")).activityIds());
		all.addAll(runToEnd(Map.of("amount", 50, "type", "A", "approver", "tester")).activityIds());

		// 期望覆盖：发起 / 网关 / 两条审批分支 / 结束
		assertThat(all).contains("startEvent", "amountGateway",
			"highApproval", "normalApproval", "endEvent");
	}

	/** 运行结果的轻量封装 */
	private static final class RunResult {
		final List<HistoricActivityInstance> activities;

		RunResult(List<HistoricActivityInstance> activities) {
			this.activities = activities;
		}

		Set<String> activityIds() {
			Set<String> ids = new HashSet<>();
			for (HistoricActivityInstance a : activities) {
				ids.add(a.getActivityId());
			}
			return ids;
		}
	}
}
