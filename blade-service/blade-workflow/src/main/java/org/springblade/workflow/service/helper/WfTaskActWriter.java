package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 任务业务列「写回原生 ACT_RU_TASK / ACT_HI_TASKINST」收口器（去 wf_ 表 P4/P5 的写侧，方案 A1）。
 *
 * <p><b>背景</b>：{@code wf_task} 是引擎 {@code ACT_RU_TASK} 之上的业务富化层。经盘点，ACT 原生已覆盖
 * {@code PROC_INST_ID_(instId) / TASK_DEF_KEY_(nodeKey) / ASSIGNEE_(assignee) / CREATE_TIME_(receiveTime)
 * / DUE_DATE_(dueTime) / END_TIME_(operateTime)}，但下列 6 类是 {@code wf_task} 独有、ACT 没有的，
 * 必须先落列（见 {@code doc/sql/migration/act_add_task_biz_columns.sql}）+ 双写，才能忠实翻源：
 * {@code IS_TEST_ / BUSINESS_STATUS_ / ORIGINAL_USER_ / SIGN_ORDER_ / VIEW_TIME_ / TIMEOUT_HANDLED_}。</p>
 *
 * <p><b>关键时序（history=audit）</b>：Flowable 在<b>任务创建时</b>即落 {@code ACT_HI_TASKINST} 行、
 * 完成时补 {@code END_TIME_}；而 {@code ACT_RU_TASK} 行在任务完成时会<b>被删除</b>。因此业务列必须
 * 在创建时同步写入<b>两张表</b>——只写 RU 会在任务完成后丢失维度，导致「已办」读不到子状态。</p>
 *
 * <p><b>与 wf_task 的关系</b>：台账写入模式（{@code blade.workflow.task-ledger-mode}）决定写路径。
 * {@code direct}（默认，去 wf_ 表）：本类<b>单路径</b>直写 ACT 业务列，不再写 {@code wf_task}；
 * {@code legacy}：仍由 {@code WfTaskMapper} 落台账（双写并存，互为校验）。{@code wf_task} 已于
 * 批次 1 在 direct 模式下退役，读侧也已全部翻源到 ACT。</p>
 *
 * <p><b>事务</b>：本类无 {@code @Transactional}；调用方（{@code WfInstanceServiceImpl#insertTask}、
 * 办理/退回/查看/超时等生命周期动作）已处于事务中，且 ACT_* 与 wf_* 共用同一 DataSource，
 * 故 {@link JdbcTemplate} 的 UPDATE 自动加入同一事务，与引擎插入同提交/回滚。</p>
 *
 * <p><b>开关</b>：{@code blade.workflow.task-act-write.enabled} 默认 false —— 关闭时所有方法空操作，
 * 完全不改变现有写入行为（H2 单测与现网默认零影响）。开启后写回 ACT_*。</p>
 *
 * <p><b>⚠️ 会签/或签 N:1 约束（硬门槛，务必先读）</b>：{@code multiInstanceEnabled=false}（默认）时，
 * blade 用「单个引擎 userTask + N 条 {@code wf_task}」自研会签——{@code WfInstanceServiceImpl} 对同一
 * {@code engineTaskId} 按办理人循环建多条 {@code wf_task}（见其注释「幂等键改为 (engineTaskId, assignee)」）。
 * 而 {@code ACT_RU_TASK}/{@code ACT_HI_TASKINST} 每个引擎任务<b>只有一行、一个 ASSIGNEE_</b>，
 * 无法表达「会签中 A 已办 / B 仍待办」。若按 {@code engineTaskId} 覆盖写入，N 个办理人会<b>互相踩踏
 * （最后写入者赢）</b>。故 {@link #sync} 对「同一引擎任务对应多条 wf_task」的情形<b>直接跳过</b>
 * （保守不写，绝不写坏）；待会签下沉引擎多实例（{@code multiInstanceEnabled=true}，1:1）后自动生效。
 * 这意味着：仅落业务列<b>不足以</b>翻源待办/已办，会签 N:1 是第二个硬门槛。</p>
 *
 * <p><b>幂等/容错</b>：写回失败仅告警不抛出（双写预热期不得阻断主流程）；对已完成任务，
 * RU 表 UPDATE 命中 0 行属正常，不影响 HI 表。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfTaskActWriter {

    private final JdbcTemplate jdbcTemplate;

    @Value("${blade.workflow.task-act-write.enabled:false}")
    private boolean enabled;

    /**
     * 解析任务租户（T-13）。
     *
     * <p>引擎创建任务时不带租户，ACT_RU_TASK/ACT_HI_TASKINST 的 TENANT_ID_ 会为空；
     * 而待办/已办读源=act 的查询（{@code WfTaskActReader}）已按租户过滤，
     * 空租户会导致新任务<b>查不到</b>。故写回时必须补上租户。</p>
     */
    private static String resolveTenant(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        String current = WfAuthUtil.tenantId();
        if (current != null && !current.isBlank()) {
            return current;
        }
        return "000000";
    }

    /** 业务子状态码 → 原生 BUSINESS_STATUS_ 字符串（与 wf_task.status 一一对应） */
    private static String statusStr(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case WfTask.STATUS_TODO -> "TODO";
            case WfTask.STATUS_DONE -> "DONE";
            case WfTask.STATUS_FINISHED -> "FINISHED";
            case WfTask.STATUS_AUTO_SUBMIT -> "AUTO_SUBMIT";
            case WfTask.STATUS_COADJUTANT -> "COADJUTANT";
            case WfTask.STATUS_CIRCULATE -> "CIRCULATE";
            case WfTask.STATUS_READ -> "READ";
            default -> null;
        };
    }

    /**
     * 任务业务列同步写回（创建 / 状态变更 / 查看 / 超时处置 通用）。
     *
     * <p>【去 wf_ 表 · 批次 1 / 2026-11-10】按 <b>D18</b> 删除原 N:1 守卫
     * （{@code SELECT COUNT(*) FROM wf_task} 与 {@code wf_ext_task} 合并统计）：
     * {@code WfExtTask} 行从不调用本方法、不会踩踏 ACT 单行，该守卫只会让
     * 「抄送后审批」跳过 ACT 业务列更新（{@code WfCirculateDriftReproTest} 已复现）。
     * 现统一委托 {@link WfTaskActDirectWriter}，保持 HI 先 / RU 后 与
     * {@code catch → log.warn} 容错。</p>
     *
     * @param task 任务实体；{@code engineTaskId} 为空（合成待办，引擎侧无对应任务）时跳过
     * @deprecated 2026-11-10 起新代码请直接用 {@link WfTaskActDirectWriter}；
     *     本方法保留作为既有调用点的兼容入口与 legacy 回退路径。
     */
    @Deprecated
    public void sync(WfTask task) {
        if (!enabled || task == null || task.getEngineTaskId() == null) {
            return;
        }
        java.util.LinkedHashMap<String, Object> cols = new java.util.LinkedHashMap<>();
        // 仅写非 null 字段：状态变更 patch 不带全量字段时，避免把 ACT 业务列覆盖成 null
        if (task.getStatus() != null) {
            cols.put("BUSINESS_STATUS_", statusStr(task.getStatus()));
        }
        if (task.getIsTest() != null) {
            cols.put("IS_TEST_", task.getIsTest());
        }
        if (task.getOriginalUser() != null) {
            cols.put("ORIGINAL_USER_", String.valueOf(task.getOriginalUser()));
        }
        if (task.getSignOrder() != null) {
            cols.put("SIGN_ORDER_", task.getSignOrder());
        }
        if (task.getViewTime() != null) {
            cols.put("VIEW_TIME_", task.getViewTime());
        }
        if (task.getTimeoutHandled() != null) {
            cols.put("TIMEOUT_HANDLED_", task.getTimeoutHandled());
        }
        if (task.getDueTime() != null) {
            cols.put("DUE_DATE_", task.getDueTime());
        }
        if (task.getId() != null) {
            cols.put("BIZ_TASK_ID_", task.getId());
        }
        if (task.getAssignee() != null) {
            cols.put("BIZ_ASSIGNEE_", task.getAssignee());
        }
        cols.put("TENANT_ID_", resolveTenant(task.getTenantId()));
        WfTaskActDirectWriter.writePartial(jdbcTemplate, task.getEngineTaskId(), cols);
    }
}
