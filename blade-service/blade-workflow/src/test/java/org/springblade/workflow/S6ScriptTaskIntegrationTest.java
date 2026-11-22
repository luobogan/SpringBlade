package org.springblade.workflow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S6 ScriptTask 引擎级集成测试（H2 + 真实 Flowable，沙箱开启）。
 *
 * <p>验证：BPMN 含 scriptFormat="groovy" 的 ScriptTask → 引擎用沙箱引擎执行脚本 →
 * 结果写入流程变量并流转到下一节点；危险脚本（System.exit）被引擎执行时抛异常、流程不流转。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
@TestPropertySource(properties = "blade.workflow.compat.s6-script-task-enabled=true")
class S6ScriptTaskIntegrationTest {

    private static final String KEY_OK = "s6ScriptOk";
    private static final String KEY_BAD = "s6ScriptBad";

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private IProcessService processService;

    private final Set<String> deployed = new HashSet<>();

    @Test
    void 合法Groovy脚本_引擎执行并写入结果变量() {
        Deployment dep = repositoryService.createDeployment()
            .key(KEY_OK).name(KEY_OK)
            .addString(KEY_OK + ".bpmn20.xml", bpmn(KEY_OK, "calculated = 6 * 7"))
            .deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(KEY_OK);
        assertThat(pi).isNotNull();

        Object value = runtimeService.getVariable(pi.getId(), "calculated");
        assertThat(value).isEqualTo(42);

        List<TaskVO> tasks = processService.currentTasks(pi.getId());
        assertThat(tasks).extracting(TaskVO::getTaskDefinitionKey).contains("afterScript");
    }

    @Test
    void 危险Groovy脚本_引擎执行被沙箱拒绝() {
        Deployment dep = repositoryService.createDeployment()
            .key(KEY_BAD).name(KEY_BAD)
            .addString(KEY_BAD + ".bpmn20.xml", bpmn(KEY_BAD, "System.exit(1)"))
            .deploy();
        deployed.add(dep.getId());

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(KEY_BAD))
            .hasMessageContaining("Script");
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            repositoryService.deleteDeployment(depId, true);
        }
        deployed.clear();
    }

    private static String bpmn(String key, String script) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://flowable.org/bpmn">
              <process id="%s" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="scriptTask"/>
                <scriptTask id="scriptTask" name="脚本" scriptFormat="groovy" flowable:resultVariable="calculated">
                  <script><![CDATA[%s]]></script>
                </scriptTask>
                <sequenceFlow id="f2" sourceRef="scriptTask" targetRef="afterScript"/>
                <userTask id="afterScript" name="脚本后节点"/>
                <sequenceFlow id="f3" sourceRef="afterScript" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """.formatted(key, script);
    }
}
