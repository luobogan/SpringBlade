package org.springblade.workflow.compat;

import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.listener.WfEngineEventListener;
import org.springblade.workflow.service.helper.WfStateProjector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 事件失败策略单测：验证 strict-event-fail 仅作用于台账投影 listener，默认 false（旧行为）。
 */
class WfEngineEventListenerFailStrategyTest {

    @Test
    void defaultSwitch_false_最终一致() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        WfEngineEventListener listener = new WfEngineEventListener(mock(WfStateProjector.class), compat);
        assertThat(listener.isFailOnException()).isFalse();
    }

    @Test
    void strictEventFail_true_C1强一致() {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setStrictEventFail(true);
        WfEngineEventListener listener = new WfEngineEventListener(mock(WfStateProjector.class), compat);
        assertThat(listener.isFailOnException()).isTrue();
    }
}
