package org.springblade.workflow.compat;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.service.impl.ProcessServiceImpl;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * deadletter 三件套之「查询入口 + 重投/删除」单测：验证 ProcessServiceImpl 的薄封装委托正确。
 */
@SuppressWarnings("unchecked")
class WfDeadLetterJobAccessTest {

    @Test
    void listDeadLetterJobs_空列表() {
        ManagementService ms = mock(ManagementService.class);
        DeadLetterJobQuery q = mock(DeadLetterJobQuery.class);
        when(ms.createDeadLetterJobQuery()).thenReturn(q);
        when(q.list()).thenReturn(List.of());
        ProcessServiceImpl svc = new ProcessServiceImpl(
            mock(RuntimeService.class), mock(TaskService.class), mock(RepositoryService.class),
            mock(HistoryService.class), ms, mock(ObjectProvider.class));
        assertThat(svc.listDeadLetterJobs()).isEmpty();
    }

    @Test
    void retryDeadLetterJob_委托moveDeadLetterJobToExecutableJob() {
        ManagementService ms = mock(ManagementService.class);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn("exec-1");
        when(ms.moveDeadLetterJobToExecutableJob("dl-1", 3)).thenReturn(job);
        ProcessServiceImpl svc = new ProcessServiceImpl(
            mock(RuntimeService.class), mock(TaskService.class), mock(RepositoryService.class),
            mock(HistoryService.class), ms, mock(ObjectProvider.class));
        assertThat(svc.retryDeadLetterJob("dl-1", 3)).isEqualTo("exec-1");
        verify(ms).moveDeadLetterJobToExecutableJob("dl-1", 3);
    }

    @Test
    void deleteDeadLetterJob_委托deleteDeadLetterJob() {
        ManagementService ms = mock(ManagementService.class);
        ProcessServiceImpl svc = new ProcessServiceImpl(
            mock(RuntimeService.class), mock(TaskService.class), mock(RepositoryService.class),
            mock(HistoryService.class), ms, mock(ObjectProvider.class));
        svc.deleteDeadLetterJob("dl-1");
        verify(ms).deleteDeadLetterJob("dl-1");
    }
}
