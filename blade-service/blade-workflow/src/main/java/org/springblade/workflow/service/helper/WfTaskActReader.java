package org.springblade.workflow.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 待办/已办「读源=act」时的 {@code ACT_RU_TASK} / {@code ACT_HI_TASKINST} 读取器 + 读源开关。
 *
 * <p>与 {@link WfTaskActWriter} 对称：写侧把 6 类业务列（{@code BUSINESS_STATUS_/IS_TEST_/ORIGINAL_USER_/
 * SIGN_ORDER_/VIEW_TIME_/TIMEOUT_HANDLED_}）加 {@code BIZ_TASK_ID_} 双写到 ACT_*，本类把它们还原成
 * {@link WfTaskVO} 维度，使待办/已办列表无需回读 {@code wf_task}。</p>
 *
 * <p><b>id 语义（关键）</b>：{@code WfTaskVO.id} 必须仍是 <b>blade 业务任务ID（wf_task.id）</b>——
 * 操作接口（approve/转办/退回/查看）一律走 {@code requireTodoTask(taskId) → taskMapper.selectById(taskId)}
 * 按 {@code wf_task.id} 查。故本类取 {@code BIZ_TASK_ID_} 而非引擎 {@code ID_}，保证翻源后操作链路不变。</p>
 *
 * <p><b>节点名</b>：取自引擎任务的 {@code NAME_}（Flowable 原生），不再依赖 {@code wf_process_node}。</p>
 *
 * <p><b>前置</b>：须先开启 {@code blade.workflow.task-act-write.enabled} 双写 + 执行
 * {@code act_add_task_biz_columns.sql} / {@code act_backfill_task_biz_id.sql}，并对存量业务列回填；
 * 否则翻源后 {@code BIZ_TASK_ID_} / {@code BUSINESS_STATUS_} 为空（id 为 null 将无法操作）。</p>
 */
@Slf4j
@Component
public class WfTaskActReader {

    /** 任务读源：wf=遗留 wf_task（默认，零行为变化）；act=原生 ACT_RU_TASK / ACT_HI_TASKINST */
    @Value("${blade.workflow.task-read-source:wf}")
    private String taskReadSource;

    private final JdbcTemplate jdbcTemplate;

    public WfTaskActReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 任务读源是否切到原生 ACT（去 wf_ 表） */
    public boolean actRead() {
        return "act".equalsIgnoreCase(taskReadSource);
    }

    /**
     * 待办：读 {@code ACT_RU_TASK}（引擎中仍在运行的任务即待办）。
     *
     * @param assignee 办理人（引擎 {@code ASSIGNEE_} 以字符串存用户ID）
     */
    public List<WfTaskVO> todoFromAct(Long assignee) {
        String sql = "SELECT r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
            + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, "
            + "r.NAME_ AS nodeName, r.CREATE_TIME_ AS receiveTime, r.DUE_DATE_ AS dueTime, "
            + "COALESCE(r.IS_TEST_, 0) AS isTest, "
            + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
            + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
            + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
            + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
            + "WHERE COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) = ? AND COALESCE(r.IS_TEST_, 0) = 0 "
            + "ORDER BY r.CREATE_TIME_ DESC";
        return jdbcTemplate.query(sql, (rs, i) -> {
            WfTaskVO vo = new WfTaskVO();
            fillCommon(vo, rs);
            vo.setStatus(WfTask.STATUS_TODO);
            vo.setReceiveTime(rs.getTimestamp("receiveTime"));
            vo.setDueTime(rs.getTimestamp("dueTime"));
            return vo;
        }, String.valueOf(assignee));
    }

    /**
     * 已办：读 {@code ACT_HI_TASKINST}（{@code END_TIME_} 非空即已办结）。
     *
     * <p>状态取 {@code BUSINESS_STATUS_} 反解；为空（双写前的存量）按 {@code DONE} 处理。</p>
     */
    public List<WfTaskVO> doneFromAct(Long assignee) {
        String sql = "SELECT h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
            + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, "
            + "h.NAME_ AS nodeName, h.START_TIME_ AS receiveTime, h.END_TIME_ AS operateTime, "
            + "COALESCE(h.IS_TEST_, 0) AS isTest, h.BUSINESS_STATUS_ AS taskStatus, "
            + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
            + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
            + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
            + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
            + "WHERE COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) = ? AND COALESCE(h.IS_TEST_, 0) = 0 "
            + "AND h.END_TIME_ IS NOT NULL "
            + "ORDER BY h.END_TIME_ DESC";
        return jdbcTemplate.query(sql, (rs, i) -> {
            WfTaskVO vo = new WfTaskVO();
            fillCommon(vo, rs);
            Integer st = taskStatus(rs.getString("taskStatus"));
            vo.setStatus(st == null ? WfTask.STATUS_DONE : st);
            vo.setReceiveTime(rs.getTimestamp("receiveTime"));
            vo.setOperateTime(rs.getTimestamp("operateTime"));
            return vo;
        }, String.valueOf(assignee));
    }

    /**
     * 角标计数（顶栏待办红点 / 监控）：待办读 {@code ACT_RU_TASK}、已办读 {@code ACT_HI_TASKINST}。
     *
     * <p>与列表同口径保留【兜底】：ACT 表达不了的（存量 N:1 会签 / 孤儿 / 无引擎任务的合成待办）
     * 仍按 {@code wf_task.id NOT IN (ACT 已覆盖的 BIZ_TASK_ID_)} 补计，保证角标数字与翻源前一致。</p>
     *
     * <p>注意「已办」口径与原实现一致：{@code status <> 0}（非待办即已办），而非仅限 2/4/6/7/8/11。</p>
     *
     * <p>⚠️ 前提：会签/或签/依次须已<b>全面下沉引擎多实例</b>，否则引擎任务生命周期与 blade 任务状态
     * 不一致（自研 {@code closeSiblings} 只改 {@code wf_task.status}、不完成引擎任务），
     * 会导致<b>待办多算</b>。详见 {@code doc/md/去wf_表-任务读源翻源上线检查.md} §4.5。</p>
     */
    public java.util.Map<String, Long> countFromAct(Long assignee) {
        String a = String.valueOf(assignee);
        long todoAct = countOne(
            "SELECT COUNT(*) FROM ACT_RU_TASK "
                + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0", a);
        long doneAct = countOne(
            "SELECT COUNT(*) FROM ACT_HI_TASKINST "
                + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0 "
                + "AND END_TIME_ IS NOT NULL", a);
        long todoResidue = countOne(
            "SELECT COUNT(*) FROM wf_task t WHERE t.assignee = ? AND t.status = 0 AND t.is_test = 0 "
                + "AND t.id NOT IN (SELECT BIZ_TASK_ID_ FROM ACT_RU_TASK "
                + "  WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0 "
                + "  AND BIZ_TASK_ID_ IS NOT NULL)", assignee, a);
        long doneResidue = countOne(
            "SELECT COUNT(*) FROM wf_task t WHERE t.assignee = ? AND t.status <> 0 AND t.is_test = 0 "
                + "AND t.id NOT IN (SELECT BIZ_TASK_ID_ FROM ACT_HI_TASKINST "
                + "  WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0 "
                + "  AND END_TIME_ IS NOT NULL AND BIZ_TASK_ID_ IS NOT NULL)", assignee, a);
        java.util.Map<String, Long> r = new java.util.LinkedHashMap<>();
        r.put("todo", todoAct + todoResidue);
        r.put("done", doneAct + doneResidue);
        return r;
    }

    private Long countOne(String sql, Object... args) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    /** 两表共有的实例维度 + 任务维度回填 */
    private static void fillCommon(WfTaskVO vo, ResultSet rs) throws SQLException {
        vo.setId(getLong(rs, "bizTaskId"));
        vo.setInstId(getLong(rs, "instId"));
        vo.setDefId(getLong(rs, "defId"));
        vo.setFormId(getLong(rs, "formId"));
        vo.setDataId(getLong(rs, "dataId"));
        vo.setStarter(getLong(rs, "starter"));
        vo.setAssignee(getLong(rs, "assignee"));
        vo.setNodeKey(rs.getString("nodeKey"));
        vo.setNodeName(rs.getString("nodeName"));
        vo.setTitle(rs.getString("title"));
        vo.setUrgency(getInt(rs, "urgency"));
        vo.setInstStatus(instStatus(rs.getString("instStatus")));
        vo.setIsTest(getInt(rs, "isTest"));
        vo.setStartTime(rs.getTimestamp("startTime"));
    }

    /** {@code BUSINESS_STATUS_}（任务子状态）→ wf_task.status */
    private static Integer taskStatus(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "TODO" -> WfTask.STATUS_TODO;
            case "DONE" -> WfTask.STATUS_DONE;
            case "FINISHED" -> WfTask.STATUS_FINISHED;
            case "AUTO_SUBMIT" -> WfTask.STATUS_AUTO_SUBMIT;
            case "COADJUTANT" -> WfTask.STATUS_COADJUTANT;
            case "CIRCULATE" -> WfTask.STATUS_CIRCULATE;
            case "READ" -> WfTask.STATUS_READ;
            default -> null;
        };
    }

    /** {@code ACT_HI_PROCINST.BUSINESS_STATUS_}（实例终态）→ WfTaskVO.instStatus */
    private static Integer instStatus(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "RUNNING" -> 0;
            case "APPROVED" -> 1;
            case "REJECTED" -> 2;
            case "CANCELED" -> 3;
            case "SUSPENDED" -> 4;
            case "DRAFT" -> 5;
            default -> null;
        };
    }

    private static Long getLong(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).longValue();
    }

    private static Integer getInt(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).intValue();
    }
}
