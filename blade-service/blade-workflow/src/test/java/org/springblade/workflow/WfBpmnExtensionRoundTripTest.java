package org.springblade.workflow;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.util.BpmnExtensionUtil.WfLinkExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfNodeExt;
import org.springblade.workflow.util.BpmnExtensionUtil.WfProcessMetaExt;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BPMN 自定义扩展（{@code wf:}）的解析与往返保真测试。
 *
 * <p>对应方案验证点 <b>V7 / V13</b>：验证 Flowable 能
 * ① 解析自定义命名空间的 {@code extensionElements}；
 * ② 写回（{@code convertToXML}）后再次解析<b>数据无损</b>；
 * ③ 经 {@link BpmnExtensionUtil} 构建/写回后数据无损（构建路径）。</p>
 *
 * <p>本测试为<b>纯单元测试</b>：不依赖 Spring 容器与数据库，只依赖 {@code flowable-bpmn-converter}。</p>
 *
 * <p><b>命名约束（重要）</b>：Flowable 的子元素解析器按 <b>localName</b> 分发
 * （见 {@code BpmnXMLUtil#genericChildParserMap}），因此自定义元素名<b>不得</b>与
 * Flowable 已注册名冲突，否则会被当作 Flowable 原生子元素解析而语义被吞。
 * 禁用名见 {@link #RESERVED_LOCAL_NAMES}。</p>
 */
class WfBpmnExtensionRoundTripTest {

	/** Flowable 已注册的子元素解析器名（节选高危项）—— 自定义元素名不得与之相同 */
	private static final List<String> RESERVED_LOCAL_NAMES = List.of(
		"condition", "conditionExpression", "documentation",
		"executionListener", "taskListener", "formProperty", "field",
		"timerEventDefinition", "timeDate", "timeCycle", "timeDuration",
		"multiInstanceLoopCharacteristics", "script", "eventListener"
	);

	/** 本项目使用的自定义元素名（须全部不与保留名冲突） */
	private static final List<String> OUR_LOCAL_NAMES = List.of(
		"node", "link", "processMeta", "operator", "fieldPerm", "detailPerm",
		"detailFilter", "timeout", "customAction", "operation", "right",
		"extJson", "extraOperations"
	);

	private static final String WF_NS = BpmnExtensionUtil.WF_NS;

	private static final String BPMN = """
		<?xml version="1.0" encoding="UTF-8"?>
		<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
		             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		             xmlns:flowable="http://flowable.org/bpmn"
		             xmlns:wf="http://www.springblade.org/workflow"
		             targetNamespace="http://www.springblade.org/workflow">
		  <process id="testProcess" name="Test" isExecutable="true">
		    <extensionElements>
		      <wf:processMeta defKey="LEAVE-2026" workflowType="1" formId="1001" layoutId="2001"
		                     grayEnabled="0" grayRule=""/>
		    </extensionElements>
		    <startEvent id="start1"/>
		    <userTask id="approve1" name="部门经理审批">
		      <extensionElements>
		        <wf:node nodeType="2" signOrder="1" mergeType="0" passNum="0"
		                 allowReject="1" allowForward="1" autoApprove="0" sortOrder="2" testStatus="0" multiInstance="1">
		          <wf:extJson><![CDATA[{"remind":{"types":"sys"}}]]></wf:extJson>
		          <wf:operator groupNo="0" opType="1" objId="10" bhxj="1" levelMin="0" levelMax="99"/>
		          <wf:operator groupNo="1" opType="17"/>
		          <wf:fieldPerm field="amount" perm="edit"/>
		          <wf:detailPerm dtKey="dt1" field="lineAmount" perm="readonly"/>
		          <wf:detailFilter dtKey="dt1" rowFilter="${row.amount > 100}"/>
		          <wf:timeout seq="0" enabled="1" durationMin="60" actionWay="autoApprove"/>
		          <wf:customAction actionKey="print" name="打印" type="1" url="/print"/>
		          <wf:operation btnName="审批通过" btnOrder="1" actionType="2" enabled="1">
		            <wf:right rightType="role" rightValue="dept_manager"/>
		          </wf:operation>
		        </wf:node>
		      </extensionElements>
		    </userTask>
		    <sequenceFlow id="flow1" sourceRef="approve1" targetRef="end1">
		      <extensionElements>
		        <wf:link isReject="0" isMustPass="1" conditionCn="金额大于1000" sortOrder="0" viaGateway="0">
		          <wf:extraOperations><![CDATA[line1
		line2]]></wf:extraOperations>
		        </wf:link>
		      </extensionElements>
		      <conditionExpression xsi:type="tFormalExpression">${amount &gt; 1000}</conditionExpression>
		    </sequenceFlow>
		    <endEvent id="end1"/>
		  </process>
		</definitions>
		""";

	// ---------------------------------------------------------------- 解析

	@Test
	@DisplayName("解析 userTask 上的 wf:node 属性")
	void parseNodeAttributes() {
		UserTask task = (UserTask) parse(BPMN).getMainProcess().getFlowElement("approve1");
		assertNotNull(task, "应解析出 userTask approve1");

		ExtensionElement node = ext(task, "node");
		assertNotNull(node, "wf:node 应被解析到 extensionElements");
		assertEquals(WF_NS, node.getNamespace());
		assertEquals("wf", node.getNamespacePrefix());

		assertEquals("2", attr(node, "nodeType"));
		assertEquals("1", attr(node, "signOrder"));
		assertEquals("1", attr(node, "multiInstance"));
		assertEquals("0", attr(node, "autoApprove"));
		assertEquals("2", attr(node, "sortOrder"));
	}

	@Test
	@DisplayName("解析 wf:node 下的全部嵌套子元素（operator/fieldPerm/detailPerm/detailFilter/timeout/customAction/operation/extJson）")
	void parseNestedChildren() {
		UserTask task = (UserTask) parse(BPMN).getMainProcess().getFlowElement("approve1");
		ExtensionElement node = ext(task, "node");

		assertEquals(2, node.getChildElements().get("operator").size(), "应有两条操作者规则");
		assertEquals("10", attr(node.getChildElements().get("operator").get(0), "objId"));

		assertNotNull(child(node, "fieldPerm"));
		assertEquals("amount", attr(child(node, "fieldPerm"), "field"));
		assertEquals("edit", attr(child(node, "fieldPerm"), "perm"));

		assertNotNull(child(node, "detailPerm"));
		assertEquals("dt1", attr(child(node, "detailPerm"), "dtKey"));

		assertNotNull(child(node, "detailFilter"));
		assertEquals("${row.amount > 100}", attr(child(node, "detailFilter"), "rowFilter"));

		assertNotNull(child(node, "timeout"));
		assertEquals("autoApprove", attr(child(node, "timeout"), "actionWay"));

		assertNotNull(child(node, "customAction"));
		assertEquals("print", attr(child(node, "customAction"), "actionKey"));

		ExtensionElement op = child(node, "operation");
		assertNotNull(op);
		assertEquals("审批通过", attr(op, "btnName"));
		assertEquals(1, op.getChildElements().get("right").size());
		assertEquals("dept_manager", attr(op.getChildElements().get("right").get(0), "rightValue"));

		assertEquals("{\"remind\":{\"types\":\"sys\"}}", child(node, "extJson").getElementText());
	}

	@Test
	@DisplayName("解析 sequenceFlow 上的 wf:link 与原生 conditionExpression")
	void parseLinkAndNativeCondition() {
		Process process = parse(BPMN).getMainProcess();
		SequenceFlow flow = (SequenceFlow) process.getFlowElement("flow1");
		assertNotNull(flow);

		ExtensionElement link = ext(flow, "link");
		assertNotNull(link, "wf:link 应被解析");
		assertEquals("0", attr(link, "isReject"));
		assertEquals("1", attr(link, "isMustPass"));
		assertEquals("金额大于1000", attr(link, "conditionCn"));

		ExtensionElement extra = child(link, "extraOperations");
		assertNotNull(extra);
		assertTrue(extra.getElementText().contains("line1"));
		assertTrue(extra.getElementText().contains("line2"));

		assertNotNull(flow.getConditionExpression(), "原生 conditionExpression 应存在");
	}

	@Test
	@DisplayName("解析 process 级 wf:processMeta（defKey 桥接 / 表单 / 类型 / 灰度）")
	void parseProcessMeta() {
		WfProcessMetaExt meta = BpmnExtensionUtil.readProcessMeta(parse(BPMN).getMainProcess());
		assertNotNull(meta);
		assertEquals("LEAVE-2026", meta.defKey);
		assertEquals("1001", meta.formId);
		assertEquals("2001", meta.layoutId);
		assertEquals("0", meta.grayEnabled);
	}

	// ------------------------------------------------------------ 往返保真（纯序列化）

	@Test
	@DisplayName("V7/V13：写回后再解析，扩展数据无损")
	void roundTripPreservesExtensionData() {
		BpmnModel model1 = parse(BPMN);
		String xml2 = write(model1);

		assertTrue(xml2.contains("wf:node"), "写回后应保留 wf:node");
		assertTrue(xml2.contains("wf:link"), "写回后应保留 wf:link");
		assertTrue(xml2.contains("wf:processMeta"), "写回后应保留 wf:processMeta");
		assertTrue(xml2.contains("wf:detailPerm"), "写回后应保留 wf:detailPerm");

		assertSameExtension(model1, parse(xml2));
	}

	@Test
	@DisplayName("V13：连续两次往返（模拟多次部署/保存）仍无损")
	void repeatedRoundTripKeepsData() {
		BpmnModel model = parse(BPMN);
		String xml = write(model);
		for (int i = 0; i < 2; i++) {
			xml = write(parse(xml));
		}
		assertSameExtension(model, parse(xml));
	}

	// ---------------------------------------------------- 往返保真（经 BpmnExtensionUtil 写回）

	@Test
	@DisplayName("经 BpmnExtensionUtil.writeNode/writeLink/writeProcessMeta 写回后无损（构建路径）")
	void utilWriteThenParseKeepsData() {
		BpmnModel model = parse(BPMN);
		Process process = model.getMainProcess();
		UserTask task = (UserTask) process.getFlowElement("approve1");
		SequenceFlow flow = (SequenceFlow) process.getFlowElement("flow1");

		// 读 -> 写回（同元素，验证幂等不重复叠加）
		BpmnExtensionUtil.writeNode(task, BpmnExtensionUtil.readNode(task));
		BpmnExtensionUtil.writeLink(flow, BpmnExtensionUtil.readLink(flow));
		BpmnExtensionUtil.writeProcessMeta(process, BpmnExtensionUtil.readProcessMeta(process));

		BpmnModel model2 = parse(write(model));
		assertSameExtension(model, model2);
		assertEquals(2, BpmnExtensionUtil.readNode((UserTask) model2.getMainProcess().getFlowElement("approve1"))
			.operators.size(), "写回后 operator 不应重复叠加");
	}

	// -------------------------------------------------------- 命名冲突校验

	@Test
	@DisplayName("自定义元素名不得与 Flowable 已注册的 localName 冲突")
	void customElementNamesDoNotCollide() {
		for (String name : OUR_LOCAL_NAMES) {
			assertFalse(RESERVED_LOCAL_NAMES.contains(name),
				"自定义元素名 '" + name + "' 与 Flowable 已注册名冲突，会导致语义被吞");
		}
	}

	// ---------------------------------------------------------------- 工具

	private static BpmnModel parse(String xml) {
		BpmnXMLConverter converter = new BpmnXMLConverter();
		return converter.convertToBpmnModel(
			() -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
	}

	private static String write(BpmnModel model) {
		return new String(new BpmnXMLConverter().convertToXML(model), StandardCharsets.UTF_8);
	}

	/** 断言两次解析得到的自定义扩展完全一致 */
	private static void assertSameExtension(BpmnModel m1, BpmnModel m2) {
		WfNodeExt n1 = BpmnExtensionUtil.readNode((UserTask) m1.getMainProcess().getFlowElement("approve1"));
		WfNodeExt n2 = BpmnExtensionUtil.readNode((UserTask) m2.getMainProcess().getFlowElement("approve1"));
		assertNotNull(n1);
		assertNotNull(n2, "往返后 wf:node 丢失");

		for (String a : List.of("nodeType", "signOrder", "mergeType", "passNum",
			"allowReject", "allowForward", "autoApprove", "sortOrder", "testStatus", "multiInstance")) {
			assertEquals(attr(n1OrElement(m1), a), attr(n1OrElement(m2), a), "属性 '" + a + "' 往返后不一致");
		}

		assertEquals(2, n1.operators.size());
		assertEquals(2, n2.operators.size(), "往返后 operator 数量不一致");
		assertEquals(n1.operators.get(0).objId, n2.operators.get(0).objId, "operator 属性往返后不一致");

		assertEquals(n1.fieldPerms.size(), n2.fieldPerms.size());
		assertEquals(n1.detailPerms.size(), n2.detailPerms.size(), "detailPerm 往返后不一致");
		assertEquals(n1.detailFilters.size(), n2.detailFilters.size(), "detailFilter 往返后不一致");
		assertEquals(n1.timeouts.size(), n2.timeouts.size(), "timeout 往返后不一致");
		assertEquals(n1.customActions.size(), n2.customActions.size(), "customAction 往返后不一致");
		assertEquals(n1.operations.size(), n2.operations.size(), "operation 往返后不一致");
		assertEquals(n1.operations.get(0).rights.size(), n2.operations.get(0).rights.size(), "right 往返后不一致");

		assertEquals(n1.extJson, n2.extJson, "extJson CDATA 往返后不一致");

		WfLinkExt l1 = BpmnExtensionUtil.readLink((SequenceFlow) m1.getMainProcess().getFlowElement("flow1"));
		WfLinkExt l2 = BpmnExtensionUtil.readLink((SequenceFlow) m2.getMainProcess().getFlowElement("flow1"));
		assertNotNull(l1);
		assertNotNull(l2, "往返后 wf:link 丢失");
		assertEquals(l1.conditionCn, l2.conditionCn);
		assertEquals(l1.extraOperations, l2.extraOperations, "多行 CDATA 往返后丢失");

		assertEquals(BpmnExtensionUtil.readProcessMeta(m1.getMainProcess()).defKey,
			BpmnExtensionUtil.readProcessMeta(m2.getMainProcess()).defKey, "processMeta 往返后不一致");
	}

	/** 取 wf:node 元素（与 model 读取对照，验证值一致） */
	private static ExtensionElement n1OrElement(BpmnModel m) {
		return ext((UserTask) m.getMainProcess().getFlowElement("approve1"), "node");
	}

	private static ExtensionElement ext(org.flowable.bpmn.model.FlowElement element, String name) {
		List<ExtensionElement> list = element.getExtensionElements().get(name);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	private static ExtensionElement child(ExtensionElement parent, String name) {
		List<ExtensionElement> list = parent.getChildElements().get(name);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	private static String attr(ExtensionElement element, String name) {
		List<ExtensionAttribute> list = element.getAttributes().get(name);
		return (list == null || list.isEmpty()) ? null : list.get(0).getValue();
	}
}
