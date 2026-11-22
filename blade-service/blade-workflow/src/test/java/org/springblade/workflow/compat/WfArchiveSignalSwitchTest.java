package org.springblade.workflow.compat;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.impl.ProcessServiceImpl;
import org.springblade.workflow.service.helper.WfCommentTenantWriter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 归档判定双轨单测：验证新增的两个引擎只读方法语义（新轨的判定基础），
 * 以及 compat.archive-signal 默认 false（旧轨安全）。
 *
 * <p>不直接测 WfInstanceServiceImpl.advanceInternal（30+ final 依赖），
 * 双轨分支本身的零回归由 e2e_freeflow_full.py 端到端保证。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfArchiveSignalSwitchTest {

    @Mock private RuntimeService runtimeService;
    @Mock private ProcessInstanceQuery processInstanceQuery;
    @Mock private ExecutionQuery executionQuery;

    @InjectMocks private ProcessServiceImpl processService;

    @Test
    void compatArchiveSignal_defaultFalse() {
        assertThat(new WfFlowtableCompatProperties().isArchiveSignal()).isFalse();
    }

    @Test
    void isProcessInstanceEnded_active实例返回false_等待态不被误判() {
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("eng-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.active()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.count()).thenReturn(1L); // 仍 active
        assertThat(processService.isProcessInstanceEnded("eng-1")).isFalse();
    }

    @Test
    void isProcessInstanceEnded_无active实例返回true() {
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId("eng-1")).thenReturn(processInstanceQuery);
        when(processInstanceQuery.active()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.count()).thenReturn(0L);
        assertThat(processService.isProcessInstanceEnded("eng-1")).isTrue();
    }

    @Test
    void activeWaitActivityId_返回等待活动ID() {
        Execution exec = mock(Execution.class);
        when(exec.getActivityId()).thenReturn("ReceiveTask_1");
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId("eng-1")).thenReturn(executionQuery);
        when(executionQuery.list()).thenReturn(List.of(exec));
        assertThat(processService.activeWaitActivityId("eng-1")).isEqualTo("ReceiveTask_1");
    }

    @Test
    void null入参安全() {
        assertThat(processService.isProcessInstanceEnded(null)).isFalse();
        assertThat(processService.activeWaitActivityId("")).isNull();
    }
}
