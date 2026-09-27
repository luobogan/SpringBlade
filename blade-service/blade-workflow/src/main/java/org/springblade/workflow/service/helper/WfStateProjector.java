package org.springblade.workflow.service.helper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 方案C 台账状态投影器 —— <b>阶段2：真实反写（C1 强一致）</b>。
 *
 * <p><b>阶段1 已完成并通过验收</b>（2026-09-27 实测）：状态变更类事件映射全部零差异 ——
 * {@code TASK_COMPLETED} / {@code PROCESS_CANCELLED} / {@code ENTITY_SUSPENDED} /
 * {@code ENTITY_ACTIVATED} / {@code TASK_ASSIGNED} 均 0 差异。</p>
 *
 * <p><b>两类事件结构性不可比，本类不参与反写</b>（详见各自方法注释）：
 * <ul>
 *   <li>{@code PROCESS_STARTED}：{@code engine_inst_id} 由业务在引擎调用返回后回填；</li>
 *   <li>{@code TASK_CREATED}：待办行由业务 {@code advance()} 在引擎调用返回后建立，
 *       且办理人由业务侧 {@code WfOperatorResolver} 解析（引擎事件里没有操作者/表单信息）。</li>
 * </ul>
 * 由此得出的<b>范围结论</b>：「建待办」无法下沉为事件驱动，仍由业务侧负责；
 * 事件驱动只承接<b>状态变更</b>（已办 / 终态 / 暂停 / 恢复）。</p>
 *
 * <p><b>反写约束</b>：① 幂等（先查后写、目标态覆盖，不累加）；② 绝不回查 {@code ACT_RU_*}
 * （事件触发时运行时行可能已删，§4.5），只用事件自带的 id 关联 {@code wf_*}；
 * ③ 与业务显式写并存（阶段2 双写校验），两者写同值。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfStateProjector {

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;

    /** 差异计数（按事件类型分桶）：反写前先比对，不一致则计数并写为目标态，保留持续校验能力 */
    private final Map<String, AtomicLong> diffCounters = new ConcurrentHashMap<>();

    // ==================== 结构性不可比：仅记录，不反写 ====================

    /**
     * PROCESS_STARTED：不可比，跳过。
     *
     * <p>业务先建 wf_instance 行 → 调引擎 → <b>返回后</b>才回填 {@code engine_inst_id}。
     * 本事件在引擎调用<b>内部</b>派发，此时该列仍为 NULL，按它查必然查不到。</p>
     */
    public void onProcessStarted(String procInstId) {
        log.info("[WfStateProjector][skip] PROCESS_STARTED -> engineInstId={}；"
            + "engine_inst_id 由业务在引擎调用返回后回填，事件时点不可比", procInstId);
    }

    /**
     * TASK_CREATED：不可比，跳过（与 PROCESS_STARTED 同一类时点限制）。
     *
     * <p>业务顺序是「先调引擎、返回后再建待办」：{@code start} / {@code advance} 驱动引擎，
     * 引擎在调用内部同步派发本事件，此刻业务尚未 {@code insertTask} → 查必 0 行。</p>
     *
     * <p>阶段1 实测佐证：4 条 TASK_CREATED 全计为差异，但库里 4 条引擎任务有 3 条确实存在
     * wf_task 行；反证 TASK_COMPLETED 零差异（审批是业务先置已办、再 completeTask）。</p>
     *
     * <p><b>故「建待办」不能下沉</b>，仍由业务侧 {@code advance()} 负责。</p>
     */
    public void onTaskCreated(String taskId, String procInstId, String assignee) {
        log.info("[WfStateProjector][skip] TASK_CREATED -> engineTaskId={}, procInstId={}；"
            + "业务待办行在引擎调用返回后才建立，且办理人由业务侧解析，事件时点不可反写", taskId, procInstId);
    }

    /** TASK_ASSIGNED：办理人由业务侧解析并已写入 wf_task，此处无需反写 */
    public void onTaskAssigned(String taskId, String assignee) {
        log.debug("[WfStateProjector][skip] TASK_ASSIGNED -> engineTaskId={}, assignee={}；"
            + "办理人由业务侧 WfOperatorResolver 解析，已随建行写入", taskId, assignee);
    }

    // ==================== 状态变更类：真实反写（幂等 + 目标态覆盖） ====================

    /** PROCESS_COMPLETED → wf_instance.status=通过(1) + end_time，并关闭残留待办 */
    public void onProcessCompleted(String procInstId) {
        writeTerminal(procInstId, WfInstance.STATUS_APPROVED, "PROCESS_COMPLETED");
    }

    /**
     * PROCESS_CANCELLED → 终态由 {@link #resolveCancelTarget} 派生（不通过2 / 撤销3），
     * 并补 end_time、关闭残留待办。
     *
     * <p><b>「不通过(2)」的处理 —— 靠 intent，不靠「业务先写」</b>：本事件只有「取消」语义，
     * 分不清「不通过(2)」与「撤销(3)」。方案是业务在调引擎终结<b>之前</b>把期望终态写入
     * {@code wf_instance.pending_status}（intent），本方法读取它派生终态。</p>
     *
     * <p><b>两道防线</b>：① 仅当实例仍为非终态（运行中0 / 暂停4）才写，已是终态则保留业务值；
     * ② 无 intent 时按事件语义默认撤销(3)。</p>
     *
     * <p><b>历史说明</b>：阶段2 曾采用「业务先写终态、监听发现已是终态就跳过」的做法（无需 intent 列），
     * 阶段3 摘掉业务显式写后该做法失效，故改为 intent 方案。</p>
     */
    public void onProcessCancelled(String procInstId) {
        writeCancel(procInstId);
    }

    /** ENTITY_SUSPENDED → wf_instance.status=暂停(4) */
    public void onEntitySuspended(String procInstId) {
        // 合法前置态：运行中(0)。阶段3 起业务不再预写「暂停」，事件到达时仍为 0 属正常。
        writeInstanceStatus(procInstId, WfInstance.STATUS_SUSPENDED, false, "ENTITY_SUSPENDED",
            WfInstance.STATUS_RUNNING);
    }

    /** ENTITY_ACTIVATED → wf_instance.status=运行中(0) */
    public void onEntityActivated(String procInstId) {
        // 合法前置态：暂停(4)。恢复只能由暂停态过渡而来。
        writeInstanceStatus(procInstId, WfInstance.STATUS_RUNNING, false, "ENTITY_ACTIVATED",
            WfInstance.STATUS_SUSPENDED);
    }

    /** TASK_COMPLETED → 该引擎任务对应的待办置已办(2) + operate_time */
    public void onTaskCompleted(String taskId, String procInstId) {
        if (taskId == null) {
            log.warn("[WfStateProjector] TASK_COMPLETED -> 事件未携带 taskId，无法反写");
            return;
        }
        List<WfTask> rows = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getEngineTaskId, taskId));
        if (rows.isEmpty()) {
            // 无对应行：多为「退回发起人」parked 任务等合法态，或业务尚未建行 —— 不抛异常（C1 下由调用方决定）
            log.info("[WfStateProjector][skip] TASK_COMPLETED -> 无 engine_task_id={} 的 wf_task 行，跳过反写", taskId);
            return;
        }
        Date now = new Date();
        for (WfTask t : rows) {
            if (t.getStatus() != null && t.getStatus() == WfTask.STATUS_DONE) {
                continue; // 幂等：已是已办
            }
            if (t.getStatus() != null && t.getStatus() != WfTask.STATUS_TODO) {
                continue; // 办结/协办等非待办态不动，避免误改业务语义
            }
            WfTask patch = new WfTask();
            patch.setId(t.getId());
            patch.setStatus(WfTask.STATUS_DONE);
            patch.setOperateTime(now);
            taskMapper.updateById(patch);
            log.info("[WfStateProjector][write] TASK_COMPLETED -> wfTaskId={} 置已办(2)", t.getId());
        }
    }

    // ==================== 反写工具（幂等，只走 wf_* Mapper） ====================

    /**
     * 终态写入：置目标终态 + end_time + 关闭残留待办。
     * 合法前置态为「运行中(0) / 暂停(4)」——终态只能由这两个非终态过渡而来。
     */
    private void writeTerminal(String procInstId, int target, String event) {
        writeInstanceStatus(procInstId, target, true, event,
            WfInstance.STATUS_RUNNING, WfInstance.STATUS_SUSPENDED);
    }

    /**
     * 撤销类写入：目标终态由 {@link #resolveCancelTarget} 派生，随后清空 intent。
     *
     * <p><b>为什么需要 intent</b>：{@code PROCESS_CANCELLED} 只有「取消」语义，
     * 无法区分「不通过(2)」与「撤销(3)」。故业务在调引擎终结前把期望终态写入
     * {@code wf_instance.pending_status}，本方法读取该列派生正确终态 —— 监听仍只依赖
     * {@code wf_*} Mapper，不注入引擎 Service、不回查 {@code ACT_RU_*}（§4.2 / §4.5）。</p>
     *
     * <p><b>两道防线</b>：① 仅当实例仍为非终态（运行中0 / 暂停4）才写，已是终态则保留业务值；
     * ② 无 intent 时按事件语义默认撤销(3)。</p>
     */
    private void writeCancel(String procInstId) {
        WfInstance inst = findInstance(procInstId, "PROCESS_CANCELLED");
        if (inst == null) {
            return;
        }
        Integer cur = inst.getStatus();
        if (cur != null && cur != WfInstance.STATUS_RUNNING && cur != WfInstance.STATUS_SUSPENDED) {
            log.info("[WfStateProjector][skip] PROCESS_CANCELLED -> instId={} 已是终态(status={})，保留业务值不改写",
                inst.getId(), cur);
            clearPendingStatus(inst.getId());
            return;
        }
        int target = resolveCancelTarget(inst);
        diffIfMismatch("PROCESS_CANCELLED", cur, target,
            WfInstance.STATUS_RUNNING, WfInstance.STATUS_SUSPENDED);
        applyInstanceStatus(inst, target, true, "PROCESS_CANCELLED");
        clearPendingStatus(inst.getId());
    }

    /**
     * 派生撤销类终态：优先取业务预留的 intent（{@code pending_status}），
     * 非空且 ∈ {1通过, 2不通过, 3撤销} 则用之；否则按事件语义默认 3(撤销)。
     */
    private int resolveCancelTarget(WfInstance inst) {
        Integer pending = inst.getPendingStatus();
        if (pending != null && (pending == WfInstance.STATUS_APPROVED
            || pending == WfInstance.STATUS_REJECTED || pending == WfInstance.STATUS_CANCELED)) {
            log.info("[WfStateProjector] PROCESS_CANCELLED -> 按 intent 派生终态. instId={}, pendingStatus={}",
                inst.getId(), pending);
            return pending;
        }
        return WfInstance.STATUS_CANCELED;
    }

    /**
     * 清空 intent，避免陈旧值影响后续派生。
     *
     * <p><b>必须用 UpdateWrapper 显式置 NULL，不能用 updateById</b>：MyBatis-Plus 的
     * {@code updateById} 只更新<b>非 null</b> 字段，传入全 null 实体会拼出
     * {@code UPDATE wf_instance WHERE id=?}（SET 子句为空）→ SQL 语法错误 →
     * 在 C1 强一致下抛异常并连带回滚整个引擎操作（真实事故：撤回返回 500）。</p>
     */
    private void clearPendingStatus(Long instId) {
        try {
            instanceMapper.update(null, new LambdaUpdateWrapper<WfInstance>()
                .setSql("pending_status = NULL")
                .eq(WfInstance::getId, instId));
        } catch (Exception e) {
            log.warn("[WfStateProjector] 清空 pending_status 失败（忽略）. instId={}, msg={}", instId, e.getMessage());
        }
    }

    /**
     * 通用状态反写：先查、比对（不一致计数）、再按目标态覆盖写入。
     *
     * @param terminal       是否终态（终态补 end_time 并关闭残留待办）
     * @param legalPreStates 该事件允许的「业务未预写」前置态（阶段3 后业务不再预写状态）
     */
    private void writeInstanceStatus(String procInstId, int target, boolean terminal, String event,
                                     int... legalPreStates) {
        WfInstance inst = findInstance(procInstId, event);
        if (inst == null) {
            return;
        }
        diffIfMismatch(event, inst.getStatus(), target, legalPreStates);
        applyInstanceStatus(inst, target, terminal, event);
    }

    private void applyInstanceStatus(WfInstance inst, int target, boolean terminal, String event) {
        Integer cur = inst.getStatus();
        if (cur != null && cur == target && !terminal) {
            return; // 幂等：非终态且已一致
        }
        WfInstance patch = new WfInstance();
        patch.setId(inst.getId());
        patch.setStatus(target);
        if (terminal) {
            patch.setEndTime(new Date());
        }
        instanceMapper.updateById(patch);
        log.info("[WfStateProjector][write] {} -> instId={} status: {} -> {}", event, inst.getId(), cur, target);
        if (terminal) {
            closeResidualTodos(inst.getId(), event);
        }
    }

    /**
     * 关闭实例上残留的待办（待办0 <b>与协办7</b> → 办结4），幂等。
     *
     * <p><b>必须同时覆盖协办(7)</b>：业务侧原实现（{@code WfWriteHelper#closePendingTasks}，
     * 源自 {@code terminate} 的关待办循环）关闭的是 {@code in(TODO 0, COADJUTANT 7)} 两类。
     * 阶段3 摘除业务循环后由本方法承接，若只关 0 会漏掉协办待办 —— 它们会变成孤儿，
     * 且 {@code WfTaskServiceImpl#done()} 把 {@code STATUS_COADJUTANT} 计入「已办」，会污染已办列表。</p>
     */
    private void closeResidualTodos(Long instId, String event) {
        int n = taskMapper.update(null, new LambdaUpdateWrapper<WfTask>()
            .set(WfTask::getStatus, WfTask.STATUS_FINISHED)
            .set(WfTask::getOperateTime, new Date())
            .eq(WfTask::getInstId, instId)
            .in(WfTask::getStatus, WfTask.STATUS_TODO, WfTask.STATUS_COADJUTANT));
        if (n > 0) {
            log.info("[WfStateProjector][write] {} -> 关闭残留待办 {} 条 (instId={})", event, n, instId);
        }
    }

    private WfInstance findInstance(String procInstId, String event) {
        if (procInstId == null) {
            log.warn("[WfStateProjector] {} -> 事件未携带 processInstanceId，无法反写", event);
            return null;
        }
        WfInstance inst = instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getEngineInstId, procInstId)
            .last("LIMIT 1"));
        if (inst == null) {
            log.warn("[WfStateProjector] {} -> 未找到 engine_inst_id={} 的 wf_instance，跳过反写", event, procInstId);
        }
        return inst;
    }

    /** 反写前比对：业务值与目标态不一致则计数（保留阶段1 的校验信号） */
    /**
     * 反写前比对 —— <b>阶段3 起语义已变，必须区分「漂移」与「正常待写」</b>。
     *
     * <p>阶段2（双写校验期）：业务已先写了同一个值，故「实际值 ≠ 目标态」即可判定为漂移。</p>
     *
     * <p>阶段3（切单写后）：业务<b>刻意不再预写</b>，事件到达时实际值就是「尚未写入」的
     * 合法前置态（例如暂停前是运行中0、恢复前是暂停4）——这是<b>正常流程</b>，不是漂移。
     * 实测佐证：阶段3 回归中 {@code ENTITY_SUSPENDED/ENTITY_ACTIVATED} 各计 1 条差异，
     * 但状态实际写对了，正是这个原因。</p>
     *
     * <p>故本方法只在实际值<b>既非目标态、也不属于合法前置态</b>时才计差异。</p>
     *
     * @param legalPreStates 该事件允许的「业务未预写」前置态（如暂停前允许 0，恢复前允许 4）
     */
    private void diffIfMismatch(String event, Integer actual, int target, int... legalPreStates) {
        if (actual == null) {
            diff(event, "实例状态为 NULL，无法判定；已按目标态 " + target + " 覆盖");
            return;
        }
        if (actual == target) {
            return; // 已是目标态：幂等命中，或双写期业务已写过同值
        }
        for (int pre : legalPreStates) {
            if (actual == pre) {
                return; // 合法前置态：业务按阶段3 约定未预写，由本反写补齐，属正常
            }
        }
        diff(event, "期望目标态=" + target + " 或合法前置态" + java.util.Arrays.toString(legalPreStates)
            + "，实际=" + actual + "；已按目标态覆盖");
    }

    private void diff(String event, String detail) {
        long n = diffCounters.computeIfAbsent(event, k -> new AtomicLong()).incrementAndGet();
        log.warn("[WfStateProjector][diff#{}] {} -> {}", n, event, detail);
    }

    /**
     * 差异计数快照（只读，按事件类型分桶）。供 {@code GET /monitor/ledger-shadow} 使用；
     * 计数为内存值，重启清零。
     */
    public Map<String, Long> diffSnapshot() {
        Map<String, Long> snap = new java.util.LinkedHashMap<>();
        diffCounters.forEach((event, counter) -> snap.put(event, counter.get()));
        return snap;
    }
}
