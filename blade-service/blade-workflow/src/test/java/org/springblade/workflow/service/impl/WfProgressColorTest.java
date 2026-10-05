package org.springblade.workflow.service.impl;

import org.flowable.engine.history.HistoricActivityInstance;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.vo.WfProgressView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WfInstanceServiceImpl#fillActivityColors} 四色集合回归：覆盖
 * 运行中 / 通过 / 不通过 / 中途取消（无 EndEvent）/ 到达 EndEvent 的取消 五种形态，
 * 断言 unfinished（蓝）/ finished（绿）/ finishedSeq（绿）/ rejected（红）/ cancelled（灰）集合。
 *
 * <p>纯函数、无引擎 / 无鉴权，直接喂构造好的 {@link HistoricActivityInstance} 桩即可断言。</p>
 */
class WfProgressColorTest {

	/** 构造一个历史活动实例桩：activityId / 类型 / 结束时间（null=进行中） */
	private static HistoricActivityInstance act(String id, String type, Date end) {
		HistoricActivityInstance hai = mock(HistoricActivityInstance.class);
		when(hai.getActivityId()).thenReturn(id);
		when(hai.getActivityType()).thenReturn(type);
		when(hai.getEndTime()).thenReturn(end);
		return hai;
	}

	/** 任意早一点的完成时刻 */
	private static final Date T0 = new Date(1_700_000_000_000L);
	/** 取消时刻（Flowable 删除事务补的 endTime 与其同源） */
	private static final Date CANCEL_T = new Date(1_700_000_100_000L);

	// ───────────────────────── 运行中（0） ─────────────────────────

	@Test
	void running_shouldMarkCurrentNodeBlueAndDoneGreen() {
		List<HistoricActivityInstance> acts = Arrays.asList(
			act("startEvent", "startEvent", T0),
			act("apply", "userTask", T0),
			act("leader", "userTask", null),        // 进行中
			act("flow_1", "sequenceFlow", T0)
		);
		WfProgressView view = new WfProgressView();
		WfInstanceServiceImpl.fillActivityColors(view, acts, 0, null, null);

		assertThat(view.getUnfinishedActivityIds()).containsExactly("leader");
		assertThat(view.getFinishedActivityIds()).containsExactlyInAnyOrder("startEvent", "apply");
		assertThat(view.getFinishedSequenceFlowIds()).containsExactly("flow_1");
		assertThat(view.getRejectedActivityIds()).isEmpty();
		assertThat(view.getCancelledActivityIds()).isEmpty();
	}

	// ───────────────────────── 通过（1） ─────────────────────────

	@Test
	void approved_shouldMarkAllGreenIncludingEndEvent() {
		List<HistoricActivityInstance> acts = Arrays.asList(
			act("startEvent", "startEvent", T0),
			act("apply", "userTask", T0),
			act("leader", "userTask", T0),
			act("endEvent", "endEvent", T0),
			act("flow_1", "sequenceFlow", T0)
		);
		WfProgressView view = new WfProgressView();
		WfInstanceServiceImpl.fillActivityColors(view, acts, 1, null, null);

		assertThat(view.getUnfinishedActivityIds()).isEmpty();
		assertThat(view.getFinishedActivityIds()).containsExactlyInAnyOrder(
			"startEvent", "apply", "leader", "endEvent");
		assertThat(view.getFinishedSequenceFlowIds()).containsExactly("flow_1");
		assertThat(view.getRejectedActivityIds()).isEmpty();
		assertThat(view.getCancelledActivityIds()).isEmpty();
	}

	// ───────────────────────── 不通过（2） ─────────────────────────

	@Test
	void rejected_shouldMarkRejectNodeRedAndRemoveFromGreen() {
		List<HistoricActivityInstance> acts = Arrays.asList(
			act("startEvent", "startEvent", T0),
			act("apply", "userTask", T0),
			act("rejectNode", "userTask", T0)        // 退回发生在此节点
		);
		WfProgressView view = new WfProgressView();
		WfInstanceServiceImpl.fillActivityColors(view, acts, 2, "rejectNode", null);

		assertThat(view.getRejectedActivityIds()).containsExactly("rejectNode");
		assertThat(view.getFinishedActivityIds()).containsExactlyInAnyOrder("startEvent", "apply");
		assertThat(view.getFinishedActivityIds()).doesNotContain("rejectNode");
		assertThat(view.getUnfinishedActivityIds()).isEmpty();
		assertThat(view.getCancelledActivityIds()).isEmpty();
	}

	// ───────────────────────── 中途取消（3，无 EndEvent） ─────────────────────────

	@Test
	void midFlightCancel_shouldGrayInFlightActivities() {
		List<HistoricActivityInstance> acts = Arrays.asList(
			act("startEvent", "startEvent", T0),
			act("apply", "userTask", T0),                 // 正常完成（保持绿）
			act("leader", "userTask", null),              // 删除时仍在运行 → 置灰
			act("branch", "userTask", CANCEL_T),          // 删除事务补的 endTime ≈ 取消时刻 → 置灰
			act("flow_1", "sequenceFlow", T0)
		);
		WfProgressView view = new WfProgressView();
		WfInstanceServiceImpl.fillActivityColors(view, acts, 3, null, CANCEL_T);

		assertThat(view.getCancelledActivityIds()).containsExactlyInAnyOrder("leader", "branch");
		assertThat(view.getFinishedActivityIds()).containsExactlyInAnyOrder("startEvent", "apply");
		assertThat(view.getFinishedActivityIds()).doesNotContain("leader", "branch");
		assertThat(view.getUnfinishedActivityIds()).doesNotContain("leader");
		assertThat(view.getFinishedSequenceFlowIds()).containsExactly("flow_1");
		assertThat(view.getRejectedActivityIds()).isEmpty();
	}

	// ───────────────────────── 到达 EndEvent 的取消（3，有 EndEvent） ─────────────────────────

	@Test
	void cancelWithEndEvent_shouldNotGrayAndLeaveEndEventGreen() {
		List<HistoricActivityInstance> acts = Arrays.asList(
			act("startEvent", "startEvent", T0),
			act("apply", "userTask", T0),
			act("leader", "userTask", T0),
			act("endEvent", "endEvent", T0)
		);
		WfProgressView view = new WfProgressView();
		WfInstanceServiceImpl.fillActivityColors(view, acts, 3, null, CANCEL_T);

		// 流经 EndEvent 的取消：后端不置灰，交给前端按 instanceStatus 纠偏 EndEvent
		assertThat(view.getCancelledActivityIds()).isEmpty();
		assertThat(view.getFinishedActivityIds()).containsExactlyInAnyOrder(
			"startEvent", "apply", "leader", "endEvent");
		assertThat(view.getUnfinishedActivityIds()).isEmpty();
		assertThat(view.getRejectedActivityIds()).isEmpty();
	}
}
