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
 * ② 写回（{@code convertToXML}）后再次解析<b>数据无损</b>。</p>
 *
 * <p>本测试为<b>纯单元测试</b>：不依赖 Spring 容器与数据库，只依赖
 * {@code flowable-bpmn-converter}。</p>
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
        "node", "link", "operator", "fieldPerm", "timeout", "extJson", "extraOperations"
    );

    private static final String WF_NS = "http://www.springblade.io/wf";

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns:flowable="http://flowable.org/bpmn"
                     xmlns:wf="http://www.springblade.io/wf"
                     targetNamespace="http://www.springblade.io/test">
          <process id="testProcess" name="Test" isExecutable="true">
            <startEvent id="start1"/>
            <userTask id="approve1" name="部门经理审批">
              <extensionElements>
                <wf:node nodeType="1" signOrder="1" mergeType="0" passNum="0"
                         allowReject="1" allowForward="1" autoApprove="0" sortOrder="2" testStatus="0">
                  <wf:extJson><![CDATA[{"remind":{"types":"sys"}}]]></wf:extJson>
                  <wf:operator groupNo="0" opType="1" objId="10" bhxj="1" levelMin="0" levelMax="99"/>
                  <wf:operator groupNo="1" opType="17"/>
                  <wf:fieldPerm field="amount" perm="edit"/>
                  <wf:timeout seq="0" enabled="1" durationMin="60" actionWay="autoApprove"/>
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

        assertEquals("1", attr(node, "nodeType"));
        assertEquals("1", attr(node, "signOrder"));
        assertEquals("0", attr(node, "autoApprove"));
        assertEquals("2", attr(node, "sortOrder"));
    }

    @Test
    @DisplayName("解析 wf:node 下的嵌套子元素（operator / fieldPerm / timeout / extJson）")
    void parseNestedChildren() {
        UserTask task = (UserTask) parse(BPMN).getMainProcess().getFlowElement("approve1");
        ExtensionElement node = ext(task, "node");

        // 多个同名子元素（两条操作者规则）
        List<ExtensionElement> operators = node.getChildElements().get("operator");
        assertNotNull(operators, "应解析出 wf:operator");
        assertEquals(2, operators.size(), "应有两条操作者规则");
        assertEquals("0", attr(operators.get(0), "groupNo"));
        assertEquals("1", attr(operators.get(0), "opType"));
        assertEquals("10", attr(operators.get(0), "objId"));
        assertEquals("17", attr(operators.get(1), "opType"));

        ExtensionElement fieldPerm = child(node, "fieldPerm");
        assertNotNull(fieldPerm);
        assertEquals("amount", attr(fieldPerm, "field"));
        assertEquals("edit", attr(fieldPerm, "perm"));

        ExtensionElement timeout = child(node, "timeout");
        assertNotNull(timeout);
        assertEquals("60", attr(timeout, "durationMin"));
        assertEquals("autoApprove", attr(timeout, "actionWay"));

        // CDATA 文本
        ExtensionElement extJson = child(node, "extJson");
        assertNotNull(extJson);
        assertEquals("{\"remind\":{\"types\":\"sys\"}}", extJson.getElementText());
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

        // 多行 CDATA（出口附加操作脚本）
        ExtensionElement extra = child(link, "extraOperations");
        assertNotNull(extra);
        assertTrue(extra.getElementText().contains("line1"),
            "多行 CDATA 应保留，实际=" + extra.getElementText());
        assertTrue(extra.getElementText().contains("line2"),
            "多行 CDATA 应保留，实际=" + extra.getElementText());

        // 原生条件表达式独立可读（不与被扩展元素混淆）
        assertNotNull(flow.getConditionExpression(), "原生 conditionExpression 应存在");
    }

    // ------------------------------------------------------------ 往返保真

    @Test
    @DisplayName("V7/V13：写回后再解析，扩展数据无损")
    void roundTripPreservesExtensionData() {
        BpmnModel model1 = parse(BPMN);
        String xml2 = write(model1);

        // 序列化结果仍包含自定义扩展
        assertTrue(xml2.contains("wf:node"), "写回后应保留 wf:node");
        assertTrue(xml2.contains("wf:link"), "写回后应保留 wf:link");
        assertTrue(xml2.contains("wf:operator"), "写回后应保留 wf:operator");

        // 再解析：属性与嵌套结构应与首次解析完全一致
        BpmnModel model2 = parse(xml2);
        assertSameExtension(model1, model2);
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
        UserTask t1 = (UserTask) m1.getMainProcess().getFlowElement("approve1");
        UserTask t2 = (UserTask) m2.getMainProcess().getFlowElement("approve1");
        assertNotNull(t1);
        assertNotNull(t2);

        ExtensionElement n1 = ext(t1, "node");
        ExtensionElement n2 = ext(t2, "node");
        assertNotNull(n1);
        assertNotNull(n2, "往返后 wf:node 丢失");

        for (String a : List.of("nodeType", "signOrder", "mergeType", "passNum",
            "allowReject", "allowForward", "autoApprove", "sortOrder", "testStatus")) {
            assertEquals(attr(n1, a), attr(n2, a), "属性 '" + a + "' 往返后不一致");
        }

        assertEquals(2, n1.getChildElements().get("operator").size());
        assertEquals(2, n2.getChildElements().get("operator").size(), "往返后 operator 数量不一致");
        assertEquals(attr(n1.getChildElements().get("operator").get(0), "objId"),
            attr(n2.getChildElements().get("operator").get(0), "objId"), "operator 属性往返后不一致");

        assertEquals(child(n1, "extJson").getElementText(),
            child(n2, "extJson").getElementText(), "extJson CDATA 往返后不一致");

        SequenceFlow f1 = (SequenceFlow) m1.getMainProcess().getFlowElement("flow1");
        SequenceFlow f2 = (SequenceFlow) m2.getMainProcess().getFlowElement("flow1");
        ExtensionElement l1 = ext(f1, "link");
        ExtensionElement l2 = ext(f2, "link");
        assertNotNull(l1);
        assertNotNull(l2, "往返后 wf:link 丢失");
        assertEquals(attr(l1, "conditionCn"), attr(l2, "conditionCn"));
        assertTrue(child(l2, "extraOperations").getElementText().contains("line1"),
            "多行 CDATA 往返后丢失");
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
        List<ExtensionAttribute> attrs = element.getAttributes().get(name);
        return (attrs == null || attrs.isEmpty()) ? null : attrs.get(0).getValue();
    }
}
