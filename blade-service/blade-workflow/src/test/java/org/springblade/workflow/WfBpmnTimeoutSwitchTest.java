package org.springblade.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfNodeTimeout;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.util.BpmnExtensionUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-5：timeout 开关「读 BPMN」分支的回归测试（纯函数，无 Spring 容器）。
 *
 * <p>覆盖 {@link WfBpmnExtensionReader#toNodeTimeouts} —— 即 {@code blade.workflow.timeout-from-bpmn.enabled}
 * 开启后 {@code WfTimeoutServiceImpl.listEnabled} 实际走的分支逻辑。验证：启用过滤、seq 升序、
 * 全字段映射、空/脏数据容错。默认关（开关未开启时走原 {@code wf_node_timeout} 表读，行为不变）。</p>
 */
class WfBpmnTimeoutSwitchTest {

	private static BpmnExtensionUtil.WfTimeoutExt ext(String seq, String enabled, String actionWay) {
		BpmnExtensionUtil.WfTimeoutExt t = new BpmnExtensionUtil.WfTimeoutExt();
		t.seq = seq;
		t.enabled = enabled;
		t.actionWay = actionWay;
		return t;
	}

	@Test
	@DisplayName("仅保留 enabled=1/true 的规则，排除 0/false/空")
	void filtersDisabledRules() {
		List<WfNodeTimeout> r = WfBpmnExtensionReader.toNodeTimeouts(1L, "n1", List.of(
			ext("0", "1", "autoApprove"),
			ext("1", "0", "remind"),
			ext("2", "true", "assign"),
			ext("3", "false", "forward")
		));
		assertEquals(2, r.size(), "应仅保留两条已启用规则");
		assertEquals("autoApprove", r.get(0).getActionWay());
		assertEquals("assign", r.get(1).getActionWay());
		// 已保留的规则统一标记 enabled=1（与原表过滤等价）
		assertEquals(Integer.valueOf(1), r.get(0).getEnabled());
		assertEquals(Integer.valueOf(1), r.get(1).getEnabled());
	}

	@Test
	@DisplayName("按 seq 升序排序（乱序输入 → 稳定升序）")
	void sortsBySeq() {
		List<WfNodeTimeout> r = WfBpmnExtensionReader.toNodeTimeouts(1L, "n1", List.of(
			ext("2", "1", "b"),
			ext("0", "1", "a"),
			ext("1", "1", "c")
		));
		assertEquals(List.of("a", "c", "b"), r.stream().map(WfNodeTimeout::getActionWay).toList());
		assertEquals(List.of(0, 1, 2), r.stream().map(WfNodeTimeout::getSeq).toList());
	}

	@Test
	@DisplayName("全字段映射正确（字段级时间/提醒配置）")
	void mapsAllFields() {
		BpmnExtensionUtil.WfTimeoutExt t = new BpmnExtensionUtil.WfTimeoutExt();
		t.seq = "0";
		t.enabled = "1";
		t.startType = "2";
		t.startField = "applyTime";
		t.endType = "2";
		t.endFixedTime = "23:59";
		t.durationMin = "120";
		t.actionWay = "assign";
		t.opinion = "超时转办";
		t.operatorIds = "100,200";
		t.remindBeforeOperator = "1";
		t.remindTypes = "sys,ml";
		t.remindPersons = "300";

		List<WfNodeTimeout> r = WfBpmnExtensionReader.toNodeTimeouts(7L, "approve1", List.of(t));
		assertEquals(1, r.size());
		WfNodeTimeout w = r.get(0);
		assertEquals(7L, w.getDefId());
		assertEquals("approve1", w.getNodeKey());
		assertEquals(Integer.valueOf(2), w.getStartType());
		assertEquals("applyTime", w.getStartField());
		assertEquals(Integer.valueOf(2), w.getEndType());
		assertEquals("23:59", w.getEndFixedTime());
		assertEquals(Integer.valueOf(120), w.getDurationMin());
		assertEquals("assign", w.getActionWay());
		assertEquals("超时转办", w.getOpinion());
		assertEquals("100,200", w.getOperatorIds());
		assertEquals(Integer.valueOf(1), w.getRemindBeforeOperator());
		assertEquals("sys,ml", w.getRemindTypes());
		assertEquals("300", w.getRemindPersons());
		assertEquals(Integer.valueOf(1), w.getEnabled());
		assertNull(w.getTargetNodeKey(), "targetNodeKey 未进 BPMN，应保持 null");
	}

	@Test
	@DisplayName("空/脏数据容错：null 入参、空列表、非数字 seq")
	void toleratesNullAndDirty() {
		assertTrue(WfBpmnExtensionReader.toNodeTimeouts(1L, "n", null).isEmpty(), "null 入参返回空");
		assertTrue(WfBpmnExtensionReader.toNodeTimeouts(1L, "n", List.of()).isEmpty(), "空列表返回空");

		BpmnExtensionUtil.WfTimeoutExt bad = new BpmnExtensionUtil.WfTimeoutExt();
		bad.seq = "abc";
		bad.enabled = "1";
		List<WfNodeTimeout> r = WfBpmnExtensionReader.toNodeTimeouts(1L, "n", List.of(bad));
		assertEquals(1, r.size());
		assertNull(r.get(0).getSeq(), "非数字 seq 应容错为 null 而非抛异常");
	}
}
