package org.springblade.workflow.compat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.listener.WfEngineAutoTodoBridgeListener;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.service.IWfInstanceService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * E7 引擎→业务状态桥 listener 单测：验证 activityType 过滤、灰度白名单、防重入与失败不传播。
 */
@ExtendWith(MockitoExtension.class)
class WfEngineAutoTodoBridgeListenerTest {

    @Mock private WfFlowtableCompatProperties compat;
    @Mock private WfInstanceMapper instanceMapper;
    @Mock private IWfInstanceService instanceService;
    @Mock private ObjectProvider<IWfInstanceService> instanceServiceProvider;
    @Mock private FlowableActivityEvent activityEvent;

    private WfEngineAutoTodoBridgeListener listener() {
        // lenient: 多数早退分支（事件类型/activityType/灰度过滤）走不到 getObject()，stub 不强制使用
        org.mockito.Mockito.lenient().when(instanceServiceProvider.getObject()).thenReturn(instanceService);
        return new WfEngineAutoTodoBridgeListener(compat, instanceMapper, instanceServiceProvider);
    }

    private WfInstance inst(Long defId) {
        WfInstance i = new WfInstance();
        i.setId(100L);
        i.setDefId(defId);
        i.setStarter(676L);
        return i;
    }

    private void givenAutoActivity(String type, String procInstId) {
        when(activityEvent.getType()).thenReturn(FlowableEngineEventType.ACTIVITY_COMPLETED);
        when(activityEvent.getActivityType()).thenReturn(type);
        when(activityEvent.getProcessInstanceId()).thenReturn(procInstId);
        // getActivityId() 仅用于日志，留空返回 null 即可，避免早退用例的 UnnecessaryStubbing
    }

    @Test
    void serviceTask完成_灰度空_触发advance() {
        givenAutoActivity("serviceTask", "eng-1");
        when(compat.getEngineAutoTodoGrayDefIds()).thenReturn(List.of());
        when(instanceMapper.selectOne(any())).thenReturn(inst(200L));

        listener().onEvent(activityEvent);

        // 用 5 参重载：instId, starter, null, null, SUBMIT
        verify(instanceService).advance(eq(100L), eq(676L), isNull(), isNull(), eq(AdvanceSrc.SUBMIT));
    }

    @Test
    void userTask完成_不触发_由业务链路own() {
        // 只 stub 到 activityType 过滤点即可（listener 在类型不匹配时提前 return）
        when(activityEvent.getType()).thenReturn(FlowableEngineEventType.ACTIVITY_COMPLETED);
        when(activityEvent.getActivityType()).thenReturn("userTask");
        listener().onEvent(activityEvent);
        verify(instanceService, never()).advance(any(), any(), any(), any(), any());
    }

    @Test
    void 灰度名单外_不触发() {
        givenAutoActivity("serviceTask", "eng-3");
        when(compat.getEngineAutoTodoGrayDefIds()).thenReturn(List.of(999L));
        when(instanceMapper.selectOne(any())).thenReturn(inst(200L));

        listener().onEvent(activityEvent);

        verify(instanceService, never()).advance(any(), any(), any(), any(), any());
    }

    @Test
    void 灰度名单内_触发() {
        givenAutoActivity("scriptTask", "eng-4");
        when(compat.getEngineAutoTodoGrayDefIds()).thenReturn(List.of(200L));
        when(instanceMapper.selectOne(any())).thenReturn(inst(200L));

        listener().onEvent(activityEvent);

        verify(instanceService).advance(eq(100L), eq(676L), isNull(), isNull(), eq(AdvanceSrc.SUBMIT));
    }

    @Test
    void 非ACTIVITY_COMPLETED事件_忽略() {
        when(activityEvent.getType()).thenReturn(FlowableEngineEventType.TASK_CREATED);
        listener().onEvent(activityEvent);
        verify(instanceService, never()).advance(any(), any(), any(), any(), any());
    }

    @Test
    void 失败策略恒false_不回滚引擎() {
        org.assertj.core.api.Assertions.assertThat(listener().isFailOnException()).isFalse();
    }
}
