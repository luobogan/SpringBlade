package org.springblade.workflow.compat;

import org.junit.jupiter.api.Test;
import org.springblade.workflow.config.FlowableConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * S3 timer 与 AsyncExecutor 互斥自检单测（2026-11-18）。
 *
 * <p>两者同时开启会重复消费 ACT_RU_TIMER_JOB，导致 timer 节点被重复触发。
 * 启动自检以 log.error 强提示（不阻断启动），判据是纯函数 {@link FlowableConfig#isTimerConflict()}。</p>
 */
class S3TimerMutualExclusionTest {

    private static FlowableConfig cfg(boolean async, boolean timer) {
        FlowableConfig c = mock(FlowableConfig.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(c, "asyncExecutorEnabled", async);
        ReflectionTestUtils.setField(c, "s3TimerEnabled", timer);
        return c;
    }

    @Test
    void 两个都关_不冲突() {
        assertThat(cfg(false, false).isTimerConflict()).isFalse();
    }

    @Test
    void 只开AsyncExecutor_不冲突() {
        assertThat(cfg(true, false).isTimerConflict()).isFalse();
    }

    @Test
    void 只开S3Timer轮询_不冲突() {
        assertThat(cfg(false, true).isTimerConflict()).isFalse();
    }

    @Test
    void 两个同时开_冲突需告警() {
        assertThat(cfg(true, true).isTimerConflict()).isTrue();
    }
}
