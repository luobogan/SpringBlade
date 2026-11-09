package org.springblade.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfExtTask;
import org.springblade.workflow.entity.WfTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 扩展任务 type/status 语义与 VO 兼容映射测试（纯 POJO，无 DB / 无 Spring）。
 *
 * <p>核心契约：{@link WfExtTask#toWfTaskStatus()} 必须映射回原 {@link WfTask} 的 6 态语义，
 * 保证前端标签零改动；{@code isTodoType/isDoneType} 必须与 §10.7 决策 2 的分段判据一致，
 * 且 <b>排除 type=7 装饰行</b>（否则列表会多算行）。</p>
 */
class WfExtTaskTest {

	private static WfExtTask task(int type, int status) {
		WfExtTask t = new WfExtTask();
		t.setType(type);
		t.setStatus(status);
		return t;
	}

	@Test
	@DisplayName("协办/抄送/传阅/已读 → 原 status 7/8/11（已办段，前端标签零改动）")
	void mapsDoneSideStatuses() {
		assertEquals(WfTask.STATUS_COADJUTANT,
			task(WfExtTask.TYPE_COADJUTANT, WfExtTask.STATUS_PENDING).toWfTaskStatus());
		assertEquals(WfTask.STATUS_COADJUTANT,
			task(WfExtTask.TYPE_COADJUTANT, WfExtTask.STATUS_DONE).toWfTaskStatus(),
			"协办无论是否已读都在已办段（与原 status=7 一致）");
		assertEquals(WfTask.STATUS_CIRCULATE,
			task(WfExtTask.TYPE_CIRCULATE, WfExtTask.STATUS_PENDING).toWfTaskStatus());
		assertEquals(WfTask.STATUS_READ,
			task(WfExtTask.TYPE_READ, WfExtTask.STATUS_PENDING).toWfTaskStatus());
		assertEquals(WfTask.STATUS_READ,
			task(WfExtTask.TYPE_READED, WfExtTask.STATUS_DONE).toWfTaskStatus());
	}

	@Test
	@DisplayName("加签协办/退回重提交 → 待处理 0、已处理 2、已拒绝 4")
	void mapsTodoSideStatuses() {
		assertEquals(WfTask.STATUS_TODO,
			task(WfExtTask.TYPE_RESUBMIT, WfExtTask.STATUS_PENDING).toWfTaskStatus());
		assertEquals(WfTask.STATUS_DONE,
			task(WfExtTask.TYPE_RESUBMIT, WfExtTask.STATUS_DONE).toWfTaskStatus());
		assertEquals(WfTask.STATUS_FINISHED,
			task(WfExtTask.TYPE_ADD_SIGN, WfExtTask.STATUS_REJECT).toWfTaskStatus());
	}

	@Test
	@DisplayName("待办段判据：仅 type=2/6；协办(1) 不得误入待办段")
	void todoTypePredicate() {
		assertTrue(WfExtTask.isTodoType(WfExtTask.TYPE_ADD_SIGN));
		assertTrue(WfExtTask.isTodoType(WfExtTask.TYPE_RESUBMIT));
		assertFalse(WfExtTask.isTodoType(WfExtTask.TYPE_COADJUTANT),
			"协办是 status=7，属已办段，误入待办段会漂移");
		assertFalse(WfExtTask.isTodoType(WfExtTask.TYPE_CIRCULATE));
		assertFalse(WfExtTask.isTodoType(WfExtTask.TYPE_FREE_FLOW), "装饰行不进列表");
	}

	@Test
	@DisplayName("已办段判据：type=1/3/4/5；装饰行排除")
	void doneTypePredicate() {
		assertTrue(WfExtTask.isDoneType(WfExtTask.TYPE_COADJUTANT));
		assertTrue(WfExtTask.isDoneType(WfExtTask.TYPE_CIRCULATE));
		assertTrue(WfExtTask.isDoneType(WfExtTask.TYPE_READ));
		assertTrue(WfExtTask.isDoneType(WfExtTask.TYPE_READED));
		assertFalse(WfExtTask.isDoneType(WfExtTask.TYPE_FREE_FLOW), "装饰行不进列表");
	}

	@Test
	@DisplayName("null type/status 时不抛异常，退化为待办")
	void nullSafe() {
		WfExtTask t = new WfExtTask();
		assertEquals(WfTask.STATUS_TODO, t.toWfTaskStatus());
	}
}
