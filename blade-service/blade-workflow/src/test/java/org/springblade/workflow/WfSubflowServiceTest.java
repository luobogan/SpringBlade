package org.springblade.workflow;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfSubflowRequest;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfSubflowRequestMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.service.helper.WfTaskActReader;
import org.springblade.workflow.service.impl.WfSubflowServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务层子流程链路单测（2026-11-18，S3 子流程按用户决策「业务层为主、补验证」）。
 *
 * <p>覆盖四个此前无单测的场景：① 归档回调（含幂等 / 未全归档等待 / 全归档汇总提醒）
 * ② autoForward 唤醒父流程 ③ 防递归（NodeActionExecutor 侧，见 WfSubflowRecursionTest）
 * ④ holdForSubflow 守卫。callActivity 保持 S0 硬拦（见 S0DeployGuardTest）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WfSubflowServiceTest {

    private static final Long MAIN_ID = 1001L;
    private static final Long SUB_ID = 2001L;
    private static final String NODE_KEY = "Task_Subflow";

    @Mock private WfSubflowRequestMapper subflowRequestMapper;
    @Mock private WfInstanceMapper instanceMapper;
    @Mock private WfTaskMapper taskMapper;
    @Mock private IWfInstanceService instanceService;
    @Mock private WfTaskActReader taskActReader;

    @InjectMocks private WfSubflowServiceImpl service;

    // ---------- ① 归档回调 ----------

    @Test
    void 归档回调_无请求记录_不抛错不推进() {
        when(subflowRequestMapper.selectOne(any())).thenReturn(null);
        service.onSubflowArchived(SUB_ID);
        verify(subflowRequestMapper, never()).updateById(any(WfSubflowRequest.class));
        verify(instanceService, never()).advance(any(), any());
    }

    @Test
    void 归档回调_已归档_幂等跳过() {
        WfSubflowRequest req = req(SUB_ID, 1); // status=1 已归档
        when(subflowRequestMapper.selectOne(any())).thenReturn(req);
        service.onSubflowArchived(SUB_ID);
        verify(subflowRequestMapper, never()).updateById(any(WfSubflowRequest.class));
        verify(instanceService, never()).advance(any(), any());
    }

    @Test
    void 归档回调_置位已归档并写归档时间() {
        WfSubflowRequest req = req(SUB_ID, 0);
        when(subflowRequestMapper.selectOne(any())).thenReturn(req);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(statusOf(req, 1)));
        service.onSubflowArchived(SUB_ID);
        assertThat(req.getStatus()).isEqualTo(1);
        assertThat(req.getArchiveTime()).isNotNull();
        verify(subflowRequestMapper).updateById(req);
    }

    @Test
    void 归档回调_未全部归档_不汇总不推进() {
        WfSubflowRequest current = req(SUB_ID, 0);
        current.setDataSummary(1);
        current.setAutoForward(1);
        WfSubflowRequest other = req(2002L, 0); // 另一子流程未归档
        when(subflowRequestMapper.selectOne(any())).thenReturn(current);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(current, other));
        service.onSubflowArchived(SUB_ID);
        verify(instanceService, never()).recordLog(any(), any(), any(), any(), any());
        verify(instanceService, never()).advance(any(), any());
    }

    @Test
    void 归档回调_全归档_数据汇总写主流程日志() {
        WfSubflowRequest current = req(SUB_ID, 0);
        current.setDataSummary(1);
        when(subflowRequestMapper.selectOne(any())).thenReturn(current);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(statusOf(current, 1)));
        WfInstance main = mainInstance(0);
        when(instanceMapper.selectById(MAIN_ID)).thenReturn(main);

        service.onSubflowArchived(SUB_ID);

        verify(instanceService).recordLog(eq(MAIN_ID), eq(NODE_KEY), eq(0L), eq(WfApprovalLog.LOG_COMMENT),
            org.mockito.ArgumentMatchers.contains("数据已汇总"));
    }

    // ---------- ② autoForward 唤醒父流程 ----------

    @Test
    void autoForward_全归档且主流程运行中_advance主流程() {
        WfSubflowRequest current = req(SUB_ID, 0);
        current.setAutoForward(1);
        when(subflowRequestMapper.selectOne(any())).thenReturn(current);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(statusOf(current, 1)));
        WfInstance main = mainInstance(WfInstance.STATUS_RUNNING);
        when(instanceMapper.selectById(MAIN_ID)).thenReturn(main);
        when(taskActReader.actRead()).thenReturn(true);
        when(taskActReader.nodeOperatorFromAct(MAIN_ID, NODE_KEY)).thenReturn(676L);

        service.onSubflowArchived(SUB_ID);

        verify(instanceService).advance(MAIN_ID, 676L);
    }

    @Test
    void autoForward_主流程已归档_不重复推进() {
        WfSubflowRequest current = req(SUB_ID, 0);
        current.setAutoForward(1);
        when(subflowRequestMapper.selectOne(any())).thenReturn(current);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(statusOf(current, 1)));
        when(instanceMapper.selectById(MAIN_ID))
            .thenReturn(mainInstance(WfInstance.STATUS_APPROVED));

        service.onSubflowArchived(SUB_ID);

        verify(instanceService, never()).advance(any(), any());
    }

    // ---------- ③ 测试态隔离（归档回调不提醒） ----------

    @Test
    void 归档回调_主流程测试态_不下发提醒() {
        WfSubflowRequest current = req(SUB_ID, 0);
        current.setRemindEnabled(1);
        current.setRemindBeforeOperator(1);
        when(subflowRequestMapper.selectOne(any())).thenReturn(current);
        when(subflowRequestMapper.selectList(any())).thenReturn(List.of(statusOf(current, 1)));
        WfInstance main = mainInstance(WfInstance.STATUS_RUNNING);
        main.setIsTest(1);
        when(instanceMapper.selectById(MAIN_ID)).thenReturn(main);

        service.onSubflowArchived(SUB_ID);

        // 只有可能的汇总日志（本例未开 dataSummary），绝无提醒日志
        verify(instanceService, never()).recordLog(eq(MAIN_ID), eq(NODE_KEY), eq(676L),
            any(), org.mockito.ArgumentMatchers.contains("请处理主流程"));
    }

    // ---------- ④ holdForSubflow 守卫 ----------

    @Test
    void holdForSubflow_入参为空_不阻塞() {
        assertThat(service.holdForSubflow(null)).isFalse();
    }

    @Test
    void holdForSubflow_有进行中且要求全归档的子流程_阻塞() {
        when(subflowRequestMapper.selectCount(any())).thenReturn(2L);
        assertThat(service.holdForSubflow(MAIN_ID)).isTrue();
    }

    @Test
    void holdForSubflow_无进行中子流程_不阻塞() {
        when(subflowRequestMapper.selectCount(any())).thenReturn(0L);
        assertThat(service.holdForSubflow(MAIN_ID)).isFalse();
    }

    // ---------- helpers ----------

    private static WfSubflowRequest req(Long subId, int status) {
        WfSubflowRequest r = new WfSubflowRequest();
        r.setMainInstId(MAIN_ID);
        r.setSubInstId(subId);
        r.setSubDefId(3001L);
        r.setMainNodeKey(NODE_KEY);
        r.setStatus(status);
        r.setIsSame(1);
        return r;
    }

    private static WfSubflowRequest statusOf(WfSubflowRequest src, int status) {
        WfSubflowRequest copy = req(src.getSubInstId(), status);
        copy.setDataSummary(src.getDataSummary());
        copy.setAutoForward(src.getAutoForward());
        copy.setRemindEnabled(src.getRemindEnabled());
        copy.setRemindBeforeOperator(src.getRemindBeforeOperator());
        copy.setRemindPersons(src.getRemindPersons());
        copy.setRemindTypes(src.getRemindTypes());
        return copy;
    }

    private static WfInstance mainInstance(int status) {
        WfInstance i = new WfInstance();
        i.setId(MAIN_ID);
        i.setStatus(status);
        i.setIsTest(0);
        return i;
    }
}
