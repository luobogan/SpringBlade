package org.springblade.workflow;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 timer 技术前提验证（H2 内存库 + 真实 Flowable 引擎，AsyncExecutor 关闭）。
 *
 * <p>验证：① 流程进入 timer 边界/捕获节点时是否创建 timer job（即使 AsyncExecutor 关闭）；
 * ② 业务侧通过 {@code managementService.executeJob(jobId)} 能否手动驱动 timer 流转。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FlowableTestConfig.class)
class S3TimerJobProbeTest {

    private static final String KEY = "s3TimerProbe";
    private static final String BOUNDARY_KEY = "s3BoundaryProbe";

    @Autowired private RepositoryService repositoryService;
    @Autowired private RuntimeService runtimeService;
    @Autowired private ManagementService managementService;
    @Autowired private org.springblade.workflow.service.IProcessService processService;

    private final Set<String> deployed = new HashSet<>();

    @Test
    void timer边界_AsyncExecutor关闭时_job仍能被创建并手动执行() {
        Deployment dep = repositoryService.createDeployment()
            .key(BOUNDARY_KEY).name(BOUNDARY_KEY)
            .addString(BOUNDARY_KEY + ".bpmn20.xml", bpmnWithBoundaryTimer())
            .deploy();
        deployed.add(dep.getId());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(BOUNDARY_KEY);
        assertThat(pi).isNotNull();

        // ① timer job 是否被创建（即使 AsyncExecutor 关闭）
        List<org.flowable.job.api.Job> timerJobs = managementService.createTimerJobQuery().processInstanceId(pi.getId()).list();
        System.out.println("[PROBE] timerJobs after start = " + timerJobs.size());
        for (org.flowable.job.api.Job j : timerJobs) {
            System.out.println("[PROBE] jobId=" + j.getId() + " retries=" + j.getRetries());
        }
        assertThat(timerJobs).hasSize(1);

        // ② 业务侧手动触发 timer job（绕开 AsyncExecutor）：
        //    timer job 存 ACT_RU_TIMER_JOB，executeJob 只认 ACT_RU_JOB，须先 moveTimerToExecutableJob 迁移
        String timerJobId = timerJobs.get(0).getId();
        org.flowable.job.api.Job executable = managementService.moveTimerToExecutableJob(timerJobId);
        System.out.println("[PROBE] moved to executable jobId=" + executable.getId());
        managementService.executeJob(executable.getId());

        // ③ 验证流转到了 timer 出口（超时路径）而非正常审批路径
        List<org.springblade.workflow.vo.TaskVO> tasks = processService.currentTasks(pi.getId());
        System.out.println("[PROBE] tasks after executeJob = " + tasks.stream()
            .map(org.springblade.workflow.vo.TaskVO::getTaskDefinitionKey).toList());
        assertThat(tasks).extracting(org.springblade.workflow.vo.TaskVO::getTaskDefinitionKey)
            .contains("afterTimer");
    }

    @AfterEach
    void cleanup() {
        for (String depId : deployed) {
            repositoryService.deleteDeployment(depId, true);
        }
        deployed.clear();
    }

    private static String bpmnWithBoundaryTimer() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://flowable.org/bpmn">
              <process id="s3BoundaryProbe" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="wait"/>
                <userTask id="wait" name="等待"/>
                <sequenceFlow id="f2" sourceRef="wait" targetRef="normalApprove"/>
                <userTask id="normalApprove" name="正常审批"/>
                <sequenceFlow id="f3" sourceRef="normalApprove" targetRef="end"/>
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
