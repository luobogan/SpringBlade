package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springblade.workflow.action.WfActionExecutor;
import org.springblade.workflow.action.WfServiceTaskDelegate;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * E1 ServiceTask 自动执行的引擎级集成测试（H2 内存库，真实 Flowable 引擎）。
 *
 * <p>验证链路：BPMN 中 ServiceTask 带 {@code flowable:delegateExpression="${wfServiceTaskDelegate}"}
 * 与 {@code wf:node/customAction}（即部署注入后的形态）→ 发起实例 → 引擎解析 Spring Bean →
 * 真实 delegate 反读 actionKey / 反查实例 → 调用 {@code executeCustomAction} → token 停在 userTask。</p>
 *
 * <p>只 mock 业务侧依赖（Mapper / 执行器），不拉起完整业务表链——那是 E2E 的职责。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FlowableTestConfig.class, WfServiceTaskDelegateIntegrationTest.E1Config.class})
class WfServiceTaskDelegateIntegrationTest {

    private static final String PROC_KEY = "e1DelegateTest";
    private static final Long INST_ID = 999L;
    private static final Long DEF_ID = 888L;
    private static final Long STARTER = 676L;

    /** 测试装配：真实 delegate + mock 业务依赖， Bean 名必须为 wfServiceTaskDelegate 供表达式解析。 */
    @Configuration
    static class E1Config {
        @Bean
        WfFlowtableCompatProperties compatProperties() {
            WfFlowtableCompatProperties p = new WfFlowtableCompatProperties();
            p.setServiceTaskExec(true);
            return p;
        }

        @Bean
        WfInstanceMapper instanceMapper() {
            return mock(WfInstanceMapper.class);
        }

        @Bean
        WfActionExecutor actionExecutor() {
            return mock(WfActionExecutor.class);
        }

        @Bean("wfServiceTaskDelegate")
        WfServiceTaskDelegate wfServiceTaskDelegate(WfFlowtableCompatProperties compat,
                                                    WfInstanceMapper mapper,
                                                    WfActionExecutor executor) {
            return new WfServiceTaskDelegate(compat, mapper, executor);
        }
    }

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private IProcessService processService;
    @Autowired private WfInstanceMapper instanceMapper;
    @Autowired private WfActionExecutor actionExecutor;
    @Autowired private WfFlowtableCompatProperties compat;

    private final Set<String> deployed = new HashSet<>();

    @BeforeEach
    void resetMocks() {
        reset(instanceMapper, actionExecutor);
        compat.setServiceTaskExec(true);
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(STARTER);
        when(instanceMapper.selectOne(any())).thenReturn(inst);
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            processService.deleteDeployment(depId);
        }
        deployed.clear();
    }

    @Test
    void serviceTask自动执行_走执行器入口_停在userTask() {
        Deployment dep = repositoryService.createDeployment()
            .key(PROC_KEY).name(PROC_KEY).addString(PROC_KEY + ".bpmn20.xml", bpmn()).deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROC_KEY);
        assertThat(pi).isNotNull();

        ArgumentCaptor<WfProcessNode> nodeCap = ArgumentCaptor.forClass(WfProcessNode.class);
        verify(actionExecutor).executeCustomAction(any(WfInstance.class), nodeCap.capture(), eq("serviceTask"),
            eq("demoAction"), isNull(), any());
        assertThat(nodeCap.getValue().getNodeKey()).isEqualTo("svcAuto");
        assertThat(nodeCap.getValue().getNodeType()).isEqualTo(6);

        assertThat(processService.currentTasks(pi.getId()))
            .extracting(t -> t.getTaskDefinitionKey())
            .containsExactly("userApprove");
    }

    @Test
    void 运行时开关关闭_delegate短路不执行动作() {
        compat.setServiceTaskExec(false);
        try {
            String key = PROC_KEY + "_off";
            Deployment dep = repositoryService.createDeployment()
                .key(key).name(key).addString(key + ".bpmn20.xml", bpmn(key)).deploy();
            deployed.add(dep.getId());

            ProcessInstance pi = runtimeService.startProcessInstanceByKey(key);
            assertThat(pi).isNotNull();

            verify(actionExecutor, never()).executeCustomAction(any(), any(), any(), any(), any(), any());
            assertThat(processService.currentTasks(pi.getId()))
                .extracting(t -> t.getTaskDefinitionKey())
                .containsExactly("userApprove");
        } finally {
            compat.setServiceTaskExec(true);
        }
    }

    private static String bpmn() {
        return bpmn(PROC_KEY);
    }

    private static String bpmn(String key) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         xmlns:wf="http://www.springblade.org/workflow"
                         id="Definitions_%s" targetNamespace="http://www.springblade.org/workflow">
              <process id="%s" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="svcAuto"/>
                <serviceTask id="svcAuto" name="自动动作" flowable:delegateExpression="${wfServiceTaskDelegate}">
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
            """.formatted(key, key);
    }
}
