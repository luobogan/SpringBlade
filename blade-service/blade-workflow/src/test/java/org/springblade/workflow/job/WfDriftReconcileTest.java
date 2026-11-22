package org.springblade.workflow.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IProcessService;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S5 前置兜底：常驻漂移对账单测（2026-11-18）。
 *
 * <p>置于 {@code org.springblade.workflow.job} 包以便直调 package-private 的 {@link WfDriftReconciler#doScan()}。
 * 查询经 {@link WfDriftReconciler#queryTerminal(Date)} 抽出，测试用 spy 替换——避开纯单测环境下
 * MyBatis-Plus LambdaQueryWrapper 的 TableInfo 缓存未初始化问题（生产 Spring 容器中该缓存已就绪）。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfDriftReconcileTest {

    @Mock private IProcessService processService;
    @Mock private WfInstanceMapper instanceMapper;

    private WfDriftReconciler spyReconciler() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setDriftReconcileEnabled(true);
        return spy(new WfDriftReconciler(processService, instanceMapper, compat));
    }

    private static WfInstance inst(long id, String engineId, int status) {
        WfInstance i = new WfInstance();
        i.setId(id);
        i.setEngineInstId(engineId);
        i.setStatus(status);
        i.setEndTime(new Date());
        return i;
    }

    @Test
    void 无终态实例_不调用引擎() {
        WfDriftReconciler r = spyReconciler();
        doReturn(List.of()).when(r).queryTerminal(any());
        r.doScan();
        verify(processService, never()).deleteProcessInstance(anyString(), anyString());
    }

    @Test
    void 终态但引擎已结束_不修复() {
        WfDriftReconciler r = spyReconciler();
        doReturn(List.of(inst(1L, "pi-1", WfInstance.STATUS_APPROVED))).when(r).queryTerminal(any());
        when(processService.isProcessInstanceEnded("pi-1")).thenReturn(true);
        r.doScan();
        verify(processService, never()).deleteProcessInstance(anyString(), anyString());
    }

    @Test
    void 终态但引擎仍活跃_修复() {
        WfDriftReconciler r = spyReconciler();
        doReturn(List.of(inst(1L, "pi-1", WfInstance.STATUS_CANCELED))).when(r).queryTerminal(any());
        when(processService.isProcessInstanceEnded("pi-1")).thenReturn(false);
        r.doScan();
        verify(processService).deleteProcessInstance(eq("pi-1"), anyString());
    }

    @Test
    void 单条修复失败_不影响其余() {
        WfDriftReconciler r = spyReconciler();
        doReturn(List.of(
            inst(1L, "pi-bad", WfInstance.STATUS_REJECTED),
            inst(2L, "pi-ok", WfInstance.STATUS_APPROVED))).when(r).queryTerminal(any());
        when(processService.isProcessInstanceEnded("pi-bad")).thenReturn(false);
        when(processService.isProcessInstanceEnded("pi-ok")).thenReturn(false);
        doThrow(new RuntimeException("boom"))
            .when(processService).deleteProcessInstance(eq("pi-bad"), anyString());

        r.doScan();

        verify(processService).deleteProcessInstance(eq("pi-bad"), anyString());
        verify(processService).deleteProcessInstance(eq("pi-ok"), anyString());
    }

    @Test
    void 查询失败_本轮跳过不抛() {
        WfDriftReconciler r = spyReconciler();
        doThrow(new RuntimeException("db down")).when(r).queryTerminal(any());
        org.assertj.core.api.Assertions.assertThatCode(r::doScan)
            .doesNotThrowAnyException();
        verify(processService, never()).deleteProcessInstance(anyString(), anyString());
    }
}
