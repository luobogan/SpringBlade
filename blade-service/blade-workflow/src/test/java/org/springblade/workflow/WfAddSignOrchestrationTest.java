package org.springblade.workflow;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.helper.WfTaskActDirectWriter;
import org.springblade.workflow.service.helper.WfTaskBizResolver;
import org.springblade.workflow.service.helper.TaskBizRecord;
import org.springblade.workflow.service.helper.WfWriteHelper;
import org.springblade.workflow.service.impl.WfTaskServiceImpl;
import org.springblade.workflow.utils.WfAuthUtil;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 加签业务入口（{@code WfTaskServiceImpl#addSign}）编排层验证（D17 方案 X）。
 *
 * <p>《去wf_表-任务读源翻源上线检查.md》§12.6.4 未验证项的补做：dev 库现成会签待办均为
 * <b>子流程实例</b>（{@code addMultiInstanceExecution} 需 MI root 而非流程实例 ID，真实 HTTP 已验证
 * 此路径正确报「未启用多人会签」），且测试套件为轻量 Flowable 上下文（无 MyBatis），无法注入
 * {@code WfTaskServiceImpl} 跑完整 Spring Bean。故本类以 Mockito 装配 {@code WfTaskServiceImpl}，
 * 验证 {@code addSign} 业务方法体在「成功路径」与「降级路径」下的编排串联。</p>
 *
 * <p>配套实证（互不重叠）：</p>
 * <ul>
 *   <li>{@code WfAddSignMiSpikeTest} 2/2：引擎层 {@code addMultiInstanceExecution} 在<b>顶层 MI 实例</b>
 *       可用（生成独立引擎任务）、集合变量写回后驳回重建保留加签人（V6 补偿有效）；</li>
 *   <li>{@code WfTaskActWriterTest#writeFull*}：新任务写独立 {@code BIZ_TASK_ID_}；</li>
 *   <li>dev HTTP：非 MI 节点调 addSign 正确报友好错误（降级路径，无副作用）。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WfAddSignOrchestrationTest {

	@Mock private WfInstanceMapper instanceMapper;
	@Mock private WfTaskBizResolver bizResolver;
	@Mock private RuntimeService runtimeService;
	@Mock private TaskService flowableTaskService;
	@Mock private WfTaskActDirectWriter directWriter;
	@Mock private WfApprovalLogMapper logMapper;
	@Mock private WfWriteHelper writeHelper;

	@InjectMocks
	private WfTaskServiceImpl taskService;

	private static final Long TASK_ID = 9000000000001000321L;
	private static final Long INST_ID = 9000000000000000321L;
	private static final String ENGINE_INST = "PI_TOP_MI";
	private static final String NODE = "miApprove";
	private static final Long ASSIGNEE = 1123598821738675201L;

	/** 测试上下文无登录身份：注入管理员用户，让 WfAuthUtil.isAdmin 豁免记录级鉴权（避免「页面已过期」） */
	private static MockedStatic<SecureUtil> adminStub() {
		MockedStatic<SecureUtil> stub = mockStatic(SecureUtil.class);
		BladeUser u = new BladeUser();
		u.setUserId(1L);
		u.setRoleName("administrator");
		u.setTenantId("000000");
		// 覆盖所有 getUser 重载 + 常用取值，避免真实实现在无 SecurityContext 时抛「页面已过期」
		stub.when(() -> SecureUtil.getUser()).thenReturn(u);
		stub.when(() -> SecureUtil.getUser(anyString())).thenReturn(u);
		stub.when(() -> SecureUtil.getUserId()).thenReturn(1L);
		stub.when(() -> SecureUtil.getTenantId()).thenReturn("000000");
		stub.when(() -> SecureUtil.getUserRole()).thenReturn("administrator");
		return stub;
	}

	@SuppressWarnings("unchecked")
	private Task mockTask(String id, String assignee, String defKey) {
		Task t = mock(Task.class);
		when(t.getId()).thenReturn(id);
		when(t.getAssignee()).thenReturn(String.valueOf(assignee));
		when(t.getTaskDefinitionKey()).thenReturn(defKey);
		return t;
	}

	@Test
	@DisplayName("成功路径：加签生成独立 ACT 任务 + 独立 BIZ_TASK_ID_ + 集合变量并回 + wf_task 零写入")
	void addSignGeneratesIndependentTask() {
		// requireTodoTask → bizResolver.resolve
		WfTask task = new WfTask();
		task.setId(TASK_ID);
		task.setInstId(INST_ID);
		task.setEngineTaskId("RT_MI_1");
		task.setNodeKey(NODE);
		task.setAssignee(676L);
		task.setIsTest(0);
		task.setStatus(WfTask.STATUS_TODO);
		when(bizResolver.resolve(TASK_ID)).thenReturn(task);

		WfInstance inst = new WfInstance();
		inst.setId(INST_ID);
		inst.setEngineInstId(ENGINE_INST);
		inst.setDefId(2108446420618047490L);
		when(instanceMapper.selectById(INST_ID)).thenReturn(inst);

		// 引擎任务快照：加签前=[RT_MI_1]，加签后=[RT_MI_1, RT_MI_NEW]
		Task oldT = mockTask("RT_MI_1", "676", NODE);
		Task newT = mockTask("RT_MI_NEW", String.valueOf(ASSIGNEE), NODE);
		org.flowable.task.api.TaskQuery query = mock(org.flowable.task.api.TaskQuery.class);
		when(flowableTaskService.createTaskQuery()).thenReturn(query);
		when(query.processInstanceId(ENGINE_INST)).thenReturn(query);
		when(query.taskAssignee(anyString())).thenReturn(query);
		when(query.list()).thenReturn(List.of(oldT)).thenReturn(List.of(oldT, newT));

		try (MockedStatic<SecureUtil> ignored = adminStub()) {
			AddSignDTO dto = new AddSignDTO();
			dto.setAssignee(ASSIGNEE);
			dto.setAddSignType(1);
			dto.setOpinion("编排E2E");
			boolean ok = taskService.addSign(TASK_ID, dto);
			assertThat(ok).isTrue();
		}

		// 1) 引擎层：addMultiInstanceExecution 在 MI 节点 + 实例上调用，传加签人
		verify(runtimeService, times(1)).addMultiInstanceExecution(eq(NODE), eq(ENGINE_INST), any(Map.class));

		// 2) 新任务写独立 BIZ_TASK_ID_（不等同原任务 id；writeFull 被调用一次）
		ArgumentCaptor<TaskBizRecord> recCap = ArgumentCaptor.forClass(TaskBizRecord.class);
		verify(directWriter, times(1)).writeFull(recCap.capture());
		TaskBizRecord rec = recCap.getValue();
		assertThat(rec.getBizTaskId()).isNotNull();
		assertThat(rec.getBizTaskId()).isNotEqualTo(TASK_ID); // 独立 bizTaskId
		assertThat(rec.getEngineTaskId()).isEqualTo("RT_MI_NEW");
		assertThat(rec.getAssignee()).isEqualTo(ASSIGNEE);
		assertThat(rec.getStatus()).isEqualTo(WfTask.STATUS_TODO);

		// 3) D17/V6 补偿：集合变量 wfMiAssignees_<nodeKey> 并回加签人
		ArgumentCaptor<Object> varCap = ArgumentCaptor.forClass(Object.class);
		verify(runtimeService, times(1)).setVariable(eq(ENGINE_INST), eq("wfMiAssignees_" + NODE), varCap.capture());
		assertThat(varCap.getValue()).isInstanceOf(List.class);
		assertThat((List<String>) varCap.getValue()).contains(String.valueOf(ASSIGNEE));

		// 4) 运行时零写入 wf_task：addSign 不落台账（无 insert 调用）
		verify(instanceMapper, never()).insert(any(WfInstance.class));
	}

	@Test
	@DisplayName("降级路径：非 MI 节点调 addSign → 友好错误，原任务不被动")
	void addSignRejectsNonMiNode() {
		WfTask task = new WfTask();
		task.setId(TASK_ID);
		task.setInstId(INST_ID);
		task.setEngineTaskId("RT_PLAIN_1");
		task.setNodeKey("plainApproval");
		task.setAssignee(676L);
		task.setIsTest(0);
		task.setStatus(WfTask.STATUS_TODO);
		when(bizResolver.resolve(TASK_ID)).thenReturn(task);

		WfInstance inst = new WfInstance();
		inst.setId(INST_ID);
		inst.setEngineInstId(ENGINE_INST);
		inst.setDefId(2108446420618047490L);
		when(instanceMapper.selectById(INST_ID)).thenReturn(inst);

		// 非 MI 节点：addMultiInstanceExecution 抛 FlowableException（Flowable 8.1 实测）
		when(runtimeService.addMultiInstanceExecution(any(), any(), any()))
			.thenThrow(new FlowableException("No multi instance execution found for activity id plainApproval"));

		Task pt = mockTask("RT_PLAIN_1", "676", "plainApproval");
		org.flowable.task.api.TaskQuery query = mock(org.flowable.task.api.TaskQuery.class);
		when(flowableTaskService.createTaskQuery()).thenReturn(query);
		when(query.processInstanceId(ENGINE_INST)).thenReturn(query);
		when(query.taskAssignee(anyString())).thenReturn(query);
		when(query.list()).thenReturn(List.of(pt));

		try (MockedStatic<SecureUtil> ignored = adminStub()) {
			AddSignDTO dto = new AddSignDTO();
			dto.setAssignee(ASSIGNEE);
			dto.setAddSignType(1);
			dto.setOpinion("降级E2E");
			assertThatThrownBy(() -> taskService.addSign(TASK_ID, dto))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("未启用多人会签");
		}
		// 降级：不写新任务业务列、不写集合变量
		verify(directWriter, never()).writeFull(any());
		verify(runtimeService, never()).setVariable(eq(ENGINE_INST), any(), any());
	}
}
