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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** 任务业务列双写收口器（去 wf_ 表写侧，方案 A1：扩展 ACT_RU_TASK/ACT_HI_TASKINST） */
    private final WfTaskActWriter taskActWriter;
    private final IProcessService processService;
    /** 去 wf_ 表预热：生命周期状态同步写回原生 ACT_HI_PROCINST（开关关闭时无操作） */
    private final WfInstanceActWriter actWriter;

    /**
     * 审批轨迹下沉开关（迁移阶段2「双写校验」）：开启后 {@link #appendLog} 在写 wf_approval_log 的同时，
     * 把意见同步到引擎 {@code ACT_HI_COMMENT}（taskService.addComment）。默认 false。
     * <p>读侧暂未切换（全模块约 28+ 处读取仍走 wf_approval_log），故本开关当前仅做<b>双写预热</b>，
     * 不影响现有读路径；读侧切到 {@code HistoryService.createCommentQuery} 需运行时回归，列为后续阶段（P3-4/P6）。</p>
     *
     * <p><b>P3-4 默认开启</b>：写侧以 {@code ACT_HI_COMMENT} 为权威存储（补齐 nodeKey/operator 维度，见 {@link #syncCommentToEngine}），
     * 现有读路径仍走 {@code wf_approval_log}，双写期间两表并存、零漂移；读侧切换与 {@code wf_approval_log} 停写随 P6 退役完成。</p>
     */
    @Value("${blade.workflow.approval-comment.enabled:true}")
    private boolean approvalCommentEnabled;

    private static final ObjectMapper COMMENT_MAPPER = new ObjectMapper();

    /**
     * 方案C 台账事件反写开关 —— 与 {@code WfEngineEventListener} 的
     * {@code @ConditionalOnProperty(name="blade.workflow.ledger-listener.enabled", havingValue="true", matchIfMissing=false)}
     * <b>同源</b>，两处判断不会不一致。
     *
     * <p>{@code true}（监听在）→ 本类<b>不写</b> {@code wf_instance.status} / {@code end_time}，
     * 也不手工关待办，改由 {@code ENTITY_SUSPENDED} / {@code ENTITY_ACTIVATED} / {@code PROCESS_CANCELLED}
     * 事件经 {@code WfStateProjector} 反写（单一写入源）。</p>
     *
     * <p>{@code false}（监听不在）→ 保留原显式写，回退方案A。</p>
     *
     * <p><b>为什么必须保留这个兜底</b>：治理文档 §7 承诺「关闭开关即回方案A 显式双写」。若直接删除业务写，
     * 关掉监听开关后 {@code wf_instance.status} 将<b>完全不被更新</b>（引擎挂起/激活照常发生），
     * 暂停/恢复/终止会<b>静默失效</b> —— 等于埋了个只在关开关时才爆的雷。</p>
     */
    @Value("${blade.workflow.ledger-listener.enabled:false}")
    private boolean ledgerListenerEnabled;

    /**
     * 终结实例（撤销 / 撤回 / 不通过等终态）：终态 + 关待办 + 流转日志 + 引擎删实例。
     *
     * <p><b>写入源（方案C 阶段3）</b>：
     * <ul>
     *   <li>{@code ledgerListenerEnabled=true} 且引擎实例存在 → 状态/关待办<b>不再由本类写</b>，
     *       改由 {@code PROCESS_CANCELLED} 事件经 {@code WfStateProjector} 反写；
     *       本类改为在调引擎<b>之前</b>写入 {@code pending_status} intent，告诉事件派生哪个终态 ——
     *       该事件只有「取消」语义，分不清「不通过(2)」与「撤销(3)」。</li>
     *   <li>否则（开关关闭，或 {@code engineInstId} 为空导致不会派发事件）→ 沿用原显式写兜底。</li>
     * </ul>
     * </p>
     *
     * @param inst    已加载的实例（调用方已完成鉴权与测试态守卫）
     * @param status  目标终态（{@link WfInstance#STATUS_APPROVED} / {@link WfInstance#STATUS_REJECTED} / {@link WfInstance#STATUS_CANCELED}）
     * @param opinion 意见（可为空）
     * @param action  动作名，用于日志与引擎删除原因
     * @return 恒 true（与收敛前语义一致）
     */
    public boolean terminate(WfInstance inst, int status, String opinion, String action) {
        boolean eventDriven = eventDriven(inst);
        if (eventDriven) {
            // ① 先落 intent（必须在调引擎之前，否则事件到达时读不到）
            markPendingStatus(inst.getId(), status);
        } else {
            // 兜底：开关关闭，或 engineInstId 为空（引擎不会派发事件）→ 显式写
            inst.setStatus(status);
            inst.setEndTime(new Date());
            instanceMapper.updateById(inst);
            closePendingTasks(inst.getId());
        }
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, action + "：" + (opinion == null ? "" : opinion));

        // 同步终结引擎运行态实例（必留：这是产生 PROCESS_CANCELLED 事件的动作本身）
        processService.deleteProcessInstance(inst.getEngineInstId(), action);
        // 去 wf_ 表预热：终态写回 ACT_HI_PROCINST（开关关闭时无操作）
        actWriter.writeLifecycle(inst.getEngineInstId(), status, new Date());
        if (eventDriven) {
            // ② 兜底校验：事件反写若未生效（如监听异常被引擎侧吞掉），补写并关待办
            ensureApplied(inst.getId(), status, true);
        }
        return true;
    }

    /**
     * 暂停实例：{@code status=暂停(4)} + 流转日志 + 引擎挂起实例（触发 {@code ENTITY_SUSPENDED}）。
     *
     * <p>开关开启且引擎实例存在时，状态改由事件反写（写入源说明见 {@link #terminate}）。</p>
     */
    public void suspend(WfInstance inst) {
        boolean eventDriven = eventDriven(inst);
        if (!eventDriven) {
            inst.setStatus(WfInstance.STATUS_SUSPENDED);
            instanceMapper.updateById(inst);
        }
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "暂停流程");
        // 同步挂起引擎运行态实例（必留：这是产生 ENTITY_SUSPENDED 事件的动作本身）
        processService.suspendProcessInstance(inst.getEngineInstId());
        // 去 wf_ 表预热：暂停态写回 ACT_HI_PROCINST（开关关闭时无操作）
        actWriter.writeLifecycle(inst.getEngineInstId(), WfInstance.STATUS_SUSPENDED, null);
        if (eventDriven) {
            ensureApplied(inst.getId(), WfInstance.STATUS_SUSPENDED, false);
        }
    }

    /**
     * 恢复实例：{@code status=运行中(0)} + 流转日志 + 引擎激活实例（触发 {@code ENTITY_ACTIVATED}）。
     *
     * <p>开关开启且引擎实例存在时，状态改由事件反写（写入源说明见 {@link #terminate}）。</p>
     */
    public void activate(WfInstance inst) {
        boolean eventDriven = eventDriven(inst);
        if (!eventDriven) {
            inst.setStatus(WfInstance.STATUS_RUNNING);
            instanceMapper.updateById(inst);
        }
        appendLog(inst.getId(), null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "恢复流程");
        // 同步激活引擎运行态实例（必留：这是产生 ENTITY_ACTIVATED 事件的动作本身）
        processService.activateProcessInstance(inst.getEngineInstId());
        // 去 wf_ 表预热：恢复运行态写回 ACT_HI_PROCINST（开关关闭时无操作）
        actWriter.writeLifecycle(inst.getEngineInstId(), WfInstance.STATUS_RUNNING, null);
        if (eventDriven) {
            ensureApplied(inst.getId(), WfInstance.STATUS_RUNNING, false);
        }
    }

    // ---------------- 方案C 阶段3 支撑方法 ----------------

    /**
     * 是否走「事件驱动」写入：开关开启 <b>且</b> 引擎实例ID 非空。
     *
     * <p>{@code engineInstId} 为空/空白时 {@code ProcessServiceImpl} 会直接 return、
     * <b>不派发任何事件</b>（见 {@code ProcessServiceImpl:303-305}），此时必须走显式写兜底，
     * 否则实例会永久停留在「运行中」。</p>
     */
    private boolean eventDriven(WfInstance inst) {
        String engineInstId = inst == null ? null : inst.getEngineInstId();
        return ledgerListenerEnabled && engineInstId != null && !engineInstId.isBlank();
    }

    /** 写入期望终态 intent（{@code PROCESS_CANCELLED} 据此派生 2不通过 / 3撤销） */
    private void markPendingStatus(Long instId, int status) {
        WfInstance patch = new WfInstance();
        patch.setId(instId);
        patch.setPendingStatus(status);
        instanceMapper.updateById(patch);
    }

    /**
     * 兜底校验：事件驱动下若引擎事件未把台账改到目标态（监听异常被引擎侧吞掉等），补写之，
     * 避免台账<b>静默</b>停留在旧状态 —— 这是事件驱动相比显式写最主要的风险。
     */
    private void ensureApplied(Long instId, int targetStatus, boolean terminal) {
        WfInstance cur = instanceMapper.selectById(instId);
        if (cur == null) {
            return;
        }
        if (cur.getStatus() == null || cur.getStatus() != targetStatus) {
            log.warn("[WfWriteHelper] 引擎事件未反写目标态，兜底补写. instId={}, target={}, actual={}",
                instId, targetStatus, cur.getStatus());
            WfInstance patch = new WfInstance();
            patch.setId(instId);
            patch.setStatus(targetStatus);
            if (terminal) {
                patch.setEndTime(new Date());
            }
            instanceMapper.updateById(patch);
            if (terminal) {
                closePendingTasks(instId);
            }
        }
        if (cur.getPendingStatus() != null) {
            // 必须用 UpdateWrapper 显式置 NULL：updateById 只更新非 null 字段，
            // 传全 null 实体会拼出无 SET 子句的 UPDATE → SQL 语法错误（详见 WfStateProjector#clearPendingStatus）
            instanceMapper.update(null, Wrappers.<WfInstance>lambdaUpdate()
                .setSql("pending_status = NULL")
                .eq(WfInstance::getId, instId));
        }
    }

    /** 关闭实例上所有未完成任务（含协办/征询待办，避免孤儿待办） */
    private void closePendingTasks(Long instId) {
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .in(WfTask::getStatus, WfTask.STATUS_TODO, WfTask.STATUS_COADJUTANT));
        for (WfTask t : tasks) {
            t.setStatus(WfTask.STATUS_FINISHED);
            t.setOperateTime(new Date());
            taskMapper.updateById(t);
            // 关闭实例残留待办 → 子状态 FINISHED 同步到 ACT_*
            taskActWriter.sync(t);
        }
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
            syncCommentToEngine(instId, taskId, nodeKey, operator, logType, opinion);
        }
    }

    /**
     * 双写引擎审批意见（开关开启时调用）。把 wf_* 主键解析为引擎 id 后调用 addComment，
     * 失败仅记日志、不阻断业务（引擎意见是预热，不影响台账权威）。
     *
     * <p><b>维度补齐（P3-4）</b>：{@code ACT_HI_COMMENT} 原生没有 {@code nodeKey} 列，而审批日志 UI 的
     * 「可见节点过滤 / 下一节点接收人」都依赖 {@code nodeKey}。故将 {@code nodeKey/operator/wfTaskId/opinion/ts}
     * 编码为 JSON 写进 {@code MESSAGE_}，{@code TYPE_} 仍保留 {@code logType}（对齐 RequestLogType）。
     * 这样读侧切到 {@code HistoryService.createCommentQuery} 时可直接还原全部维度，无需再加列。</p>
     */
    private void syncCommentToEngine(Long instId, Long wfTaskId, String nodeKey, Long operator,
                                     String logType, String opinion) {
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
            Map<String, Object> payload = new LinkedHashMap<>(8);
            payload.put("nodeKey", nodeKey == null ? "" : nodeKey);
            payload.put("operator", operator == null ? 0L : operator);
            payload.put("wfTaskId", wfTaskId == null ? 0L : wfTaskId);
            payload.put("opinion", opinion == null ? "" : opinion);
            payload.put("ts", System.currentTimeMillis());
            String message = COMMENT_MAPPER.writeValueAsString(payload);
            processService.addComment(engineTaskId, procInstId, logType, message);
        } catch (Exception e) {
            log.warn("[WfWriteHelper] 审批意见双写引擎失败（忽略，不影响台账）: {}", e.getMessage());
        }
    }
}
