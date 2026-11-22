package org.springblade.workflow.job;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springblade.workflow.FlowableTestConfig;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 timer 业务侧轮询集成测试（H2 + 真实 Flowable，AsyncExecutor 关闭）。
 *
 * <p>验证：部署含 timer 边界事件的流程 → 引擎创建 timer job → {@link WfTimerJobScanner#doScan()}
 * 扫描到期 job 并手动执行 → token 沿 timer 出口流转。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class WfTimerJobScannerIntegrationTest {

    private static final String KEY = "s3TimerScan";

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private ManagementService managementService;
    @Autowired private IProcessService processService;
    @Autowired private javax.sql.DataSource dataSource;

    private final Set<String> deployed = new HashSet<>();

    @Test
    void 到期timerJob_被业务扫描器驱动流转() {
        Deployment dep = repositoryService.createDeployment()
            .key(KEY).name(KEY).addString(KEY + ".bpmn20.xml", bpmn()).deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(KEY);
        assertThat(pi).isNotNull();

        // 前置：timer job 已创建且到期（PT0S）
        long before = managementService.createTimerJobQuery().processInstanceId(pi.getId()).count();
        assertThat(before).isEqualTo(1);

        // 业务扫描器驱动（H2 无 GET_LOCK，直接调 doScan）
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setS3TimerEnabled(true);
        WfTimerJobScanner scanner = new WfTimerJobScanner(
            managementService, new JdbcTemplate(dataSource), compat);
        scanner.doScan();

        // 验证流转到 timer 出口
        List<TaskVO> tasks = processService.currentTasks(pi.getId());
        assertThat(tasks).extracting(TaskVO::getTaskDefinitionKey).contains("afterTimer");
        assertThat(tasks).extracting(TaskVO::getTaskDefinitionKey).doesNotContain("wait");
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            repositoryService.deleteDeployment(depId, true);
        }
        deployed.clear();
    }

    private static String bpmn() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://flowable.org/bpmn">
              <process id="s3TimerScan" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="wait"/>
                <userTask id="wait" name="等待"/>
                <sequenceFlow id="f2" sourceRef="wait" targetRef="normalApprove"/>
                <userTask id="normalApprove" name="正常审批"/>
                <boundaryEvent id="timeoutBoundary" attachedToRef="wait" cancelActivity="true">
                  <timerEventDefinition><timeDuration>PT0S</timeDuration></timerEventDefinition>
                </boundaryEvent>
                <sequenceFlow id="f4" sourceRef="timeoutBoundary" targetRef="afterTimer"/>
                <userTask id="afterTimer" name="超时后节点"/>
                <sequenceFlow id="f5" sourceRef="afterTimer" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;
    }
}
