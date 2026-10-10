package org.springblade.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.entity.WfTask;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.mapper.WfNodeTimeoutMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springblade.workflow.service.impl.WfTimeoutServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 超时动作守卫回归（对应文档 §12.6 必测「超时（提醒 与 自动通过 两类）」+ 风险 R4）。
 *
 * <p><b>R4（现存缺陷已修）</b>：旧实现中动作执行 {@code try/catch} 仅告警，随后<b>无论成败</b>都置
 * {@code timeout_handled=1} ⇒ 超时动作失败后永不重试、静默丢失。现在必须保证
 * 「<b>仅成功才置位</b>」，失败保留 0 由下一扫描周期重试。两类动作（{@code autoApprove} / {@code remind}）
 * 都必须满足，故本类逐一固化。</p>
 *
 * <p>另固化测试态纵深防御：{@code is_test=1} 的实例不得执行任何超时动作（方案 §6.4 C5），
 * 防止测试流程的「自动通过 / 转办 / 催办」外溢到生产。</p>
 */
class WfTimeoutActionGuardTest {

	private WfNodeTimeoutMapper timeoutMapper;
	private WfProcessNodeMapper nodeMapper;
	private WfTaskMapper taskMapper;
	private WfProcessDefinitionMapper definitionMapper;
	private IWfTaskService taskService;
	private INoticeClient noticeClient;
	private WfTaskActWriter taskActWriter;
	private IWfInstanceService instanceService;
	private IFormmodeClient formmodeClient;
	private WfBpmnExtensionReader bpmnReader;

	private WfTimeoutServiceImpl service;

	@BeforeEach
	void setUp() {
		timeoutMapper = mock(WfNodeTimeoutMapper.class);
		nodeMapper = mock(WfProcessNodeMapper.class);
		taskMapper = mock(WfTaskMapper.class);
		definitionMapper = mock(WfProcessDefinitionMapper.class);
		taskService = mock(IWfTaskService.class);
		noticeClient = mock(INoticeClient.class);
		taskActWriter = mock(WfTaskActWriter.class);
		instanceService = mock(IWfInstanceService.class);
		formmodeClient = mock(IFormmodeClient.class);
		bpmnReader = mock(WfBpmnExtensionReader.class);
		service = new WfTimeoutServiceImpl(timeoutMapper, nodeMapper, taskMapper, definitionMapper,
			taskService, noticeClient, taskActWriter, instanceService, formmodeClient, bpmnReader);
	}

	private WfTask task() {
		WfTask t = new WfTask();
		t.setId(2104055356658364418L);
		t.setAssignee(1123598821738675201L);
		t.setNodeKey("approval");
		t.setTimeoutHandled(0);
		return t;
	}

	private WfInstance inst(int isTest) {
		WfInstance i = new WfInstance();
		i.setId(2104055356658364419L);
		i.setIsTest(isTest);
		return i;
	}

	private WfNodeTimeout rule(String actionWay) {
		WfNodeTimeout r = new WfNodeTimeout();
		r.setActionWay(actionWay);
		return r;
	}

	@Test
	@DisplayName("自动通过成功：置 timeout_handled=1 并同步 ACT_*（防重复触发）")
	void autoApproveSuccess_shouldMarkHandled() {
		WfTask task = task();

		service.fire(rule("autoApprove"), task, inst(0));

		assertThat(task.getTimeoutHandled()).isEqualTo(1);
		verify(taskMapper).updateById(task);
		verify(taskActWriter).sync(task);
	}

	@Test
	@DisplayName("R4：自动通过失败 → 不置位、不写 ACT_*（保留重试，不静默丢失）")
	void autoApproveFailure_shouldKeepUnhandled() {
		WfTask task = task();
		doThrow(new IllegalStateException("引擎审批失败")).when(taskService)
			.autoApprove(anyLong(), anyString());

		service.fire(rule("autoApprove"), task, inst(0));

		assertThat(task.getTimeoutHandled()).as("失败必须保留 0，交由下一周期重试").isEqualTo(0);
		verify(taskMapper, never()).updateById(any(WfTask.class));
		verify(taskActWriter, never()).sync(any(WfTask.class));
	}

	@Test
	@DisplayName("提醒类成功（无提醒配置视为成功）：同样置位防重复")
	void remindSuccess_shouldMarkHandled() {
		WfTask task = task();

		service.fire(rule("remind"), task, inst(0));

		assertThat(task.getTimeoutHandled()).isEqualTo(1);
		verify(taskMapper).updateById(task);
	}

	@Test
	@DisplayName("R4：提醒留痕失败 → 同样不置位（两类动作口径一致）")
	void remindFailure_shouldKeepUnhandled() {
		WfTask task = task();
		WfNodeTimeout r = rule("remind");
		r.setRemindTypes("sys");
		r.setRemindBeforeOperator(1);
		doThrow(new IllegalStateException("留痕失败")).when(instanceService)
			.recordLog(anyLong(), anyString(), anyLong(), eq(WfApprovalLog.LOG_COMMENT), anyString());

		service.fire(r, task, inst(0));

		assertThat(task.getTimeoutHandled()).isEqualTo(0);
		verify(taskMapper, never()).updateById(any(WfTask.class));
	}

	@Test
	@DisplayName("测试态纵深防御：is_test=1 不执行任何超时动作、不置位")
	void testInstance_shouldSkipAllTimeoutActions() {
		WfTask task = task();

		service.fire(rule("autoApprove"), task, inst(1));

		assertThat(task.getTimeoutHandled()).isEqualTo(0);
		verify(taskService, never()).autoApprove(anyLong(), anyString());
		verify(taskMapper, never()).updateById(any(WfTask.class));
		verify(taskActWriter, never()).sync(any(WfTask.class));
	}
}
