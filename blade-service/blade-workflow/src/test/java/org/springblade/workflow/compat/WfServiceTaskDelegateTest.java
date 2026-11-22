package org.springblade.workflow.compat;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.action.WfActionExecutor;
import org.springblade.workflow.action.WfServiceTaskDelegate;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * E1 统一 delegate 单测（2026-11-18）：验证开关/节点类型/actionKey/实例反查的分支判定，
 * 以及「必须走带 gate 的 executeCustomAction 入口」这一核心约束。
 */
@ExtendWith(MockitoExtension.class)
class WfServiceTaskDelegateTest {

    private static final String PROC_INST_ID = "procInst-1";
    private static final Long INST_ID = 999L;
    private static final Long DEF_ID = 888L;

    @Mock private WfFlowtableCompatProperties compat;
    @Mock private WfInstanceMapper instanceMapper;
    @Mock private WfActionExecutor actionExecutor;
    @Mock private DelegateExecution execution;

    @InjectMocks private WfServiceTaskDelegate delegate;

    @Test
    void 开关关闭_跳过执行() {
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);

        delegate.execute(execution);

        verifyNoInteractions(actionExecutor);
    }

    @Test
    void 非ServiceTask_跳过执行() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        // getCurrentFlowElement 默认返回 null → 非 ServiceTask
        delegate.execute(execution);
        verifyNoInteractions(actionExecutor);
    }

    @Test
    void serviceTask无customAction_跳过执行() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        ServiceTask st = loadServiceTask(BPMN_NO_ACTION);
        when(execution.getCurrentFlowElement()).thenReturn(st);

        delegate.execute(execution);

        verifyNoInteractions(actionExecutor);
    }

    @Test
    void 反查不到业务实例_跳过执行() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(execution.getCurrentFlowElement()).thenReturn(loadServiceTask(BPMN));
        when(instanceMapper.selectOne(any())).thenReturn(null);

        delegate.execute(execution);

        verifyNoInteractions(actionExecutor);
    }

    @Test
    void 正常_走带gate的执行器入口且节点上下文正确() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(execution.getCurrentFlowElement()).thenReturn(loadServiceTask(BPMN));
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(676L);
        inst.setEngineInstId(PROC_INST_ID);
        when(instanceMapper.selectOne(any())).thenReturn(inst);

        delegate.execute(execution);

        ArgumentCaptor<WfProcessNode> nodeCap = ArgumentCaptor.forClass(WfProcessNode.class);
        verify(actionExecutor).executeCustomAction(eq(inst), nodeCap.capture(), eq("serviceTask"),
            eq("demoAction"), isNull(), any());
        assertThat(nodeCap.getValue().getNodeKey()).isEqualTo("svcAuto");
        assertThat(nodeCap.getValue().getNodeName()).isEqualTo("自动动作");
        assertThat(nodeCap.getValue().getDefId()).isEqualTo(DEF_ID);
        assertThat(nodeCap.getValue().getNodeType()).isEqualTo(6);
    }

    @Test
    void 灰度名单外_跳过不执行动作() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(compat.getServiceTaskExecGrayDefIds()).thenReturn(List.of(777L)); // DEF_ID=888 不在名单
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(execution.getCurrentFlowElement()).thenReturn(loadServiceTask(BPMN));
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(676L);
        inst.setEngineInstId(PROC_INST_ID);
        when(instanceMapper.selectOne(any())).thenReturn(inst);

        delegate.execute(execution);

        verifyNoInteractions(actionExecutor);
    }

    @Test
    void 灰度名单内_正常执行() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(compat.getServiceTaskExecGrayDefIds()).thenReturn(List.of(DEF_ID));
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(execution.getCurrentFlowElement()).thenReturn(loadServiceTask(BPMN));
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(676L);
        inst.setEngineInstId(PROC_INST_ID);
        when(instanceMapper.selectOne(any())).thenReturn(inst);

        delegate.execute(execution);

        verify(actionExecutor).executeCustomAction(eq(inst), any(), eq("serviceTask"), eq("demoAction"),
            isNull(), any());
    }

    @Test
    void 引擎变量透传给动作() {
        when(compat.isServiceTaskExec()).thenReturn(true);
        when(execution.getProcessInstanceId()).thenReturn(PROC_INST_ID);
        when(execution.getCurrentFlowElement()).thenReturn(loadServiceTask(BPMN));
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("approver", 676L);
        when(execution.getVariables()).thenReturn(vars);
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(676L);
        inst.setEngineInstId(PROC_INST_ID);
        when(instanceMapper.selectOne(any())).thenReturn(inst);

        delegate.execute(execution);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varCap = ArgumentCaptor.forClass(Map.class);
        verify(actionExecutor).executeCustomAction(eq(inst), any(), eq("serviceTask"), eq("demoAction"),
            isNull(), varCap.capture());
        assertThat(varCap.getValue()).containsEntry("approver", 676L);
    }

    @Test
    void 实现JavaDelegate接口() {
        assertThat(delegate).isInstanceOf(JavaDelegate.class);
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
          <process id="e1Test" isExecutable="true">
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
}
