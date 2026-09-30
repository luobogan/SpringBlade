package org.springblade.workflow;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-5：「定义源=BPMN」节点/出口列表读取的回归测试（纯函数，无 Spring 容器）。
 *
 * <p>覆盖 {@link WfBpmnExtensionReader#toNodes}（/definition/{id}/nodes 的 BPMN 读源转换）：
 * 开始节点（nodeType=0）覆盖、sortOrder 排序（XML 顺序无关）、无扩展 UserTask 的降级映射；
 * 以及 {@link WfBpmnExtensionReader#toNodeLinks}（/definition/{id}/links）的
 * 真实 sequenceFlow + 流程级 wf:foldedLink 合并。</p>
 */
class WfBpmnDefNodesTest {

	/** 三个 UserTask 刻意乱序排列：证明结果按 sortOrder 排序而非 XML 顺序 */
	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:flowable="http://flowable.org/bpmn"
		             xmlns:wf="http://www.springblade.org/workflow"
		             targetNamespace="http://www.springblade.org/workflow">
		  <process id="testProcess" name="Test" isExecutable="true">
		    <extensionElements>
		      <wf:foldedLink from="taskA" to="taskC" viaGatewayKey="gw1" isReject="0"
		                     isMustPass="0" conditionCn="金额大于1万" sortOrder="9"/>
		    </extensionElements>
		    <startEvent id="start1" name="开始"/>
		    <exclusiveGateway id="gw1" name="网关"/>
		    <endEvent id="end1" name="结束"/>
		    <userTask id="taskB" name="部门经理审批">
		      <extensionElements>
		        <wf:node nodeType="1" signOrder="1" mergeType="0" allowReject="1"
		                 allowForward="0" autoApprove="0" sortOrder="2" testStatus="0"/>
		      </extensionElements>
		    </userTask>
		    <userTask id="taskC" name="无扩展任务"/>
		    <userTask id="taskA" name="申请人">
		      <extensionElements>
		        <wf:node nodeType="0" sortOrder="1" autoApprove="1"/>
		      </extensionElements>
		    </userTask>
		    <sequenceFlow id="f1" sourceRef="taskA" targetRef="taskB">
		      <extensionElements>
		        <wf:link isReject="1" isMustPass="0" conditionCn="同意" sortOrder="1"
		                 viaGateway="0" viaGatewayKey=""/>
		      </extensionElements>
		    </sequenceFlow>
		    <sequenceFlow id="f2" sourceRef="taskB" targetRef="taskC"/>
		  </process>
		</definitions>
		""";

	private static BpmnModel parse(String xml) {
		return new BpmnXMLConverter().convertToBpmnModel(
			() -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
	}

	@Test
	@DisplayName("toNodes：覆盖开始节点(nodeType=0)，按 sortOrder 升序，无扩展任务仅回填标识字段")
	void nodesSortedAndCoverStart() {
		List<WfProcessNode> nodes = WfBpmnExtensionReader.toNodes(101L, parse(BPMN));
		assertEquals(6, nodes.size(), "3 个 UserTask + 合成的开始/结束/网关");
		assertEquals("taskA", nodes.get(0).getNodeKey());
		assertEquals("taskB", nodes.get(1).getNodeKey());
		assertEquals("taskC", nodes.get(2).getNodeKey(), "无 sortOrder 的排末尾");
		// 合成节点（无 wf:node 扩展 → 无 sortOrder）：StartEvent→0、EndEvent→3、Gateway→7
		WfProcessNode startEv = nodes.get(3);
		assertEquals("start1", startEv.getNodeKey());
		assertEquals(Integer.valueOf(0), startEv.getNodeType(), "发起页按 nodeType=0 定位开始节点");
		assertEquals("开始", startEv.getNodeName());
		assertNull(startEv.getSortOrder());
		assertEquals(Integer.valueOf(3), nodes.get(4).getNodeType(), "EndEvent→3(归档)");
		assertEquals("gw1", nodes.get(5).getNodeKey());
		assertEquals(Integer.valueOf(7), nodes.get(5).getNodeType(), "Gateway→7");

		// 开始 UserTask（nodeType=0）：发起页据此定位
		WfProcessNode start = nodes.get(0);
		assertEquals(Integer.valueOf(0), start.getNodeType());
		assertEquals("申请人", start.getNodeName());
		assertEquals(Integer.valueOf(1), start.getSortOrder());
		assertEquals(Integer.valueOf(1), start.getAutoApprove());
		assertEquals(101L, start.getDefId());

		// 审批节点全字段
		WfProcessNode approve = nodes.get(1);
		assertEquals(Integer.valueOf(1), approve.getNodeType());
		assertEquals(Integer.valueOf(1), approve.getSignOrder());
		assertEquals(Integer.valueOf(1), approve.getAllowReject());

		// 无 wf:node 扩展：只回填 defId/nodeKey/nodeName
		WfProcessNode plain = nodes.get(2);
		assertEquals("无扩展任务", plain.getNodeName());
		assertNull(plain.getNodeType());
		assertNull(plain.getSortOrder());
	}

	@Test
	@DisplayName("toNodes：null 模型 / 无主流程返回空列表（调用方按空回退 wf_process_node）")
	void nullModelReturnsEmpty() {
		assertTrue(WfBpmnExtensionReader.toNodes(101L, null).isEmpty());
		assertTrue(WfBpmnExtensionReader.toNodes(101L, new BpmnModel()).isEmpty());
	}

	@Test
	@DisplayName("toNodeLinks：真实 sequenceFlow 与流程级 foldedLink 合并，按 sortOrder 排序")
	void linksMergeRealAndFolded() {
		List<WfNodeLink> links = WfBpmnExtensionReader.toNodeLinks(101L, parse(BPMN));
		assertEquals(3, links.size(), "2 条真实连线 + 1 条折叠连线");
		// 排序：sortOrder 1 < 9 < null(MAX)，与出现顺序无关
		assertEquals("taskA->taskB", links.get(0).getFromNodeKey() + "->" + links.get(0).getToNodeKey());
		assertEquals(Integer.valueOf(1), links.get(0).getIsReject());
		// 折叠连线：无对应 sequenceFlow，仍按 sortOrder=9 排在中间，带网关标记
		WfNodeLink folded = links.get(1);
		assertEquals("taskA->taskC", folded.getFromNodeKey() + "->" + folded.getToNodeKey());
		assertEquals("gw1", folded.getViaGatewayKey());
		assertEquals(Integer.valueOf(1), folded.getViaGateway());
		assertEquals("金额大于1万", folded.getConditionCn());
		// f2 无扩展：仍返回结构（from/to），维度为空，排末尾
		WfNodeLink plain = links.get(2);
		assertEquals("taskB->taskC", plain.getFromNodeKey() + "->" + plain.getToNodeKey());
		assertNull(plain.getConditionCn());
		assertNull(plain.getViaGatewayKey());
	}
}
