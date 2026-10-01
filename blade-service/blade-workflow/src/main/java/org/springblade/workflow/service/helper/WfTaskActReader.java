package org.springblade.workflow.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
     * 当前登录用户所属租户（无登录上下文返回 null，例如定时任务）。
     *
     * <p>前置：{@code ACT_RU_TASK/ACT_HI_TASKINST} 的 {@code TENANT_ID_} 须已回填
     * （{@code V2026.10.01_003__act_backfill_task_tenant.sql}），否则过滤后读空。</p>
     */
    private static String tenantOrNull() {
        String t = WfAuthUtil.tenantId();
        return (t == null || t.isBlank()) ? null : t;
    }

    /**
     * 追加 {@code ACT_*} 租户过滤（T-13）。
     *
     * <p>ACT_* 不是 Blade 托管的 MyBatis-Plus 实体，<b>不会被租户插件自动追加 tenant_id 条件</b>，
     * 必须显式 {@code TENANT_ID_ = ?}，否则多租户下会串数据。</p>
     *
     * @param sql   SQL 缓冲
     * @param args  参数列表
     * @param alias 表别名（null 表示无别名，如单表 COUNT 查询）
     */
    private static void appendActTenant(StringBuilder sql, List<Object> args, String alias) {
        String tenant = tenantOrNull();
        if (tenant == null) {
            log.warn("[blade-workflow] ACT 任务读源缺少租户上下文，本次查询未加租户过滤（多租户下可能串数据）");
            return;
        }
        sql.append(" AND ").append(alias == null ? "TENANT_ID_" : alias + ".TENANT_ID_").append(" = ?");
        args.add(tenant);
    }

    /** 追加 {@code wf_*} 表租户过滤（列名 {@code tenant_id}，与 ACT 的 {@code TENANT_ID_} 不同）。 */
    private static void appendWfTenant(StringBuilder sql, List<Object> args, String alias) {
        String tenant = tenantOrNull();
        if (tenant == null) {
            return;
        }
        sql.append(" AND ").append(alias).append(".tenant_id = ?");
        args.add(tenant);
    }

    /**
     * 待办：读 {@code ACT_RU_TASK}（引擎中仍在运行的任务即待办）。
     *
     * @param assignee 办理人（引擎 {@code ASSIGNEE_} 以字符串存用户ID）
     */
    public List<WfTaskVO> todoFromAct(Long assignee) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, "
                + "r.NAME_ AS nodeName, r.CREATE_TIME_ AS receiveTime, r.DUE_DATE_ AS dueTime, "
                + "COALESCE(r.IS_TEST_, 0) AS isTest, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) = ? AND COALESCE(r.IS_TEST_, 0) = 0");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "r");
        sql.append(" ORDER BY r.CREATE_TIME_ DESC");
        return jdbcTemplate.query(sql.toString(), (rs, i) -> {
            WfTaskVO vo = new WfTaskVO();
            fillCommon(vo, rs);
            vo.setStatus(WfTask.STATUS_TODO);
            vo.setReceiveTime(rs.getTimestamp("receiveTime"));
            vo.setDueTime(rs.getTimestamp("dueTime"));
            return vo;
        }, args.toArray());
    }

    /**
     * 已办：读 {@code ACT_HI_TASKINST}（{@code END_TIME_} 非空即已办结）。
     *
     * <p>状态取 {@code BUSINESS_STATUS_} 反解；为空（双写前的存量）按 {@code DONE} 处理。</p>
     */
    public List<WfTaskVO> doneFromAct(Long assignee) {
        StringBuilder sql = new StringBuilder(
            "SELECT h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, "
                + "h.NAME_ AS nodeName, h.START_TIME_ AS receiveTime, h.END_TIME_ AS operateTime, "
                + "COALESCE(h.IS_TEST_, 0) AS isTest, h.BUSINESS_STATUS_ AS taskStatus, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) = ? AND COALESCE(h.IS_TEST_, 0) = 0 "
                + "AND h.END_TIME_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "h");
        sql.append(" ORDER BY h.END_TIME_ DESC");
        return jdbcTemplate.query(sql.toString(), (rs, i) -> {
            WfTaskVO vo = new WfTaskVO();
            fillCommon(vo, rs);
            Integer st = taskStatus(rs.getString("taskStatus"));
            vo.setStatus(st == null ? WfTask.STATUS_DONE : st);
            vo.setReceiveTime(rs.getTimestamp("receiveTime"));
            vo.setOperateTime(rs.getTimestamp("operateTime"));
            return vo;
        }, args.toArray());
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

        // ① ACT 待办
        StringBuilder todoSql = new StringBuilder("SELECT COUNT(*) FROM ACT_RU_TASK "
            + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0");
        List<Object> todoArgs = new ArrayList<>();
        todoArgs.add(a);
        appendActTenant(todoSql, todoArgs, null);
        long todoAct = countOne(todoSql.toString(), todoArgs.toArray());

        // ② ACT 已办
        StringBuilder doneSql = new StringBuilder("SELECT COUNT(*) FROM ACT_HI_TASKINST "
            + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0 "
            + "AND END_TIME_ IS NOT NULL");
        List<Object> doneArgs = new ArrayList<>();
        doneArgs.add(a);
        appendActTenant(doneSql, doneArgs, null);
        long doneAct = countOne(doneSql.toString(), doneArgs.toArray());

        // ③ 兜底：ACT 表达不了的 wf_task 行。主查询与子查询【同口径带租户】，避免两侧口径不一致导致漏算/多算。
        StringBuilder ruSub = new StringBuilder("SELECT BIZ_TASK_ID_ FROM ACT_RU_TASK "
            + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0");
        List<Object> ruArgs = new ArrayList<>();
        ruArgs.add(a);
        appendActTenant(ruSub, ruArgs, null);
        ruSub.append(" AND BIZ_TASK_ID_ IS NOT NULL");

        StringBuilder todoResSql = new StringBuilder("SELECT COUNT(*) FROM wf_task t "
            + "WHERE t.assignee = ? AND t.status = 0 AND t.is_test = 0");
        List<Object> todoResArgs = new ArrayList<>();
        todoResArgs.add(assignee);
        appendWfTenant(todoResSql, todoResArgs, "t");
        todoResSql.append(" AND t.id NOT IN (").append(ruSub).append(")");
        todoResArgs.addAll(ruArgs);
        long todoResidue = countOne(todoResSql.toString(), todoResArgs.toArray());

        StringBuilder hiSub = new StringBuilder("SELECT BIZ_TASK_ID_ FROM ACT_HI_TASKINST "
            + "WHERE COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? AND COALESCE(IS_TEST_, 0) = 0 "
            + "AND END_TIME_ IS NOT NULL");
        List<Object> hiArgs = new ArrayList<>();
        hiArgs.add(a);
        appendActTenant(hiSub, hiArgs, null);
        hiSub.append(" AND BIZ_TASK_ID_ IS NOT NULL");

        StringBuilder doneResSql = new StringBuilder("SELECT COUNT(*) FROM wf_task t "
            + "WHERE t.assignee = ? AND t.status <> 0 AND t.is_test = 0");
        List<Object> doneResArgs = new ArrayList<>();
        doneResArgs.add(assignee);
        appendWfTenant(doneResSql, doneResArgs, "t");
        doneResSql.append(" AND t.id NOT IN (").append(hiSub).append(")");
        doneResArgs.addAll(hiArgs);
        long doneResidue = countOne(doneResSql.toString(), doneResArgs.toArray());

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
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            // Flowable ASSIGNEE_/BIZ_ASSIGNEE_ 等列是 VARCHAR 存数字ID，需解析而非强转
            String s = ((String) v).trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        }
        return null;
    }

    private static Integer getInt(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            return s.isEmpty() ? null : Integer.parseInt(s);
        }
        return null;
    }

    /**
     * 节点操作者面板（{@code WfInstanceServiceImpl#nodeOperators}）所需任务行：取某流程实例的全部任务
     * （待办 {@code ACT_RU_TASK} + 已办 {@code ACT_HI_TASKINST}），按节点归组。与 todo/done 同源，
     * 保证「操作者」面板与待办/已办列表一致。N:1 会签多引擎任务 → 多个操作者，天然覆盖。
     *
     * @param engineInstId 引擎流程实例ID（{@code ACT_*.PROC_INST_ID_}）
     */
    public List<NodeOpRow> nodeOperatorTasksFromAct(String engineInstId) {
        if (engineInstId == null) {
            return java.util.Collections.emptyList();
        }
        List<NodeOpRow> rows = new java.util.ArrayList<>();
        // 待办（RU 中仍为活动态，状态恒为 TODO；VIEW_TIME_ 区分「已查看 / 未操作」）
        jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee, VIEW_TIME_ AS viewTime "
                + "FROM ACT_RU_TASK WHERE PROC_INST_ID_ = ? AND COALESCE(IS_TEST_, 0) = 0",
            (rs, i) -> {
                rows.add(new NodeOpRow(rs.getString("nodeKey"), getLong(rs, "assignee"),
                    WfTask.STATUS_TODO, tsToMillis(rs, "viewTime")));
                return null;
            }, engineInstId);
        // 已办（HI 已完成，状态由 BUSINESS_STATUS_ 还原）
        jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee, "
                + "BUSINESS_STATUS_ AS taskStatus, VIEW_TIME_ AS viewTime "
                + "FROM ACT_HI_TASKINST WHERE PROC_INST_ID_ = ? AND COALESCE(IS_TEST_, 0) = 0 AND END_TIME_ IS NOT NULL",
            (rs, i) -> {
                Integer st = taskStatus(rs.getString("taskStatus"));
                rows.add(new NodeOpRow(rs.getString("nodeKey"), getLong(rs, "assignee"),
                    st == null ? WfTask.STATUS_DONE : st, tsToMillis(rs, "viewTime")));
                return null;
            }, engineInstId);
        return rows;
    }

    /**
     * 界面节点是否仍有活动任务（并行分支安全，{@code isNodeStillActive} 用）：该节点在
     * {@code ACT_RU_TASK} 仍有待办即视为活动。
     */
    public long countActiveNodeTasksFromAct(String engineInstId, String nodeKey) {
        if (engineInstId == null || nodeKey == null || nodeKey.isEmpty()) {
            return 0L;
        }
        return countOne("SELECT COUNT(*) FROM ACT_RU_TASK "
            + "WHERE PROC_INST_ID_ = ? AND TASK_DEF_KEY_ = ? AND COALESCE(IS_TEST_, 0) = 0",
            engineInstId, nodeKey);
    }

    /** 按业务任务ID（wf_task.id）取节点Key：办理页 UI 持有任务 → 定位节点。 */
    public String findNodeKeyByBizIdFromAct(Long bizTaskId) {
        if (bizTaskId == null) {
            return null;
        }
        String ru = jdbcTemplate.queryForObject(
            "SELECT TASK_DEF_KEY_ FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            String.class, String.valueOf(bizTaskId));
        if (ru != null) {
            return ru;
        }
        return jdbcTemplate.queryForObject(
            "SELECT TASK_DEF_KEY_ FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            String.class, String.valueOf(bizTaskId));
    }

    /** UI 持有任务是否已办结（提交/退回/转办后旧页签失效判定）：RU 有行=活动，HI 有行=办结。 */
    public boolean isTaskDoneFromAct(Long bizTaskId) {
        if (bizTaskId == null) {
            return false;
        }
        if (countOne("SELECT COUNT(*) FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ = ?", String.valueOf(bizTaskId)) > 0) {
            return false;
        }
        return countOne("SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ = ? AND END_TIME_ IS NOT NULL",
            String.valueOf(bizTaskId)) > 0;
    }

    /**
     * 按业务任务ID 还原 {@link WfTask}（仅填 {@code canOperate} 消费的 id/nodeKey/assignee/status），
     * 供 {@code render} 在 ACT 读源下判定「当前用户是否本节点待办人」。找不到返回 null。
     */
    public WfTask toWfTaskByBizIdFromAct(Long bizTaskId) {
        if (bizTaskId == null) {
            return null;
        }
        WfTask ru = jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee "
                + "FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            (rs, i) -> {
                WfTask t = new WfTask();
                t.setId(bizTaskId);
                t.setNodeKey(rs.getString("nodeKey"));
                t.setAssignee(getLong(rs, "assignee"));
                t.setStatus(WfTask.STATUS_TODO);
                return t;
            }, String.valueOf(bizTaskId)).stream().findFirst().orElse(null);
        if (ru != null) {
            return ru;
        }
        return jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee, "
                + "BUSINESS_STATUS_ AS taskStatus FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            (rs, i) -> {
                WfTask t = new WfTask();
                t.setId(bizTaskId);
                t.setNodeKey(rs.getString("nodeKey"));
                t.setAssignee(getLong(rs, "assignee"));
                Integer st = taskStatus(rs.getString("taskStatus"));
                t.setStatus(st == null ? WfTask.STATUS_DONE : st);
                return t;
            }, String.valueOf(bizTaskId)).stream().findFirst().orElse(null);
    }

    private static Long tsToMillis(ResultSet rs, String col) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.getTime();
    }

    /** 节点操作者归组用的任务行（与 wf_task 维度对齐，仅含面板所需字段）。 */
    public static final class NodeOpRow {
        private final String nodeKey;
        private final Long assignee;
        private final Integer status;
        private final Long viewTime;

        public NodeOpRow(String nodeKey, Long assignee, Integer status, Long viewTime) {
            this.nodeKey = nodeKey;
            this.assignee = assignee;
            this.status = status;
            this.viewTime = viewTime;
        }

        public String getNodeKey() {
            return nodeKey;
        }

        public Long getAssignee() {
            return assignee;
        }

        public Integer getStatus() {
            return status;
        }

        public Long getViewTime() {
            return viewTime;
        }
    }
}
