package org.springblade.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.util.BpmnExtensionUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * P3-5：「定义源=BPMN」节点读取的回归测试（纯函数，无 Spring 容器）。
 *
 * <p>覆盖 {@link WfBpmnExtensionReader#toProcessNode}：全字段映射、BPMN 未写 {@code wf:node}
 * 扩展时的降级、脏数据（非数字）容错。</p>
 */
class WfBpmnNodeSwitchTest {

	private static BpmnExtensionUtil.WfNodeExt ext() {
		BpmnExtensionUtil.WfNodeExt e = new BpmnExtensionUtil.WfNodeExt();
		e.nodeType = "1";
		e.signOrder = "1";
		e.mergeType = "3";
		e.passNum = "2";
		e.allowReject = "1";
		e.allowForward = "0";
		e.autoApprove = "0";
		e.sortOrder = "5";
		e.testStatus = "0";
		e.extJson = "{\"timeout\":[]}";
		return e;
	}

	@Test
	@DisplayName("全字段映射：wf:node 扩展 → WfProcessNode（含节点名）")
	void mapsAllFields() {
		WfProcessNode n = WfBpmnExtensionReader.toProcessNode(101L, "miNode", "会签节点", ext());
		assertEquals(101L, n.getDefId());
		assertEquals("miNode", n.getNodeKey());
		assertEquals("会签节点", n.getNodeName());
		assertEquals(Integer.valueOf(1), n.getNodeType());
		assertEquals(Integer.valueOf(1), n.getSignOrder());
		assertEquals(Integer.valueOf(3), n.getMergeType());
		assertEquals(Integer.valueOf(2), n.getPassNum());
		assertEquals(Integer.valueOf(1), n.getAllowReject());
		assertEquals(Integer.valueOf(0), n.getAllowForward());
		assertEquals(Integer.valueOf(0), n.getAutoApprove());
		assertEquals(Integer.valueOf(5), n.getSortOrder());
		assertEquals(Integer.valueOf(0), n.getTestStatus());
		assertEquals("{\"timeout\":[]}", n.getExtJson());
	}

	@Test
	@DisplayName("BPMN 未写 wf:node 扩展：只回填标识字段，其余为 null（按「未配置」处理）")
	void nullExtKeepsIdentityOnly() {
		WfProcessNode n = WfBpmnExtensionReader.toProcessNode(101L, "plainTask", "普通任务", null);
		assertEquals(101L, n.getDefId());
		assertEquals("plainTask", n.getNodeKey());
		assertEquals("普通任务", n.getNodeName());
		assertNull(n.getNodeType());
		assertNull(n.getSignOrder());
		assertNull(n.getExtJson());
	}

	@Test
	@DisplayName("脏数据容错：非数字维度转为 null，不抛异常")
	void toleratesDirtyValues() {
		BpmnExtensionUtil.WfNodeExt bad = ext();
		bad.nodeType = "x";
		bad.signOrder = "";
		bad.passNum = "abc";
		WfProcessNode n = WfBpmnExtensionReader.toProcessNode(101L, "miNode", "会签节点", bad);
		assertNull(n.getNodeType(), "非数字 nodeType 应容错为 null");
		assertNull(n.getSignOrder(), "空串 signOrder 应容错为 null");
		assertNull(n.getPassNum(), "非数字 passNum 应容错为 null");
		// 未脏的字段不受影响
		assertEquals(Integer.valueOf(3), n.getMergeType());
		assertEquals(Integer.valueOf(5), n.getSortOrder());
	}
}
