package org.springblade.workflow;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.reject.WfRejectManager;
import org.springblade.workflow.resolver.WfBpmnExtensionReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-5：「定义源=BPMN」退回可达性（{@link WfRejectManager}）的回归测试（纯函数，无 Spring 容器）。
 *
 * <p>核心断言：同一拓扑下，BPMN 读源数据（{@code wf:link} + {@code wf:foldedLink} + {@code wf:node}，
 * 经 {@link WfBpmnExtensionReader#toNodeLinks}/{@link WfBpmnExtensionReader#toNodes} 转换）与
 * {@code wf_node_link}/{@code wf_process_node} 表数据，计算出的可退回候选<b>完全一致</b>。</p>
 *
 * <p>拓扑（taskC 为当前节点）：
 * {@code taskA → taskB → gw1 → taskC}；{@code taskD → taskC}（is_reject=1 封锁）；
 * {@code taskE(归档) → taskC}；折叠连线 {@code taskA →[gw1]→ taskC}。</p>
 */
class WfRejectManagerBpmnTest {

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:flowable="http://flowable.org/bpmn"
		             xmlns:wf="http://www.springblade.org/workflow"
		             targetNamespace="http://www.springblade.org/workflow">
		  <process id="testProcess" name="Test" isExecutable="true">
		    <extensionElements>
		      <wf:foldedLink from="taskA" to="taskC" viaGatewayKey="gw1" sortOrder="9"/>
		    </extensionElements>
		    <startEvent id="start1"/>
		    <userTask id="taskA" name="申请人">
		      <extensionElements><wf:node nodeType="0" sortOrder="1"/></extensionElements>
		    </userTask>
		    <userTask id="taskB" name="部门审批">
		      <extensionElements><wf:node nodeType="1" sortOrder="2"/></extensionElements>
		    </userTask>
		    <userTask id="taskC" name="分管审批">
		      <extensionElements><wf:node nodeType="1" sortOrder="3"/></extensionElements>
		    </userTask>
		    <userTask id="taskD" name="分支审批">
		      <extensionElements><wf:node nodeType="1" sortOrder="4"/></extensionElements>
		    </userTask>
		    <userTask id="taskE" name="归档">
		      <extensionElements><wf:node nodeType="3" sortOrder="5"/></extensionElements>
		    </userTask>
		    <exclusiveGateway id="gw1"/>
		    <sequenceFlow id="f1" sourceRef="taskA" targetRef="taskB">
		      <extensionElements><wf:link sortOrder="1"/></extensionElements>
		    </sequenceFlow>
		    <sequenceFlow id="f2" sourceRef="taskB" targetRef="gw1">
		      <extensionElements><wf:link sortOrder="2"/></extensionElements>
		    </sequenceFlow>
		    <sequenceFlow id="f3" sourceRef="gw1" targetRef="taskC">
		      <extensionElements><wf:link sortOrder="3"/></extensionElements>
		    </sequenceFlow>
		    <sequenceFlow id="f4" sourceRef="taskD" targetRef="taskC">
		      <extensionElements><wf:link sortOrder="4" isReject="1"/></extensionElements>
		    </sequenceFlow>
		    <sequenceFlow id="f5" sourceRef="taskE" targetRef="taskC">
		      <extensionElements><wf:link sortOrder="5"/></extensionElements>
		    </sequenceFlow>
		  </process>
		</definitions>
		""";

	private static BpmnModel parse(String xml) {
		return new BpmnXMLConverter().convertToBpmnModel(
			() -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
	}

	/** 与 BPMN 拓扑等价的 wf_node_link / wf_process_node 表数据（DB 口径参照） */
	private static DbGraph dbGraph() {
		List<WfProcessNode> nodes = List.of(
			node("taskA", 0), node("taskB", 1), node("taskC", 1), node("taskD", 1), node("taskE", 3));
		List<WfNodeLink> links = List.of(
			link("taskA", "taskB", null, 1),
			link("taskB", "gw1", null, 2),
			link("gw1", "taskC", null, 3),
			link("taskD", "taskC", 1, 4),
			link("taskE", "taskC", null, 5),
			// 折叠连线在 DB 中也是一行（via_gateway=1）
			link("taskA", "taskC", null, 9));
		return new DbGraph(nodes, links);
	}

	private record DbGraph(List<WfProcessNode> nodes, List<WfNodeLink> links) {
	}

	private static WfProcessNode node(String key, int type) {
		WfProcessNode n = new WfProcessNode();
		n.setNodeKey(key);
		n.setNodeType(type);
		return n;
	}

	private static WfNodeLink link(String from, String to, Integer isReject, int sortOrder) {
		WfNodeLink l = new WfNodeLink();
		l.setFromNodeKey(from);
		l.setToNodeKey(to);
		l.setIsReject(isReject);
		l.setSortOrder(sortOrder);
		return l;
	}

	private static List<String> keys(List<WfProcessNode> nodes) {
		return nodes.stream().map(WfProcessNode::getNodeKey).toList();
	}

	@Test
	@DisplayName("BPMN 读源与 wf_* 表读同口径：封锁分支剔除、归档剔除、网关穿透、折叠连线参与回溯")
	void bpmnSourceMatchesDbSource() {
		DbGraph db = dbGraph();
		List<WfProcessNode> fromDb = WfRejectManager.computeRejectableNodes(
			101L, "taskC", null, db.links(), db.nodes());

		BpmnModel model = parse(BPMN);
		List<WfProcessNode> fromBpmn = WfRejectManager.computeRejectableNodes(
			101L, "taskC", null,
			WfBpmnExtensionReader.toNodeLinks(101L, model),
			WfBpmnExtensionReader.toNodes(101L, model));

		assertEquals(keys(fromDb), keys(fromBpmn), "两种读源的候选必须一致");
		// taskA 经折叠连线直达（由近及远排前）；taskB 经 gw1；taskD 被 is_reject=1 封锁；
		// taskE(归档, nodeType=3) 引擎停不住被剔除；gw1 无节点对象被跳过
		assertEquals(List.of("taskA", "taskB"), keys(fromBpmn));
	}

	@Test
	@DisplayName("白名单裁剪：settings.reject.nodeKeys 限制可选范围（rejectType=2）")
	void whitelistCropsCandidates() {
		BpmnModel model = parse(BPMN);
		List<WfProcessNode> nodes = WfBpmnExtensionReader.toNodes(101L, model);
		List<WfNodeLink> links = WfBpmnExtensionReader.toNodeLinks(101L, model);

		WfProcessNode current = node("taskC", 1);
		current.setExtJson("{\"settings\":{\"reject\":{\"type\":2,\"nodeKeys\":[\"taskB\"]}}}");
		List<WfProcessNode> candidates = WfRejectManager.computeRejectableNodes(
			101L, "taskC", current, links, nodes);
		assertEquals(List.of("taskB"), keys(candidates), "白名单之外的候选（taskA）应被裁掉");
	}

	@Test
	@DisplayName("空连线 / 空节点数据：返回空候选，不抛异常（兜底语义）")
	void emptyDataReturnsEmpty() {
		assertTrue(WfRejectManager.computeRejectableNodes(101L, "taskC", null, List.of(), List.of()).isEmpty());
	}
}
