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
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.enums.AdvanceSrc;
import org.flowable.engine.impl.test.NoOpServiceTask;
import org.springblade.workflow.listener.WfEngineAutoTodoBridgeListener;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * E7 引擎→业务状态桥的引擎级集成测试（H2 内存库，真实 Flowable 引擎）。
 *
 * <p>验证核心链路：部署 {@code startEvent → serviceTask → userTask} BPMN，发起后
 * ServiceTask 被引擎自动完成 → 派发 {@code ACTIVITY_COMPLETED} → E7 listener 捕获 →
 * 调 {@code advance(instId, starter, ..., SUBMIT)} 同步待办。</p>
 *
 * <p>只对 advance 调用做断言（mock IWfInstanceService），不拉起完整业务 Mapper 链——
 * 那是 E2E（e2e_freeflow_full.py）的职责。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FlowableTestConfig.class, WfEngineAutoTodoBridgeIntegrationTest.E7Config.class})
class WfEngineAutoTodoBridgeIntegrationTest {

    private static final String PROC_KEY = "e7AutoBridgeTest";
    private static final Long DEF_ID = 888L;
    private static final Long INST_ID = 999L;
    private static final Long STARTER = 676L;

    /** 测试装配：mock 依赖的 E7 listener，显式注册为引擎全局监听。 */
    @Configuration
    static class E7Config {
        @Bean
        WfFlowtableCompatProperties compatProperties() {
            WfFlowtableCompatProperties p = new WfFlowtableCompatProperties();
            p.setEngineAutoTodoBridge(true);
            return p;
        }

        @Bean
        IWfInstanceService instanceService() {
            return mock(IWfInstanceService.class);
        }

        @Bean
        WfInstanceMapper instanceMapper() {
            WfInstanceMapper m = mock(WfInstanceMapper.class);
            WfInstance inst = new WfInstance();
            inst.setId(INST_ID);
            inst.setDefId(DEF_ID);
            inst.setStarter(STARTER);
            inst.setEngineInstId("placeholder"); // 实际在测试里按运行时 engineInstId 动态 stub
            when(m.selectOne(any())).thenReturn(inst);
            return m;
        }

        @Bean
        WfEngineAutoTodoBridgeListener e7Listener(WfFlowtableCompatProperties compat,
                                                  WfInstanceMapper mapper,
                                                  IWfInstanceService svc) {
            org.springframework.beans.factory.ObjectProvider<IWfInstanceService> provider =
                mock(org.springframework.beans.factory.ObjectProvider.class);
            when(provider.getObject()).thenReturn(svc);
            return new WfEngineAutoTodoBridgeListener(compat, mapper, provider);
        }
    }

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private IProcessService processService;
    @Autowired private IWfInstanceService instanceService;
    @Autowired private WfInstanceMapper instanceMapper;

    private final Set<String> deployed = new HashSet<>();

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(instanceService, instanceMapper);
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            processService.deleteDeployment(depId);
        }
        deployed.clear();
        NoOpServiceTask.reset();
    }

    @Test
    void serviceTask完成_触发E7同步_停在userTask() {
        Deployment dep = repositoryService.createDeployment()
            .key(PROC_KEY).name(PROC_KEY).addString(PROC_KEY + ".bpmn20.xml", bpmn()).deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROC_KEY);
        assertThat(pi).isNotNull();

        // ServiceTask 应已被引擎自动执行（NoOpServiceTask），token 停在 userTask
        assertThat(NoOpServiceTask.CALL_COUNT.get()).isEqualTo(1);
        assertThat(processService.currentTasks(pi.getId()))
            .extracting(t -> t.getTaskDefinitionKey())
            .containsExactly("userApprove");

        // E7 listener 应收到 ACTIVITY_COMPLETED（serviceTask）并触发 advance
        // engineInstId 动态：让 mapper 的 selectOne 返回带真实 engineInstId 的实例
        WfInstance inst = new WfInstance();
        inst.setId(INST_ID);
        inst.setDefId(DEF_ID);
        inst.setStarter(STARTER);
        inst.setEngineInstId(pi.getId());
        when(instanceMapper.selectOne(any())).thenReturn(inst);

        // 重新发起一个实例，确保这次 mapper 返回正确 engineInstId（上一个实例的事件发生在 stub 之前）
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey(PROC_KEY);

        ArgumentCaptor<Long> instCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AdvanceSrc> srcCaptor = ArgumentCaptor.forClass(AdvanceSrc.class);
        verify(instanceService, atLeastOnce()).advance(instCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(STARTER),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            srcCaptor.capture());

        assertThat(instCaptor.getValue()).isEqualTo(INST_ID);
        assertThat(srcCaptor.getValue()).isEqualTo(AdvanceSrc.SUBMIT);
        assertThat(processService.currentTasks(pi2.getId())).hasSize(1);
    }

    @Test
    void 灰度名单外_不触发E7同步() {
        // 本用例通过动态修改 compat 白名单验证（单例 Bean，测试间注意复位）
        WfFlowtableCompatProperties bean = e7Compat;
        bean.setEngineAutoTodoGrayDefIds(List.of(777L)); // DEF_ID=888 不在名单
        try {
            Deployment dep = repositoryService.createDeployment()
                .key(PROC_KEY + "_gray").name(PROC_KEY + "_gray").addString(PROC_KEY + "_gray.bpmn20.xml", bpmn(PROC_KEY + "_gray")).deploy();
            deployed.add(dep.getId());

            ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROC_KEY + "_gray");
            WfInstance inst = new WfInstance();
            inst.setId(INST_ID); inst.setDefId(DEF_ID); inst.setStarter(STARTER); inst.setEngineInstId(pi.getId());
            when(instanceMapper.selectOne(any())).thenReturn(inst);

            verify(instanceService, never()).advance(any(), any(), any(), any(), any());
        } finally {
            bean.setEngineAutoTodoGrayDefIds(new java.util.ArrayList<>()); // 复位
        }
    }

    @Autowired private WfFlowtableCompatProperties e7Compat;

    private String bpmn() { return bpmn(PROC_KEY); }

    private String bpmn(String key) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
            + "xmlns:flowable=\"http://flowable.org/bpmn\" "
            + "id=\"Definitions_" + key + "\" targetNamespace=\"http://bpmn.io/schema/bpmn\">"
            + "<bpmn:process id=\"" + key + "\" isExecutable=\"true\">"
            + "<bpmn:startEvent id=\"start\" />"
            + "<bpmn:sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"svc\" />"
            + "<bpmn:serviceTask id=\"svc\" name=\"自动节点\" "
            + "flowable:class=\"org.flowable.engine.impl.test.NoOpServiceTask\">"
            + "<bpmn:extensionElements><flowable:field name=\"name\"><flowable:string>svc</flowable:string></flowable:field></bpmn:extensionElements>"
            + "</bpmn:serviceTask>"
            + "<bpmn:sequenceFlow id=\"f2\" sourceRef=\"svc\" targetRef=\"userApprove\" />"
            + "<bpmn:userTask id=\"userApprove\" name=\"审批\" />"
            + "<bpmn:sequenceFlow id=\"f3\" sourceRef=\"userApprove\" targetRef=\"end\" />"
            + "<bpmn:endEvent id=\"end\" />"
            + "</bpmn:process></bpmn:definitions>";
    }
}
