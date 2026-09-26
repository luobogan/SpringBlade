package org.springblade.workflow.service.helper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.service.IProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 工作流「双写收口器」——生命周期类操作的唯一写入口（方案 A）。
 *
 * <p>背景：{@code wf_*} 业务台账与 {@code ACT_*} 引擎表是两套数据。凡改变实例
 * <b>生命周期状态</b>的操作（终结 / 暂停 / 恢复），必须<b>同时</b>写两侧，
 * 只写一侧即产生漂移（历史 P0：撤销/撤回/终止只改 {@code wf_instance}，引擎里
 * {@code ACT_RU_EXECUTION} 仍挂着运行态 → 业务已终态、引擎仍运行）。</p>
 *
 * <p>本类把「台账写 + 引擎写」封装成<b>原子的一组动作</b>：调用方只需表达意图
 * （{@link #terminate} / {@link #suspend} / {@link #activate}），无法再「只写一边」——
 * 引擎调用是方法内的固定步骤，不是可选参数，从结构上杜绝漏写。</p>
 *
 * <p><b>事务</b>：本类无 {@code @Transactional}（与 {@link IProcessService} 同策略），
 * 由调用方的 {@code @Transactional} 提供事务；引擎调用以 {@code REQUIRED} 加入同一事务，
 * 前提是 {@code ACT_*} 与 {@code wf_*} 共用同一 DataSource（见治理文档 §4）。</p>
 *
 * <p><b>不做的事</b>：不含鉴权 / 测试态守卫等业务规则（仍在 Service 层）；
 * 不覆盖 OA 独占写（协办/抄送/传阅、草稿、审批轨迹、表单快照仍由业务代码显式写）——
 * 那些在引擎里没有对应物，本就不存在「两次写」。</p>
 *
 * @see org.springblade.workflow.service.impl.WfInstanceServiceImpl
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfWriteHelper {

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfApprovalLogMapper logMapper;
    private final IProcessService processService;

    /**
     * 审批轨迹下沉开关（迁移阶段2「双写校验」）：开启后 {@link #appendLog} 在写 wf_approval_log 的同时，
     * 把意见同步到引擎 {@code ACT_HI_COMMENT}（taskService.addComment）。默认 false。
     * <p>读侧暂未切换（全模块约 28+ 处读取仍走 wf_approval_log），故开启本开关仅做<b>双写预热</b>，
     * 不影响现有读路径；读侧切到 {@code HistoryService.createCommentQuery} 需运行时回归，列为后续阶段。</p>
     */
    @Value("${blade.workflow.approval-comment.enabled:false}")
    private boolean approvalCommentEnabled;

    /**
     * 终结实例（撤销 / 撤回 / 不通过等终态）：台账置终态 + 关待办 + 流转日志 + 引擎删实例。
     *
     * <p>四步同事务，顺序与收敛前一致：先台账、后引擎（引擎调用为「尽力而为」，
     * 实例已不存在时 {@link IProcessService#deleteProcessInstance} 内部吞异常，不阻断业务）。</p>
     *
     * @param inst    已加载的实例（调用方已完成鉴权与测试态守卫）
     * @param status  目标终态（{@link WfInstance#STATUS_APPROVED} / {@link WfInstance#STATUS_REJECTED} / {@link WfInstance#STATUS_CANCELED}）
     * @param opinion 意见（可为空）
     * @param action  动作名，用于日志与引擎删除原因
     * @return 恒 true（与收敛前语义一致）
     */
    public boolean terminate(WfInstance inst, int status, String opinion, String action) {
        inst.setStatus(status);
        inst.setEndTime(new Date());
        instanceMapper.updateById(inst);

        // 关闭所有未完成任务（含协办/征询待办：实例终止时一并清掉，避免孤儿待办）
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .in(WfTask::getStatus, WfTask.STATUS_TODO, WfTask.STATUS_COADJUTANT));
        for (WfTask t : tasks) {
            t.setStatus(WfTask.STATUS_FINISHED);
            t.setOperateTime(new Date());
            taskMapper.updateById(t);
        }
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, action + "：" + (opinion == null ? "" : opinion));

        // 同步终结引擎运行态实例（消除 ACT_RU_* 孤儿漂移：撤销/撤回后引擎不再持有该流程）
        processService.deleteProcessInstance(inst.getEngineInstId(), action);
        return true;
    }

    /**
     * 暂停实例：台账置 {@link WfInstance#STATUS_SUSPENDED} + 流转日志 + 引擎挂起实例。
     */
    public void suspend(WfInstance inst) {
        inst.setStatus(WfInstance.STATUS_SUSPENDED);
        instanceMapper.updateById(inst);
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "暂停流程");
        // 同步挂起引擎运行态实例，使引擎与 wf_instance.status=已暂停 对齐
        processService.suspendProcessInstance(inst.getEngineInstId());
    }

    /**
     * 恢复实例：台账置 {@link WfInstance#STATUS_RUNNING} + 流转日志 + 引擎激活实例。
     */
    public void activate(WfInstance inst) {
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.updateById(inst);
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "恢复流程");
        // 同步激活引擎运行态实例，使引擎与 wf_instance.status=运行中 对齐
        processService.activateProcessInstance(inst.getEngineInstId());
    }

    /**
     * 追加流转记录（全模块唯一实现：Service 层的 {@code appendLog} 委托至此，避免同名逻辑分散）。
     */
    public void appendLog(Long instId, Long taskId, String nodeKey, Long operator,
                          String logType, String opinion) {
        WfApprovalLog log = new WfApprovalLog();
        log.setInstId(instId);
        log.setTaskId(taskId);
        log.setNodeKey(nodeKey == null ? "" : nodeKey);
        log.setOperator(operator);
        log.setLogType(logType);
        log.setOpinion(opinion == null ? "" : opinion);
        log.setOperateTime(new Date());
        logMapper.insert(log);
        if (approvalCommentEnabled) {
            syncCommentToEngine(instId, taskId, logType, opinion);
        }
    }

    /**
     * 双写引擎审批意见（开关开启时调用）。把 wf_* 主键解析为引擎 id 后调用 addComment，
     * 失败仅记日志、不阻断业务（引擎意见是预热，不影响台账权威）。
     */
    private void syncCommentToEngine(Long instId, Long wfTaskId, String logType, String opinion) {
        try {
            WfInstance inst = instanceMapper.selectById(instId);
            if (inst == null || inst.getEngineInstId() == null) {
                return;
            }
            String procInstId = inst.getEngineInstId();
            String engineTaskId = null;
            if (wfTaskId != null) {
                WfTask wfTask = taskMapper.selectById(wfTaskId);
                if (wfTask != null) {
                    engineTaskId = wfTask.getEngineTaskId();
                }
            }
            processService.addComment(engineTaskId, procInstId, logType, opinion == null ? "" : opinion);
        } catch (Exception e) {
            log.warn("[WfWriteHelper] 审批意见双写引擎失败（忽略，不影响台账）: {}", e.getMessage());
        }
    }
}
