package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfTask;
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
 * <p><b>与 wf_task 的关系</b>：迁移期间本类与 {@code WfTaskMapper} 写入<b>并存</b>（双写），互为校验；
 * 读侧切换（待办/已办读 ACT）且验证无误后，才退役 {@code wf_task}。</p>
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
     * <p>同时写 {@code ACT_RU_TASK} 与 {@code ACT_HI_TASKINST}（按 {@code engineTaskId} 定位，即两表 {@code ID_}）。
     * 传已持久化的 {@link WfTask}（含 id / engineTaskId / status / isTest / originalUser / signOrder /
     * viewTime / timeoutHandled 的最新值）。</p>
     *
     * @param task 任务实体；{@code engineTaskId} 为空（如「退回发起人」的合成待办，引擎侧无对应任务）时跳过
     */
    public void sync(WfTask task) {
        if (!enabled || task == null || task.getEngineTaskId() == null) {
            return;
        }
        String engineTaskId = task.getEngineTaskId();
        // 会签/或签/依次：同一引擎任务对应 N 条 wf_task（每办理人一条），ACT 单 row 无法表达，
        // 按人覆盖写会互相踩踏 → 保守跳过（详见类注释「会签/或签 N:1 约束」）。
        // 计数失败同样跳过，宁可不写也不能写坏。
        try {
            Integer siblings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_task WHERE engine_task_id = ?", Integer.class, engineTaskId);
            if (siblings != null && siblings > 1) {
                return;
            }
        } catch (Exception e) {
            log.warn("[WfTaskActWriter] 统计同一引擎任务的 wf_task 条数失败，跳过双写. engineTaskId={}",
                engineTaskId, e);
            return;
        }
        Object[] args = {
            statusStr(task.getStatus()),
            task.getIsTest(),
            task.getOriginalUser() == null ? null : String.valueOf(task.getOriginalUser()),
            task.getSignOrder(),
            task.getViewTime(),
            task.getTimeoutHandled(),
            // blade 业务任务ID：翻源后待办/已办列表仍吐此 ID，操作接口（approve/转办/退回）链路不变
            task.getId(),
            // blade 办理人：未开多实例时引擎 ASSIGNEE_ 常为 NULL（办理人只在 wf_task），读源须以此为准
            task.getAssignee(),
            task.getEngineTaskId()
        };
        // 已办：HI 行承载最终业务维度（RU 行完成即删除，故必须写 HI）
        try {
            jdbcTemplate.update(
                "UPDATE ACT_HI_TASKINST SET BUSINESS_STATUS_=?, IS_TEST_=?, ORIGINAL_USER_=?, "
                    + "SIGN_ORDER_=?, VIEW_TIME_=?, TIMEOUT_HANDLED_=?, BIZ_TASK_ID_=?, "
                    + "BIZ_ASSIGNEE_=? WHERE ID_=?",
                args);
        } catch (Exception e) {
            log.warn("[WfTaskActWriter] 写回 ACT_HI_TASKINST 失败（双写预热，不影响台账）. engineTaskId={}",
                task.getEngineTaskId(), e);
        }
        // 待办：RU 行存在时同步（已完成的任务命中 0 行，属正常）
        try {
            jdbcTemplate.update(
                "UPDATE ACT_RU_TASK SET BUSINESS_STATUS_=?, IS_TEST_=?, ORIGINAL_USER_=?, "
                    + "SIGN_ORDER_=?, VIEW_TIME_=?, TIMEOUT_HANDLED_=?, BIZ_TASK_ID_=?, "
                    + "BIZ_ASSIGNEE_=? WHERE ID_=?",
                args);
        } catch (Exception e) {
            log.warn("[WfTaskActWriter] 写回 ACT_RU_TASK 失败（双写预热，不影响台账）. engineTaskId={}",
                task.getEngineTaskId(), e);
        }
    }
}
