package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.impl.WfWakeupServiceImpl;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S4 通用唤醒 H2 集成测试（真实 Flowable 8.1 引擎）：
 * 验证 ReceiveTask.trigger 与 messageEventReceived 能把等待态流程推进到下一节点。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfWakeupServiceIntegrationTest {

    private static final String RCV_KEY = "s4ReceiveProbe";
    private static final String MSG_KEY = "s4MessageProbe";

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private IProcessService processService;
    @Autowired private DataSource dataSource;

    private final Set<String> deployed = new HashSet<>();
    private WfWakeupServiceImpl wakeup;

    @BeforeEach
    void setUp() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setS4WakeupEnabled(true);
        wakeup = new WfWakeupServiceImpl(runtimeService, new JdbcTemplate(dataSource), compat);
    }

    @Test
    void receiveTask_trigger_推进到下一节点() {
        Deployment dep = repositoryService.createDeployment()
            .key(RCV_KEY).name(RCV_KEY).addString(RCV_KEY + ".bpmn20.xml", receiveBpmn()).deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(RCV_KEY);
        assertThat(pi).isNotNull();
        // ReceiveTask 等待：无 UserTask 待办
        assertThat(processService.currentTasks(pi.getId())).isEmpty();

        boolean ok = wakeup.triggerReceive(pi.getId(), "waitReceive", null);
        assertThat(ok).isTrue();

        List<TaskVO> tasks = processService.currentTasks(pi.getId());
        assertThat(tasks).extracting(TaskVO::getTaskDefinitionKey).containsExactly("afterWake");
    }

    @Test
    void messageEventReceived_推进到下一节点() {
        Deployment dep = repositoryService.createDeployment()
            .key(MSG_KEY).name(MSG_KEY).addString(MSG_KEY + ".bpmn20.xml", messageBpmn()).deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(MSG_KEY);
        assertThat(pi).isNotNull();
        assertThat(processService.currentTasks(pi.getId())).isEmpty();

        // message catch event 的元素 id = waitMessage；用它作为 messageName 定位等待 execution
        boolean ok = wakeup.signalMessage(pi.getId(), "waitMessage", null);
        assertThat(ok).isTrue();

        List<TaskVO> tasks = processService.currentTasks(pi.getId());
        assertThat(tasks).extracting(TaskVO::getTaskDefinitionKey).containsExactly("afterWake");
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            repositoryService.deleteDeployment(depId, true);
        }
        deployed.clear();
    }

    private static String receiveBpmn() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://flowable.org/bpmn">
              <process id="s4ReceiveProbe" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="waitReceive"/>
                <receiveTask id="waitReceive" name="等待"/>
                <sequenceFlow id="f2" sourceRef="waitReceive" targetRef="afterWake"/>
                <userTask id="afterWake" name="唤醒后审批"/>
                <sequenceFlow id="f3" sourceRef="afterWake" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;
    }

    private static String messageBpmn() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://flowable.org/bpmn">
              <message id="msgDef" name="waitMessage"/>
              <process id="s4MessageProbe" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="waitMessage"/>
                <intermediateCatchEvent id="waitMessage" name="消息等待">
                  <messageEventDefinition messageRef="msgDef"/>
                </intermediateCatchEvent>
                <sequenceFlow id="f2" sourceRef="waitMessage" targetRef="afterWake"/>
                <userTask id="afterWake" name="唤醒后审批"/>
                <sequenceFlow id="f3" sourceRef="afterWake" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;
    }
}
