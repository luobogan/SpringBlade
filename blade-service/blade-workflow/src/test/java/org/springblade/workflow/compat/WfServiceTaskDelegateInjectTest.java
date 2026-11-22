package org.springblade.workflow.compat;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.impl.WfDefinitionServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Answers.CALLS_REAL_METHODS;

/**
 * E1 部署期注入单测（2026-11-18）：验证开关语义与「仅注入挂了 customAction 的 ServiceTask」。
 *
 * <p>注入方法是 WfDefinitionServiceImpl 的私有部署归一化步骤，这里用 CALLS_REAL_METHODS 代理 +
 * ReflectionTestUtils 只驱动该方法，避开构造 20+ 个 Mapper 依赖。</p>
 */
class WfServiceTaskDelegateInjectTest {

    @Test
    void 开关关闭_原样返回不改BPMN() {
        assertThat(inject(false, null, BPMN)).isEqualTo(BPMN);
    }

    @Test
    void 开关打开_有customAction的ServiceTask注入delegateExpression() {
        String out = inject(true, null, BPMN);
        ServiceTask st = loadServiceTask(out);
        assertThat(st.getImplementationType()).isEqualTo(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        assertThat(st.getImplementation()).isEqualTo("${wfServiceTaskDelegate}");
    }

    @Test
    void 开关打开_无customAction不注入() {
        assertThat(inject(true, null, BPMN_NO_ACTION)).isEqualTo(BPMN_NO_ACTION);
    }

    @Test
    void 灰度名单外_不注入() {
        // 名单非空且 defId 不在其中 → 保持原生 ServiceTask 语义
        assertThat(inject(true, List.of(777L), BPMN)).isEqualTo(BPMN);
    }

    @Test
    void 灰度名单内_注入() {
        String out = inject(true, List.of(888L), BPMN);
        ServiceTask st = loadServiceTask(out);
        assertThat(st.getImplementation()).isEqualTo("${wfServiceTaskDelegate}");
    }

    @Test
    void 声明了customAction但BPMN畸形_注入失败failFast中止发布() {
        // 含 customAction 声明但 XML 被截断（无法解析）→ converter 抛异常 → 必须 fail-fast 而非静默回退，
        // 否则「发布成功但运行期 token 卡在无实现的 ServiceTask」。
        assertThatThrownBy(() -> inject(true, null, MALFORMED_WITH_ACTION))
            .isInstanceOf(org.springblade.core.log.exception.ServiceException.class)
            .hasMessageContaining("E1 注入失败");
    }

    private static String inject(boolean enabled, List<Long> gray, String xml) {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setServiceTaskExec(enabled);
        if (gray != null) {
            compat.setServiceTaskExecGrayDefIds(gray);
        }
        WfDefinitionServiceImpl svc = mock(WfDefinitionServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(svc, "compat", compat);
        return ReflectionTestUtils.invokeMethod(svc, "injectServiceTaskDelegate", xml, 888L);
    }

    private static ServiceTask loadServiceTask(String xml) {
        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(
            () -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false, false);
        return (ServiceTask) model.getMainProcess().getFlowElement("svcAuto");
    }

    private static final String BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:flowable="http://flowable.org/bpmn"
                     xmlns:wf="http://www.springblade.org/workflow"
                     targetNamespace="http://www.springblade.org/workflow">
          <process id="e1InjectTest" isExecutable="true">
            <startEvent id="start"/>
            <sequenceFlow id="f1" sourceRef="start" targetRef="svcAuto"/>
            <serviceTask id="svcAuto" name="自动动作">
              <extensionElements>
                <wf:node nodeType="6">
                  <wf:customAction actionKey="demoAction" name="演示动作"/>
                </wf:node>
              </extensionElements>
            </serviceTask>
            <sequenceFlow id="f2" sourceRef="svcAuto" targetRef="userApprove"/>
            <userTask id="userApprove" name="审批"/>
            <sequenceFlow id="f3" sourceRef="userApprove" targetRef="end"/>
            <endEvent id="end"/>
          </process>
        </definitions>
        """;

    private static final String BPMN_NO_ACTION = BPMN.replace(
        "<wf:customAction actionKey=\"demoAction\" name=\"演示动作\"/>", "");

    /** 含 customAction 声明但结构被截断 → BpmnXMLConverter 解析必失败 */
    private static final String MALFORMED_WITH_ACTION =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
        + " xmlns:wf=\"http://www.springblade.org/workflow\" targetNamespace=\"http://x\">"
        + "<process id=\"p\" isExecutable=\"true\"><serviceTask id=\"svc\">"
        + "<extensionElements><wf:node nodeType=\"6\"><wf:customAction actionKey=\"k\"/>"
        + "</wf:node></extensionElements></serviceTask>";
}
