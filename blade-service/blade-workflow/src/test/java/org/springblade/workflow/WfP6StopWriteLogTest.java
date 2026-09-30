package org.springblade.workflow;

import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.helper.WfInstanceActWriter;
import org.springblade.workflow.service.helper.WfTaskActWriter;
import org.springblade.workflow.service.helper.WfWriteHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P6「停止写入 {@code wf_approval_log}」门控单测（《去 wf_ 表改造分析》P6 项）。
 *
 * <p>核心诉求：读源切到 {@code ACT_HI_COMMENT} 后，{@code wf_approval_log} 从「权威台账」降级为
 * 「仅缺口填充」——正常审批意见（ACT 已落账）不再冗余写 {@code wf_approval_log}；只有
 * {@code AddCommentCmd} 硬校验场景（归档/挂起期间运行期 execution 不存在或非挂起态）导致 ACT 写不进时，
 * 才保留 {@code wf} 一行作 {@code WfApprovalLogActReader} 兜底合并的输入，避免读源=act 丢单。</p>
 *
 * <p>本测试用 Mockito 隔离 {@code WfWriteHelper} 的全部协作者，精确断言四种组合下
 * {@code wf_approval_log} 的 insert 是否被调用，覆盖 P6 门控真值表。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfP6StopWriteLogTest {

	@Mock
	private WfInstanceMapper instanceMapper;
	@Mock
	private WfTaskMapper taskMapper;
	@Mock
	private WfApprovalLogMapper logMapper;
	@Mock
	private WfTaskActWriter taskActWriter;
	@Mock
	private WfInstanceActWriter actWriter;
	@Mock
	private IProcessService processService;
	@Mock
	private TaskService taskService;

	@InjectMocks
	private WfWriteHelper writeHelper;

	@BeforeEach
	void setUp() {
		// 默认：双写开、停写开关开（legacy 零行为变化）
		ReflectionTestUtils.setField(writeHelper, "approvalCommentEnabled", true);
		ReflectionTestUtils.setField(writeHelper, "approvalLogWriteEnabled", true);
	}

	/** 正常审批意见（ACT 已落账）的数据，供 actOk=true 场景使用 */
	private void givenActSyncOk() {
		WfInstance inst = new WfInstance();
		inst.setEngineInstId("eng-123");
		when(instanceMapper.selectById(anyLong())).thenReturn(inst);
		WfTask task = new WfTask();
		task.setEngineTaskId("task-456");
		when(taskMapper.selectById(anyLong())).thenReturn(task);
		// addComment 成功（无异常）
	}

	@Test
	@DisplayName("P6-legacy: 停写开关开(true) 时，无论 ACT 是否成功都双写 wf_approval_log")
	void legacy_alwaysWritesWfLog() {
		// 即使 ACT 同步失败（inst 为空 → actOk=false），legacy 仍写 wf
		when(instanceMapper.selectById(anyLong())).thenReturn(null);

		writeHelper.appendLog(1L, 10L, "miApprove", 1001L, WfApprovalLog.LOG_SUPERVISE, "意见A");

		verify(logMapper, times(1)).insert(any(WfApprovalLog.class));
	}

	@Test
	@DisplayName("P6-主路径: 停写开关关(false) 且 ACT 同步成功 → 不再写 wf_approval_log（去冗余写）")
	void p6_actOk_skipsWfLog() {
		ReflectionTestUtils.setField(writeHelper, "approvalLogWriteEnabled", false);
		givenActSyncOk();

		writeHelper.appendLog(1L, 10L, "miApprove", 1001L, WfApprovalLog.LOG_SUPERVISE, "意见B");

		verify(logMapper, never()).insert(any(WfApprovalLog.class));
	}

	@Test
	@DisplayName("P6-缺口填充: 停写关(false) 但 ACT 写不进(归档/挂起 AddCommentCmd 失败) → 仍写 wf 兜底")
	void p6_actFailed_keepsWfGapFiller() {
		ReflectionTestUtils.setField(writeHelper, "approvalLogWriteEnabled", false);
		// ACT 同步失败：实例存在、任务存在，但 addComment 抛异常（模拟 AddCommentCmd 硬校验）
		givenActSyncOk();
		doThrow(new RuntimeException("AddCommentCmd 硬校验：运行期 execution 不存在"))
			.when(processService).addComment(any(), anyString(), anyString(), anyString());

		writeHelper.appendLog(1L, 10L, "miApprove", 1001L, WfApprovalLog.LOG_SUPERVISE, "归档意见");

		verify(logMapper, times(1)).insert(any(WfApprovalLog.class));
	}

	@Test
	@DisplayName("P6-双关: 停写关(false) 且审批意见双写也关(false) → actOk=false，wf 兜底保留")
	void p6_bothOff_keepsWfFallback() {
		ReflectionTestUtils.setField(writeHelper, "approvalCommentEnabled", false);
		ReflectionTestUtils.setField(writeHelper, "approvalLogWriteEnabled", false);

		writeHelper.appendLog(1L, 10L, "miApprove", 1001L, WfApprovalLog.LOG_SUPERVISE, "意见C");

		// 审批意见双写关 → 根本不调 ACT；actOk=false → 兜底写 wf
		verify(processService, never()).addComment(any(), anyString(), anyString(), anyString());
		verify(logMapper, times(1)).insert(any(WfApprovalLog.class));
	}

	@Test
	@DisplayName("P6-大量写入: 停写关 + ACT 成功，循环 2000 次混合节点/类型审批写入，wf_approval_log 增长为 0 且无异常")
	void p6_volumeWrite_zeroWfGrowth_noException() {
		ReflectionTestUtils.setField(writeHelper, "approvalLogWriteEnabled", false);
		givenActSyncOk();

		// 贴近真实新流程：多实例(1)、多节点(miApprove/start)、多动作(批准/提交/督办)、多变操作人
		String[] nodes = {"miApprove", "start", "miApprove", "nodeB"};
		String[] types = {WfApprovalLog.LOG_APPROVE, WfApprovalLog.LOG_SUBMIT,
			WfApprovalLog.LOG_SUPERVISE, WfApprovalLog.LOG_APPROVE};
		long[] operators = {1001L, 1002L, 1003L, 2001L, 2002L};

		for (int i = 0; i < 2000; i++) {
			writeHelper.appendLog(
				1L + (i % 50),                       // 50 个不同实例
				(long) (10 + i),                      // 各自任务
				nodes[i % nodes.length],
				operators[i % operators.length],
				types[i % types.length],
				"批量意见-" + i);
		}

		verify(logMapper, never()).insert(any(WfApprovalLog.class));
		assertThat(true).isTrue(); // 循环未抛异常即达预期（异常会直接失败本用例）
	}
}
