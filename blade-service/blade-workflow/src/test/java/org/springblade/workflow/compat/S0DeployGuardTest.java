package org.springblade.workflow.compat;

import org.junit.jupiter.api.Test;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.service.impl.WfDefinitionServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * S0 防呆单测（2026-11-18）：发布期对「画了也跑不通」的未实现元素硬拦，
 * 杜绝「测试态静默降级 → 测试绿灯 → 正式发布后卡单/空跑」。
 *
 * <p>{@code validateForDeploy} 是 WfDefinitionServiceImpl 的私有部署门禁，这里用 CALLS_REAL_METHODS 代理
 * 只驱动该方法，避开构造 20+ 个 Mapper 依赖。注意本方法还会检查 wf:node / operator，
 * 因此「合法」样例也必须带完整节点扩展与操作者。</p>
 */
class S0DeployGuardTest {

    @Test
    void 合法流程_userTask带节点扩展与操作者_放行() {
        WfProcessDefinition def = def(VALID_WITH_OPERATOR);
        assertThatCode(() -> validate(def, false))
            .doesNotThrowAnyException();
    }

    @Test
    void scriptTask_硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:scriptTask id=\"S1\" name=\"脚本\"><bpmn:extensionElements><wf:node nodeType=\"6\"/>"
                + "</bpmn:extensionElements></bpmn:scriptTask>"), false, "脚本任务");
    }

    @Test
    void callActivity_硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:callActivity id=\"C1\" name=\"子流程调用\"><bpmn:extensionElements><wf:node nodeType=\"6\"/>"
                + "</bpmn:extensionElements></bpmn:callActivity>"), false, "调用活动");
    }

    @Test
    void subProcess_硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:subProcess id=\"SP1\" name=\"内嵌子流程\"><bpmn:startEvent id=\"spStart\"/>"
                + "</bpmn:subProcess>"), false, "子流程");
    }

    @Test
    void receiveTask_硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:receiveTask id=\"R1\" name=\"接收\"><bpmn:extensionElements><wf:node nodeType=\"5\"/>"
                + "</bpmn:extensionElements></bpmn:receiveTask>"), false, "接收任务");
    }

    @Test
    void sendTask_原静默降级_现硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:sendTask id=\"SD1\" name=\"发送\"><bpmn:extensionElements><wf:node nodeType=\"6\"/>"
                + "</bpmn:extensionElements></bpmn:sendTask>"), false, "发送任务");
    }

    @Test
    void serviceTask_E1关闭时硬拦() {
        assertBlocked(ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:serviceTask id=\"ST1\" name=\"服务\"><bpmn:extensionElements><wf:node nodeType=\"6\"/>"
                + "</bpmn:extensionElements></bpmn:serviceTask>"), false, "E1 自动执行未开启");
    }

    @Test
    void serviceTask_E1开启时放行() {
        // E1 打开后 ServiceTask 由部署期注入 delegate，属于受支持元素
        String xml = ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:serviceTask id=\"ST1\" name=\"服务\"><bpmn:extensionElements><wf:node nodeType=\"6\"/>"
                + "</bpmn:extensionElements></bpmn:serviceTask>");
        assertThatCode(() -> validate(def(xml), true)).doesNotThrowAnyException();
    }

    @Test
    void receiveTask_S4唤醒开启时放行() {
        String xml = ELEMENTS.replace("<!--ELEMENT-->",
            "<bpmn:receiveTask id=\"R1\" name=\"接收\"><bpmn:extensionElements><wf:node nodeType=\"5\"/>"
                + "</bpmn:extensionElements></bpmn:receiveTask>");
        assertThatCode(() -> validate(def(xml), false, true)).doesNotThrowAnyException();
    }

    private static void assertBlocked(String xml, boolean e1On, String msgPart) {
        assertThatThrownBy(() -> validate(def(xml), e1On))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("发布被拒绝")
            .hasMessageContaining(msgPart);
    }

    private static WfProcessDefinition def(String xml) {
        WfProcessDefinition d = new WfProcessDefinition();
        d.setId(1L);
        d.setBpmnXml(xml);
        return d;
    }

    private static void validate(WfProcessDefinition def, boolean e1On) {
        validate(def, e1On, false);
    }

    private static void validate(WfProcessDefinition def, boolean e1On, boolean s4On) {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setServiceTaskExec(e1On);
        compat.setS4WakeupEnabled(s4On);
        WfDefinitionServiceImpl svc = mock(WfDefinitionServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(svc, "compat", compat);
        // warnConditionFields 会调 formmodeClient（跨服务），但条件为空时提前 return，不会触发
        ReflectionTestUtils.invokeMethod(svc, "validateForDeploy", def);
    }

    /** 基础骨架：start → 创建UserTask（带 operator）→ ELEMENT → end */
    private static final String ELEMENTS = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:flowable="http://flowable.org/bpmn"
                     xmlns:wf="http://www.springblade.org/workflow"
                     targetNamespace="http://www.springblade.org/workflow">
          <bpmn:process id="s0Test" isExecutable="true">
            <bpmn:startEvent id="start"/>
            <bpmn:sequenceFlow id="f1" sourceRef="start" targetRef="create"/>
            <bpmn:userTask id="create" name="创建">
              <bpmn:extensionElements>
                <wf:node nodeType="0" sortOrder="1">
                  <wf:operator groupNo="1" opType="17"/>
                  <wf:fieldPerm scope="main" field="bt" perm="2" required="0"/>
                </wf:node>
              </bpmn:extensionElements>
            </bpmn:userTask>
            <bpmn:sequenceFlow id="f2" sourceRef="create" targetRef="end"/>
            <!--ELEMENT-->
            <bpmn:endEvent id="end"/>
          </bpmn:process>
          </bpmn:definitions>
          """;

          private static final String VALID_WITH_OPERATOR = ELEMENTS.replace("<!--ELEMENT-->", "");
          }
