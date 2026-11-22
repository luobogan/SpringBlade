package org.springblade.workflow.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springblade.core.tool.api.R;
import org.springblade.message.dto.NoticeSendDTO;
import org.springblade.message.feign.INoticeClient;
import org.springblade.workflow.config.WfFlowtableCompatProperties;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.vo.DeadLetterJobVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S5 前置兜底 deadletter 告警单测：验证死信非空时 log.error + 通知投递、空时不告警、
 * 未配置接收人时只记日志不投递、通知失败不影响主流程。
 *
 * <p>置于 {@code org.springblade.workflow.job} 包以便直调 package-private 的
 * {@link WfDeadLetterJobScanner#doScan()}（跳过 H2 无 GET_LOCK 的选主）。</p>
 */
@ExtendWith(MockitoExtension.class)
class WfDeadLetterAlarmTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private IProcessService processService;
    @Mock private INoticeClient noticeClient;

    private WfDeadLetterJobScanner scanner(String userIds) {
        WfFlowtableCompatProperties compat = new WfFlowtableCompatProperties();
        compat.setDeadletterAlarmEnabled(true);
        WfDeadLetterJobScanner s = new WfDeadLetterJobScanner(jdbcTemplate, processService, noticeClient, compat);
        ReflectionTestUtils.setField(s, "alarmUserIds", userIds);
        return s;
    }

    private static DeadLetterJobVO deadLetter(String id) {
        DeadLetterJobVO vo = new DeadLetterJobVO();
        vo.setJobId(id);
        vo.setProcessInstanceId("pi-1");
        vo.setExceptionMessage("boom");
        return vo;
    }

    @Test
    void 无死信_不投递通知() {
        when(processService.listDeadLetterJobs()).thenReturn(List.of());
        scanner("676").doScan();
        verify(noticeClient, never()).sendToUsers(any());
    }

    @Test
    void 有死信且配置了接收人_投递告警通知() {
        when(processService.listDeadLetterJobs()).thenReturn(List.of(deadLetter("job-1"), deadLetter("job-2")));
        when(noticeClient.sendToUsers(any())).thenReturn(R.data(true));
        scanner("676,1").doScan();
        verify(noticeClient).sendToUsers(any(NoticeSendDTO.class));
    }

    @Test
    void 有死信但未配置接收人_只记日志不投递() {
        when(processService.listDeadLetterJobs()).thenReturn(List.of(deadLetter("job-1")));
        scanner("").doScan();
        verify(noticeClient, never()).sendToUsers(any());
    }

    @Test
    void 查询失败_本轮跳过不投递() {
        when(processService.listDeadLetterJobs()).thenThrow(new RuntimeException("db down"));
        scanner("676").doScan();
        verify(noticeClient, never()).sendToUsers(any());
    }

    @Test
    void 通知投递失败_不影响主流程() {
        when(processService.listDeadLetterJobs()).thenReturn(List.of(deadLetter("job-1")));
        when(noticeClient.sendToUsers(any())).thenReturn(R.fail("投递失败"));
        scanner("676").doScan();
        verify(noticeClient).sendToUsers(any());
    }

    @Test
    void 接收人配置非法_不投递() {
        when(processService.listDeadLetterJobs()).thenReturn(List.of(deadLetter("job-1")));
        scanner("abc").doScan();
        verify(noticeClient, never()).sendToUsers(any());
    }
}
