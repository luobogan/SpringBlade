package org.springblade.workflow.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.config.WfTaskResidueProperties;
import org.springblade.workflow.dto.WfTaskListFilter;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.entity.WfExtTask;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
 *
 * <p><b>列表查询索引（2026-11-08）</b>：WHERE 条件改为直接命中权威列 {@code BIZ_ASSIGNEE_}，
 * 配套迁移脚本 {@code V2026.11.08_001__act_task_assignee_index.sql}（回填 + 建复合索引）。
 * 原写法 {@code COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ?} 与 {@code COALESCE(IS_TEST_,0)=0}
 * 因函数包列使既有 ASSIGNEE_ 索引全部失效，实测 EXPLAIN {@code type=ALL + Using filesort}。
 * ⚠️ <b>部署顺序</b>：必须先执行迁移脚本的回填（保证 BIZ_ASSIGNEE_ 非空且与 COALESCE 语义等价），
 * 再部署本类改写；否则生产上 {@code BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL} 的行会漏单。
 * dev 实测该值为 0（两表均 0），故 dev 上改写等价且不漏单。</p>
 */
@Slf4j
@Component
public class WfTaskActReader {

    /** 任务读源：wf=遗留 wf_task（默认，零行为变化）；act=原生 ACT_RU_TASK / ACT_HI_TASKINST */
    @Value("${blade.workflow.task-read-source:wf}")
    private String taskReadSource;

    /**
     * 扩展任务读源（去 wf_ 表 · P6 退役第一台阶，2026-11-09）。
     *
     * <p>blade 扩展任务（协办/抄送/传阅/已读/退回重提交）已迁至 {@code wf_ext_task}，
     * 列表与角标的「扩展任务段」改由本表读取，不再回读 {@code wf_task}。
     * 判据用不可变的 {@code type}（语义类别）+ 自身 {@code status}，与原
     * {@code wf_task.status IN (7,8,11) OR engine_task_id 为空} 等价：</p>
     * <ul>
     *   <li>待办段：{@code type IN (2,6) AND status = 0}（加签协办/退回重提交）；</li>
     *   <li>已办段：{@code type IN (1,3,4,5)}（协办/抄送/传阅/已读，未读也进已办侧，
     *       与原 status=7/8/11 行为一致）；</li>
     *   <li>两段都排除 {@code type=7}（自由流程步骤名装饰行，非列表条目）。</li>
     * </ul>
     *
     * <p><b>裸 SQL 纪律</b>：{@code wf_ext_task} 虽 extends TenantEntity，但 JdbcTemplate
     * 绕过租户插件与 {@code @TableLogic}，故必须显式写 {@code is_deleted = 0} 与租户条件。</p>
     */
    private static final String EXT_TODO_TYPE_SQL = WfExtTask.TODO_TYPE_SQL;
    private static final String EXT_DONE_TYPE_SQL = WfExtTask.DONE_ONLY_TYPE_SQL;
    private static final String EXT_DECORATION_TYPE = String.valueOf(WfExtTask.TYPE_DECORATION);

    /**
     * 「读 wf_ext_task 的扩展任务」开关（去 wf_ 表 · P6 退役第一台阶）。
     *
     * <p>角标计数与列表口径<b>必须一致</b>：本方法自带残留兜底计数，若只关列表不关角标，
     * 会出现「角标 25、列表只有 24」的割裂。故与 {@code WfTaskServiceImpl#list/page}
     * 共用同一个开关。</p>
     */
    @Autowired
    private WfTaskResidueProperties residueMerge = new WfTaskResidueProperties();

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
     * @param f        列表筛选条件（title/defId/formId/时间范围，可 null）
     */
    public List<WfTaskVO> todoFromAct(Long assignee, WfTaskListFilter f) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, "
                + "r.NAME_ AS nodeName, r.CREATE_TIME_ AS receiveTime, r.DUE_DATE_ AS dueTime, "
                + "COALESCE(r.IS_TEST_, 0) AS isTest, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE r.BIZ_ASSIGNEE_ = ? AND (r.IS_TEST_ = 0 OR r.IS_TEST_ IS NULL)");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "r");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "r", "CREATE_TIME_", f);
        // 二级排序 ID_ 保证分页稳定：同一时刻创建的任务若只按 CREATE_TIME_ 排序，
        // LIMIT/OFFSET 在不同页可能返回相同行（造数实测 135 条同时间待办重叠 35 条）。
        sql.append(" ORDER BY r.CREATE_TIME_ DESC, r.ID_ DESC");
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
     * 待办分页（服务端真分页）：与 {@link #todoFromAct} 完全同口径，仅追加
     * {@code LIMIT ? OFFSET ?}，并复用同一 {@code RowMapper}。
     *
     * @param offset 偏移量，{@code (current - 1) * pageSize}
     * @param size   每页条数（调用方已做上限收敛）
     */
    public List<WfTaskVO> todoPageFromAct(Long assignee, WfTaskListFilter f, long offset, long size) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, "
                + "r.NAME_ AS nodeName, r.CREATE_TIME_ AS receiveTime, r.DUE_DATE_ AS dueTime, "
                + "COALESCE(r.IS_TEST_, 0) AS isTest, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE r.BIZ_ASSIGNEE_ = ? AND (r.IS_TEST_ = 0 OR r.IS_TEST_ IS NULL)");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "r");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "r", "CREATE_TIME_", f);
        appendStatusFilter(sql, args, "r", f, true);
        sql.append(" ORDER BY r.CREATE_TIME_ DESC, r.ID_ DESC LIMIT ? OFFSET ?");
        args.add(size);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), (rs, i) -> {
            WfTaskVO vo = new WfTaskVO();
            fillCommon(vo, rs);
            vo.setStatus(WfTask.STATUS_TODO);
            vo.setReceiveTime(rs.getTimestamp("receiveTime"));
            vo.setDueTime(rs.getTimestamp("dueTime"));
            return vo;
        }, args.toArray());
    }

    /** 待办总数（与 {@link #todoPageFromAct} 同口径，用于分页 total）。 */
    public long countTodoFromAct(Long assignee, WfTaskListFilter f) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ACT_RU_TASK r "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
            + "WHERE r.BIZ_ASSIGNEE_ = ? AND (r.IS_TEST_ = 0 OR r.IS_TEST_ IS NULL)");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "r");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "r", "CREATE_TIME_", f);
        appendStatusFilter(sql, args, "r", f, true);
        Long n = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    /**
     * 已办：读 {@code ACT_HI_TASKINST}（{@code END_TIME_} 非空即已办结）。
     *
     * <p>状态取 {@code BUSINESS_STATUS_} 反解；为空（双写前的存量）按 {@code DONE} 处理。</p>
     *
     * @param assignee 办理人
     * @param f        列表筛选条件（title/defId/formId/时间范围，可 null）
     */
    public List<WfTaskVO> doneFromAct(Long assignee, WfTaskListFilter f) {
        StringBuilder sql = new StringBuilder(
            "SELECT h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, "
                + "h.NAME_ AS nodeName, h.START_TIME_ AS receiveTime, h.END_TIME_ AS operateTime, "
                + "COALESCE(h.IS_TEST_, 0) AS isTest, h.BUSINESS_STATUS_ AS taskStatus, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE h.BIZ_ASSIGNEE_ = ? AND (h.IS_TEST_ = 0 OR h.IS_TEST_ IS NULL) "
                + "AND h.END_TIME_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "h");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "h", "END_TIME_", f);
        sql.append(" ORDER BY h.END_TIME_ DESC, h.ID_ DESC");
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

    /** 已办分页（服务端真分页）：与 {@link #doneFromAct} 同口径 + {@code LIMIT ? OFFSET ?}。 */
    public List<WfTaskVO> donePageFromAct(Long assignee, WfTaskListFilter f, long offset, long size) {
        StringBuilder sql = new StringBuilder(
            "SELECT h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, "
                + "h.NAME_ AS nodeName, h.START_TIME_ AS receiveTime, h.END_TIME_ AS operateTime, "
                + "COALESCE(h.IS_TEST_, 0) AS isTest, h.BUSINESS_STATUS_ AS taskStatus, "
                + "p.BUSINESS_ID_ AS instId, p.DEF_ID_ AS defId, p.FORM_ID_ AS formId, p.DATA_ID_ AS dataId, "
                + "p.TITLE_ AS title, p.STARTER_ AS starter, p.START_TIME_ AS startTime, "
                + "p.URGENCY_ AS urgency, p.BUSINESS_STATUS_ AS instStatus "
                + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE h.BIZ_ASSIGNEE_ = ? AND (h.IS_TEST_ = 0 OR h.IS_TEST_ IS NULL) "
                + "AND h.END_TIME_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "h");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "h", "END_TIME_", f);
        appendStatusFilter(sql, args, "h", f, false);
        sql.append(" ORDER BY h.END_TIME_ DESC, h.ID_ DESC LIMIT ? OFFSET ?");
        args.add(size);
        args.add(offset);
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

    /** 已办总数（与 {@link #donePageFromAct} 同口径，用于分页 total）。 */
    public long countDoneFromAct(Long assignee, WfTaskListFilter f) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ACT_HI_TASKINST h "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
            + "WHERE h.BIZ_ASSIGNEE_ = ? AND (h.IS_TEST_ = 0 OR h.IS_TEST_ IS NULL) "
            + "AND h.END_TIME_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(String.valueOf(assignee));
        appendActTenant(sql, args, "h");
        appendInstanceFilter(sql, args, "p", f);
        appendTimeRange(sql, args, "h", "END_TIME_", f);
        appendStatusFilter(sql, args, "h", f, false);
        Long n = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    /** 追加实例维度筛选（标题模糊 / 流程定义 / 表单），作用于 JOIN 的 {@code ACT_HI_PROCINST} 别名。 */
    /**
     * 待办判重（direct 模式）：该引擎任务是否已生成业务待办。
     *
     * <p><b>为什么必须有</b>：direct 模式下 {@code wf_task} 不落行，旧的
     * {@code WfInstanceServiceImpl#existsTask}（查 wf_task）恒返回 false，
     * 于是每轮 {@code advanceInternal} 都会重新调用 {@code insertTask} 生成<b>新的</b>
     * {@code BIZ_TASK_ID_} 并覆盖 ACT 行 —— 用户手上待办的 id 漂移，提交时报
     * 「任务不存在（可能该流程已被删除）」。此处按 {@code ACT_RU_TASK} 判重。</p>
     *
     * @param instId       业务实例 id（{@code ACT_HI_PROCINST.BUSINESS_ID_}）
     * @param engineTaskId 引擎任务 id
     * @param assignee     办理人（null = 不按人区分）
     * @param status       业务状态（null = 不按状态过滤；传 {@link WfTask#STATUS_TODO} 即待办）
     */
    public boolean existsTaskFromAct(Long instId, String engineTaskId, Long assignee, Integer status) {
        if (instId == null || engineTaskId == null || engineTaskId.isBlank()) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ACT_RU_TASK r "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
            + "WHERE p.BUSINESS_ID_ = ? AND r.ID_ = ? "
            // 必须要求「业务待办已生成」（BIZ_TASK_ID_ 非空）：引擎任务行在业务建待办之前就已存在，
            // 若只按引擎行判重，首轮 advance 会误判为「已存在」→ insertTask 被跳过 →
            // BIZ_TASK_ID_/BIZ_ASSIGNEE_ 等业务列永不写入 → 待办列表 id 为空、用户看不到待办。
            + "AND r.BIZ_TASK_ID_ IS NOT NULL AND r.BIZ_TASK_ID_ <> 0");
        List<Object> args = new ArrayList<>();
        args.add(instId);
        args.add(engineTaskId);
        if (assignee != null) {
            sql.append(" AND COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) = ?");
            args.add(String.valueOf(assignee));
        }
        if (status != null) {
            sql.append(" AND r.BUSINESS_STATUS_ = ?");
            args.add(WfTaskActDirectWriter.statusStr(status));
        }
        try {
            Long n = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
            return n != null && n > 0;
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 待办判重失败（按未命中处理）. {}", e.getMessage());
            return false;
        }
    }

    /**
     * 该实例下「已绑定业务待办」的引擎任务 id 集合（direct 模式的 A 类漂移判据）。
     *
     * <p>漂移定义：引擎有活跃 userTask，但其 ACT 行没有 {@code BIZ_TASK_ID_}（业务待办从未生成）
     * → 用户在待办列表里看不到它。direct 模式下 {@code BIZ_TASK_ID_} 恒非空（建待办即写），
     * 故本判据在 direct 模式下才成立。</p>
     */
    public java.util.Set<String> boundEngineTaskIdsFromAct(Long instId) {
        java.util.Set<String> bound = new java.util.LinkedHashSet<>();
        if (instId == null) {
            return bound;
        }
        String sql = "SELECT r.ID_ FROM ACT_RU_TASK r "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
            + "WHERE p.BUSINESS_ID_ = ? AND r.BIZ_TASK_ID_ IS NOT NULL AND r.BIZ_TASK_ID_ <> 0";
        try {
            for (String id : jdbcTemplate.queryForList(sql, String.class, instId)) {
                bound.add(id);
            }
        } catch (Exception e) {
            log.warn("[WfTaskActReader] 取已绑定引擎任务集合失败（漂移校验跳过）. {}", e.getMessage());
        }
        return bound;
    }

    /**
     * direct 模式通用读源：按条件查<b>活跃待办</b>（{@code ACT_RU_TASK}），还原为 {@link WfTask} 内存对象。
     *
     * <p>批次 1 把「写侧」单路径化后，仍有若干读点（流程测试域、自由流程、监管动作等）
     * 用 {@code taskMapper.selectList(instId/nodeKey/status)} 把 {@code wf_task} 当权威源 ——
     * direct 模式下该表为空 → 静默返回空集。本方法是这些读点的统一翻源落点。</p>
     *
     * @param instId   业务实例 id（{@code ACT_HI_PROCINST.BUSINESS_ID_}），null = 不限
     * @param nodeKey  节点 key，null = 不限
     * @param assignee 办理人，null = 不限
     * @param testOnly true = 只取测试态（{@code IS_TEST_=1}）；false/null = 不限
     */
    public List<WfTask> activeTasksFromAct(Long instId, String nodeKey, Long assignee, Boolean testOnly) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.ID_ AS engineTaskId, r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, r.ORIGINAL_USER_ AS originalUser, "
                + "r.IS_TEST_ AS isTest, r.SIGN_ORDER_ AS signOrder, r.VIEW_TIME_ AS viewTime, "
                + "r.TIMEOUT_HANDLED_ AS timeoutHandled, r.DUE_DATE_ AS dueTime, "
                + "r.CREATE_TIME_ AS receiveTime, r.TENANT_ID_ AS tenantId, p.BUSINESS_ID_ AS instId "
                + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE (r.BUSINESS_STATUS_ = 'TODO' OR r.BUSINESS_STATUS_ IS NULL)");
        List<Object> args = new ArrayList<>();
        if (instId != null) {
            sql.append(" AND p.BUSINESS_ID_ = ?");
            args.add(instId);
        }
        if (nodeKey != null) {
            sql.append(" AND r.TASK_DEF_KEY_ = ?");
            args.add(nodeKey);
        }
        if (assignee != null) {
            sql.append(" AND COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) = ?");
            args.add(String.valueOf(assignee));
        }
        if (Boolean.TRUE.equals(testOnly)) {
            sql.append(" AND r.IS_TEST_ = 1");
        }
        sql.append(" ORDER BY r.CREATE_TIME_ ASC, r.ID_ ASC");
        try {
            return jdbcTemplate.query(sql.toString(), (rs, i) -> taskRow(rs, WfTask.STATUS_TODO), args.toArray());
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 活跃待办查询失败（返回空集）. {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * direct 模式通用读源：按业务待办 id 重读<b>活跃待办</b>；已办结返回 {@code null}。
     *
     * <p>用于「逐条办理前重读状态」的场景（如流程测试交互式提交）：或签办结首条会关闭同节点
     * 其余待办，若不重读会误办已关闭的任务并整笔回滚。direct 模式下 {@code ACT_RU_TASK} 行
     * 在任务完成时被引擎删除，故查不到即代表「已非待办」，与旧 {@code selectById} 语义等价。</p>
     */
    public WfTask activeTaskFromAct(Long bizTaskId) {
        if (bizTaskId == null) {
            return null;
        }
        String sql = "SELECT r.ID_ AS engineTaskId, r.BIZ_TASK_ID_ AS bizTaskId, r.TASK_DEF_KEY_ AS nodeKey, "
            + "COALESCE(r.BIZ_ASSIGNEE_, r.ASSIGNEE_) AS assignee, r.ORIGINAL_USER_ AS originalUser, "
            + "r.IS_TEST_ AS isTest, r.SIGN_ORDER_ AS signOrder, r.VIEW_TIME_ AS viewTime, "
            + "r.TIMEOUT_HANDLED_ AS timeoutHandled, r.DUE_DATE_ AS dueTime, "
            + "r.CREATE_TIME_ AS receiveTime, r.TENANT_ID_ AS tenantId, p.BUSINESS_ID_ AS instId "
            + "FROM ACT_RU_TASK r LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
            + "WHERE r.BIZ_TASK_ID_ = ? AND (r.BUSINESS_STATUS_ = 'TODO' OR r.BUSINESS_STATUS_ IS NULL) LIMIT 1";
        try {
            List<WfTask> rows = jdbcTemplate.query(sql, (rs, i) -> taskRow(rs, WfTask.STATUS_TODO), bizTaskId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 待办重读失败（按已办结处理）. {}", e.getMessage());
            return null;
        }
    }

    /**
     * direct 模式通用读源：按实例查<b>已办任务数</b>（{@code ACT_HI_TASKINST.END_TIME_} 非空）。
     * 用于「本实例是否首次提交」这类判据（创建节点必填兜底）。
     */
    public long countDoneByInstanceFromAct(Long instId) {
        if (instId == null) {
            return 0L;
        }
        String sql = "SELECT COUNT(*) FROM ACT_HI_TASKINST h "
            + "LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
            + "WHERE p.BUSINESS_ID_ = ? AND h.END_TIME_ IS NOT NULL";
        try {
            Long n = jdbcTemplate.queryForObject(sql, Long.class, instId);
            return n == null ? 0L : n;
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 已办任务计数失败（按 0 处理）. {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * direct 模式通用读源：按实例 + 节点查<b>全部任务（含已办）</b>，供「操作者」面板展示。
     * 与 {@link #activeTasksFromAct} 的区别：历史行保留在 {@code ACT_HI_TASKINST}，能显示已办过的人。
     */
    public List<WfTask> allTasksByInstanceFromAct(Long instId, String nodeKey) {
        if (instId == null) {
            return new ArrayList<>();
        }
        StringBuilder sql = new StringBuilder(
            "SELECT h.ID_ AS engineTaskId, h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
                + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, h.ORIGINAL_USER_ AS originalUser, "
                + "h.IS_TEST_ AS isTest, h.SIGN_ORDER_ AS signOrder, h.VIEW_TIME_ AS viewTime, "
                + "h.TIMEOUT_HANDLED_ AS timeoutHandled, h.DUE_DATE_ AS dueTime, "
                + "h.START_TIME_ AS receiveTime, h.TENANT_ID_ AS tenantId, p.BUSINESS_ID_ AS instId "
                + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ?");
        List<Object> args = new ArrayList<>();
        args.add(instId);
        if (nodeKey != null) {
            sql.append(" AND h.TASK_DEF_KEY_ = ?");
            args.add(nodeKey);
        }
        sql.append(" ORDER BY h.START_TIME_ ASC, h.ID_ ASC");
        try {
            return jdbcTemplate.query(sql.toString(), (rs, i) -> taskRow(rs, null), args.toArray());
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 实例任务查询失败（返回空集）. {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * direct 模式：取实例下<b>待收尾的残留待办</b>（待办 {@code TODO} / 协办 {@code COADJUTANT}、
     * 且引擎侧尚未结束），供撤销/撤回/不通过/归档时的关待办循环使用。
     *
     * <p>与 {@link #activeTasksFromAct} 的区别：读 {@code ACT_HI_TASKINST}（含协办态、
     * 以 {@code END_TIME_ IS NULL} 判定未结束），而前者只读 {@code ACT_RU_TASK} 的纯待办态。</p>
     */
    public List<WfTask> residualTasksFromAct(Long instId) {
        if (instId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT h.ID_ AS engineTaskId, h.BIZ_TASK_ID_ AS bizTaskId, h.TASK_DEF_KEY_ AS nodeKey, "
            + "COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee, h.ORIGINAL_USER_ AS originalUser, "
            + "h.IS_TEST_ AS isTest, h.SIGN_ORDER_ AS signOrder, h.VIEW_TIME_ AS viewTime, "
            + "h.TIMEOUT_HANDLED_ AS timeoutHandled, h.DUE_DATE_ AS dueTime, "
            + "h.START_TIME_ AS receiveTime, h.TENANT_ID_ AS tenantId, p.BUSINESS_ID_ AS instId "
            + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
            + "WHERE p.BUSINESS_ID_ = ? AND h.END_TIME_ IS NULL "
            + "AND (h.BUSINESS_STATUS_ IN ('TODO', 'COADJUTANT') OR h.BUSINESS_STATUS_ IS NULL) "
            + "ORDER BY h.START_TIME_ ASC, h.ID_ ASC";
        try {
            return jdbcTemplate.query(sql, (rs, i) -> taskRow(rs, WfTask.STATUS_TODO), instId);
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 残留待办查询失败（返回空集）. {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * direct 模式：取实例<b>最后一位实际办理人</b>（最近一条已办任务的办理人），供归档人兜底。
     *
     * <p>旧实现查 {@code wf_task.status IN (DONE, FINISHED)} → direct 模式恒空 → 回退发起人，
     * 归档意见的「归档人」变成发起人而非实际最后办理人。</p>
     *
     * @return 办理人 id；无已办记录返回 {@code null}（由调用方继续兜底到发起人）
     */
    public Long lastDoneOperatorFromAct(Long instId) {
        if (instId == null) {
            return null;
        }
        String sql = "SELECT COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee "
            + "FROM ACT_HI_TASKINST h LEFT JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
            + "WHERE p.BUSINESS_ID_ = ? AND h.END_TIME_ IS NOT NULL "
            + "AND COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) IS NOT NULL "
            + "ORDER BY h.END_TIME_ DESC, h.ID_ DESC LIMIT 1";
        try {
            List<String> rows = jdbcTemplate.queryForList(sql, String.class, instId);
            if (rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank()) {
                return null;
            }
            return Long.valueOf(rows.get(0).trim());
        } catch (Exception e) {
            log.warn("[WfTaskActReader] ACT 最后办理人查询失败（回退发起人）. {}", e.getMessage());
            return null;
        }
    }

    /** ACT 行 → {@link WfTask}（不落库）；status 为 null 时按 BUSINESS_STATUS_ 反解（调用方 SQL 需带该列） */
    private static WfTask taskRow(ResultSet rs, Integer status) throws SQLException {
        WfTask t = new WfTask();
        t.setId(getLong(rs, "bizTaskId"));
        t.setEngineTaskId(rs.getString("engineTaskId"));
        t.setNodeKey(rs.getString("nodeKey"));
        t.setAssignee(getLong(rs, "assignee"));
        t.setOriginalUser(getLong(rs, "originalUser"));
        t.setIsTest(getInt(rs, "isTest"));
        t.setSignOrder(getInt(rs, "signOrder"));
        t.setViewTime(rs.getTimestamp("viewTime"));
        t.setTimeoutHandled(getInt(rs, "timeoutHandled"));
        t.setDueTime(rs.getTimestamp("dueTime"));
        t.setReceiveTime(rs.getTimestamp("receiveTime"));
        t.setInstId(getLong(rs, "instId"));
        t.setTenantId(rs.getString("tenantId"));
        t.setStatus(status);
        return t;
    }

    private static void appendInstanceFilter(StringBuilder sql, List<Object> args, String pAlias, WfTaskListFilter f) {
        if (f == null) {
            return;
        }
        if (f.getTitle() != null && !f.getTitle().isBlank()) {
            sql.append(" AND ").append(pAlias).append(".TITLE_ LIKE ? ESCAPE '\\\\'");
            args.add("%" + escapeLike(f.getTitle()) + "%");
        }
        if (f.getDefId() != null) {
            sql.append(" AND ").append(pAlias).append(".DEF_ID_ = ?");
            args.add(f.getDefId());
        }
        if (f.getFormId() != null) {
            sql.append(" AND ").append(pAlias).append(".FORM_ID_ = ?");
            args.add(f.getFormId());
        }
        // 显式实例范围（公文维度等）：pAlias.BUSINESS_ID_ IN (...)。空集合不加（= 不限定）。
        if (f.getInstIds() != null && !f.getInstIds().isEmpty()) {
            sql.append(" AND ").append(pAlias).append(".BUSINESS_ID_ IN (");
            for (int i = 0; i < f.getInstIds().size(); i++) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append("?");
                args.add(f.getInstIds().get(i));
            }
            sql.append(")");
        }
    }

    /** 追加时间范围筛选（待办=接收时间列；已办=处理时间列）。 */
    private static void appendTimeRange(StringBuilder sql, List<Object> args, String alias, String col, WfTaskListFilter f) {
        if (f == null) {
            return;
        }
        if (f.getBeginTime() != null) {
            sql.append(" AND ").append(alias).append(".").append(col).append(" >= ?");
            args.add(f.getBeginTime());
        }
        if (f.getEndTime() != null) {
            sql.append(" AND ").append(alias).append(".").append(col).append(" <= ?");
            args.add(f.getEndTime());
        }
    }

    /**
     * 追加前端列「状态」筛选（分页下推，保证 records 与 total 同口径）。
     *
     * <p>ACT 侧 {@code BUSINESS_STATUS_} 存的是业务状态字符串（{@link #taskStatus} 的反解来源），
     * 故需把前端的数字状态码反向映射；待办恒为 status=0，筛选其它值直接返回空。</p>
     */
    private static void appendStatusFilter(StringBuilder sql, List<Object> args, String alias,
                                           WfTaskListFilter f, boolean todo) {
        Integer status = f == null ? null : f.getStatus();
        if (status == null) {
            return;
        }
        if (todo) {
            if (status != WfTask.STATUS_TODO) {
                sql.append(" AND 1 = 0");
            }
            return;
        }
        String code = statusCode(status);
        if (code == null) {
            sql.append(" AND 1 = 0");
            return;
        }
        // 存量行（双写前）BUSINESS_STATUS_ 为 NULL，taskStatus() 反解为 DONE(2)。
        // 筛选「已办」时必须把这些行一并计入，否则筛选结果与未筛选列表的展示口径不一致。
        if (status == WfTask.STATUS_DONE) {
            sql.append(" AND (").append(alias).append(".BUSINESS_STATUS_ = ? OR ")
                .append(alias).append(".BUSINESS_STATUS_ IS NULL)");
        } else {
            sql.append(" AND ").append(alias).append(".BUSINESS_STATUS_ = ?");
        }
        args.add(code);
    }

    /** 数字状态 → ACT_HI_TASKINST.BUSINESS_STATUS_ 字符串（{@link #taskStatus} 的反向映射）。 */
    private static String statusCode(int status) {
        if (status == WfTask.STATUS_TODO) return "TODO";
        if (status == WfTask.STATUS_DONE) return "DONE";
        if (status == WfTask.STATUS_FINISHED) return "FINISHED";
        if (status == WfTask.STATUS_AUTO_SUBMIT) return "AUTO_SUBMIT";
        if (status == WfTask.STATUS_COADJUTANT) return "COADJUTANT";
        if (status == WfTask.STATUS_CIRCULATE) return "CIRCULATE";
        if (status == WfTask.STATUS_READ) return "READ";
        return null;
    }

    /** 转义 LIKE 通配符（% _ \），配合 {@code ESCAPE '\\'} 实现精确子串匹配。 */
    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 角标计数（顶栏待办红点 / 监控）：待办读 {@code ACT_RU_TASK}、已办读 {@code ACT_HI_TASKINST}。
     *
     * <p>与列表同口径保留【兜底】：ACT 表达不了的（存量 N:1 会签 / 孤儿 / 无引擎任务的合成待办）
     * 仍按 {@code wf_task.id NOT IN (ACT 已覆盖的 BIZ_TASK_ID_)} 补计，保证角标数字与翻源前一致。</p>
     *
     * <p><b>已办口径（2026-11-08 对齐）</b>：{@code wf_task} 兜底计数改为
 * {@code status IN (2,4,6,7,8,11)}，与 {@code WfTaskServiceImpl.done()} 的 statuses 六态一致，
 * 避免角标数与列表条数不一致。原写法 {@code status <> 0} 会把非枚举状态计入角标。</p>
     *
     * <p>⚠️ 前提：会签/或签/依次须已<b>全面下沉引擎多实例</b>，否则引擎任务生命周期与 blade 任务状态
     * 不一致（自研 {@code closeSiblings} 只改 {@code wf_task.status}、不完成引擎任务），
     * 会导致<b>待办多算</b>。详见 {@code doc/md/去wf_表-任务读源翻源上线检查.md} §4.5。</p>
     */
    public java.util.Map<String, Long> countFromAct(Long assignee) {
        String a = String.valueOf(assignee);

        // ① ACT 待办
        StringBuilder todoSql = new StringBuilder("SELECT COUNT(*) FROM ACT_RU_TASK "
            + "WHERE BIZ_ASSIGNEE_ = ? AND (IS_TEST_ = 0 OR IS_TEST_ IS NULL)");
        List<Object> todoArgs = new ArrayList<>();
        todoArgs.add(a);
        appendActTenant(todoSql, todoArgs, null);
        long todoAct = countOne(todoSql.toString(), todoArgs.toArray());

        // ② ACT 已办
        StringBuilder doneSql = new StringBuilder("SELECT COUNT(*) FROM ACT_HI_TASKINST "
            + "WHERE BIZ_ASSIGNEE_ = ? AND (IS_TEST_ = 0 OR IS_TEST_ IS NULL) "
            + "AND END_TIME_ IS NOT NULL");
        List<Object> doneArgs = new ArrayList<>();
        doneArgs.add(a);
        appendActTenant(doneSql, doneArgs, null);
        long doneAct = countOne(doneSql.toString(), doneArgs.toArray());

        // ③ 扩展任务段：读 wf_ext_task（去 wf_ 表 · 2026-11-09）。
        //
        // 【扩展任务开关】关闭时（纯 ACT 单源）：整段跳过，角标只数 ACT_*。
        //   必须与列表侧（WfTaskServiceImpl#list/page 的 extTaskQuery）同开关、同 type 判据，
        //   否则会出现「角标 25、列表只有 24」的割裂。
        //
        // 【裸 SQL 纪律】JdbcTemplate 绕过 MyBatis-Plus 的 @TableLogic 与租户插件，
        //   故必须显式 is_deleted = 0 与 tenant_id 条件（appendWfTenant 别名 t）。
        long todoResidue = 0L;
        long doneResidue = 0L;
        if (residueMerge.isEnabled()) {
            // 待办段：可办理扩展待办（加签协办/退回重提交），且自身 status=0
            StringBuilder todoResSql = new StringBuilder("SELECT COUNT(*) FROM wf_ext_task t "
                + "WHERE t.assignee = ? AND t.status = 0 AND t.is_test = 0"
                + " AND t.is_deleted = 0"
                + " AND t.type IN (" + EXT_TODO_TYPE_SQL + ")"
                + " AND t.type <> " + EXT_DECORATION_TYPE
                // 扩展任务与 ACT 结构性无交集（引擎无原生物），无需 ACT 排重；
                // 列表侧 extTaskQuery 采用同一口径，保证角标与列表条数一致。
                // 【草稿移出待办 · 方案 A】排除草稿实例的合成待办，与列表侧同口径
                + " AND NOT EXISTS (SELECT 1 FROM wf_instance i WHERE i.id = t.inst_id "
                + "AND i.status = " + WfInstance.STATUS_DRAFT + " AND i.is_deleted = 0)");
            List<Object> todoResArgs = new ArrayList<>();
            todoResArgs.add(assignee);
            appendWfTenant(todoResSql, todoResArgs, "t");
            todoResidue = countOne(todoResSql.toString(), todoResArgs.toArray());

            // 已办段：协办/抄送/传阅/已读（未读也进已办侧，与原 status=7/8/11 行为一致）
            StringBuilder doneResSql = new StringBuilder("SELECT COUNT(*) FROM wf_ext_task t "
                + "WHERE t.assignee = ? AND t.is_test = 0"
                + " AND t.is_deleted = 0"
                + " AND t.type IN (" + EXT_DONE_TYPE_SQL + ")"
                + " AND t.type <> " + EXT_DECORATION_TYPE
                // 【草稿移出待办 · 方案 A】口径对称，防止将来出现草稿已办态
                + " AND NOT EXISTS (SELECT 1 FROM wf_instance i WHERE i.id = t.inst_id "
                + "AND i.status = " + WfInstance.STATUS_DRAFT + " AND i.is_deleted = 0)");
            List<Object> doneResArgs = new ArrayList<>();
            doneResArgs.add(assignee);
            appendWfTenant(doneResSql, doneResArgs, "t");
            doneResidue = countOne(doneResSql.toString(), doneResArgs.toArray());
        }

        java.util.Map<String, Long> r = new java.util.LinkedHashMap<>();
        r.put("todo", todoAct + todoResidue);
        r.put("done", doneAct + doneResidue);
        return r;
    }

    /**
     * 按 {@code BIZ_TASK_ID_}（blade 业务任务ID）反查引擎任务ID {@code ID_}。
     *
     * <p>用于审批意见/签章双写等「只有 wfTaskId、需要 engineTaskId」的链路，
     * 使其不再依赖 {@code wf_task.engine_task_id}（去 wf_ 表 · 2026-11-09）。
     * 先查运行态 {@code ACT_RU_TASK}，再查历史 {@code ACT_HI_TASKINST}；查不到返回 null。</p>
     */
    public String findEngineTaskIdByBizId(Long bizTaskId) {
        if (bizTaskId == null) {
            return null;
        }
        String id = String.valueOf(bizTaskId);
        List<String> ru = jdbcTemplate.queryForList(
            "SELECT ID_ FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ = ? LIMIT 1", String.class, id);
        if (!ru.isEmpty()) {
            return ru.get(0);
        }
        List<String> hi = jdbcTemplate.queryForList(
            "SELECT ID_ FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ = ? LIMIT 1", String.class, id);
        return hi.isEmpty() ? null : hi.get(0);
    }

    /**
     * ACT 读源：<b>超时实例ID集合</b>（blade 实例维度）。
     *
     * <p>口径与 wf 侧严格对齐（翻源不改行为）：{@code wf_task.status=0 AND due_time < now}
     * → {@code ACT_RU_TASK.DUE_DATE_ < now}（{@code DUE_DATE_} 是 {@code wf_task.due_time} 的投影）。
     * 实例维度靠 {@code ACT_RU_TASK.PROC_INST_ID_ → ACT_HI_PROCINST.ID_ → BUSINESS_ID_}；
     * {@code BUSINESS_ID_} 即原 {@code wf_instance.id} 雪花值（见 {@code ActHiProcinst#businessId}）。</p>
     *
     * <p>⚠️ <b>刻意不加 {@code IS_TEST_} 过滤</b>：wf 侧原查询（{@code WfInstanceServiceImpl#enrichMonitor}
     * 与 {@code #monitorStats}）也没有过滤测试态 —— 翻源必须<b>严格同口径</b>，否则监控列表与看板会分裂。</p>
     *
     * @param now          判定时刻
     * @param scopeInstIds 限定实例范围（null/空 = 不限定；act 读源传「非删除实例ID」集合）
     */
    public List<Long> overdueInstanceIdsFromAct(Date now, Collection<Long> scopeInstIds) {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT p.BUSINESS_ID_ FROM ACT_RU_TASK r "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE r.DUE_DATE_ < ?");
        List<Object> args = new ArrayList<>();
        args.add(now);
        appendActTenant(sql, args, "r");
        appendInstScope(sql, args, scopeInstIds);
        return jdbcTemplate.query(sql.toString(), (rs, i) -> {
            long v = rs.getLong(1);
            return rs.wasNull() ? null : v;
        }, args.toArray()).stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
    }

    /**
     * ACT 读源：<b>超时实例数</b>（去重计数，与 {@link #overdueInstanceIdsFromAct} 同口径）。
     * 用于监控异常卡片与看板统计（避免把实例ID全量拉进内存）。
     */
    public long countOverdueInstancesFromAct(Date now, Collection<Long> scopeInstIds) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT p.BUSINESS_ID_) FROM ACT_RU_TASK r "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE r.DUE_DATE_ < ?");
        List<Object> args = new ArrayList<>();
        args.add(now);
        appendActTenant(sql, args, "r");
        appendInstScope(sql, args, scopeInstIds);
        Long n = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    /**
     * ACT 读源：<b>实例 → 当前待办 taskId</b>（{@code BIZ_TASK_ID_}，即 {@code wf_task.id}）。
     *
     * <p>供监控页的任务级监管动作（转办 {@code /task/{id}/forward}、催办 {@code /task/{id}/urge}）
     * 定位可操作目标。多待办场景取<b>最小 id</b> 的一条，与 wf 侧
     * {@code orderByAsc(WfTask::getId)} 取第一条<b>同口径</b>。</p>
     *
     * <p>草稿实例天然不在此结果里（草稿从未启动引擎、{@code ACT_RU_TASK} 无其行），
     * 与 wf 侧显式排除草稿的意图一致。</p>
     */
    public Map<Long, Long> pendingTaskIdsByInstanceFromAct(Collection<Long> bladeInstIds) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.BUSINESS_ID_, MIN(r.BIZ_TASK_ID_) FROM ACT_RU_TASK r "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE r.BIZ_TASK_ID_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        appendActTenant(sql, args, "r");
        appendInstScope(sql, args, bladeInstIds);
        sql.append(" GROUP BY p.BUSINESS_ID_");
        Map<Long, Long> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query(sql.toString(), (rs, i) -> {
            long instId = rs.getLong(1);
            long taskId = rs.getLong(2);
            if (!rs.wasNull()) {
                out.put(instId, taskId);
            }
            return null;
        }, args.toArray());
        return out;
    }

    /**
     * ACT 读源：本实例「<b>出现过</b>」的节点 Key 集合。
     *
     * <p>严格对齐 wf 侧 {@code WfInstanceServiceImpl#loadPassedNodeKeys} 的口径 ——
     * <b>不加 status 过滤</b>（该方法就是「本实例出现过的所有 node_key」，含当前待办节点、
     * 协办/抄送/传阅节点），翻源不得改变语义。</p>
     *
     * <p>取 {@code ACT_HI_TASKINST ∪ ACT_RU_TASK} 的 {@code TASK_DEF_KEY_}：
     * Flowable 在任务创建时即写 HI 表（{@code END_TIME_} 为空）、完成时更新同一行 → HI 表覆盖全程；
     * 但仍 UNION RU 表以防引擎侧行为差异，两者取并集不会引入多余节点。</p>
     *
     * <p>⚠️ 与 wf 侧的一处<b>已知差异</b>：草稿合成待办（{@code node_key='Task_Create'}、无引擎任务）
     * 在 wf 侧会进此集合，ACT 侧没有 → 草稿实例的集合会少该节点。但草稿从未启动引擎，
     * 不存在归档/回退/监控动作场景；且创建节点在 {@code recallTargetPassed} 里单独放行，实际无影响。</p>
     */
    public java.util.Set<String> passedNodeKeysFromAct(Long bladeInstId) {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (bladeInstId == null) {
            return set;
        }
        // HI 段（历史任务：含已完成与仍在办的行）
        StringBuilder hi = new StringBuilder(
            "SELECT h.TASK_DEF_KEY_ FROM ACT_HI_TASKINST h "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND h.TASK_DEF_KEY_ IS NOT NULL");
        List<Object> hiArgs = new ArrayList<>();
        hiArgs.add(bladeInstId);
        appendActTenant(hi, hiArgs, "h");
        for (String k : jdbcTemplate.query(hi.toString(), (rs, i) -> rs.getString(1), hiArgs.toArray())) {
            if (k != null && !k.isBlank()) {
                set.add(k);
            }
        }
        // RU 段（运行中任务）：与 HI 段取并集，防引擎侧行为差异导致漏节点
        StringBuilder ru = new StringBuilder(
            "SELECT r.TASK_DEF_KEY_ FROM ACT_RU_TASK r "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND r.TASK_DEF_KEY_ IS NOT NULL");
        List<Object> ruArgs = new ArrayList<>();
        ruArgs.add(bladeInstId);
        appendActTenant(ru, ruArgs, "r");
        for (String k : jdbcTemplate.query(ru.toString(), (rs, i) -> rs.getString(1), ruArgs.toArray())) {
            if (k != null && !k.isBlank()) {
                set.add(k);
            }
        }
        return set;
    }

    /**
     * ACT 读源：本实例「<b>已办（DONE）</b>」的节点 Key，按办理完成时间<b>倒序</b>。
     *
     * <p>对齐 wf 侧 {@code WfInstanceServiceImpl#archivedCandidates}：
     * {@code status = WfTask.STATUS_DONE(2)} → ACT 侧 {@code BUSINESS_STATUS_='DONE'}（映射见
     * {@code WfTaskActWriter#statusStr}），并要求 {@code END_TIME_ IS NOT NULL}（已办结）。</p>
     *
     * <p>排序差异（可接受）：wf 侧按 {@code wf_task.id} 倒序（雪花 ID ≈ 创建序），
     * ACT 侧按 {@code END_TIME_} 倒序（真实办理完成时间，更贴合「最近一次办理由近及远」的语义）。
     * 最终输出是节点集合，顺序仅影响候选弹窗的展示次序。</p>
     */
    public java.util.LinkedHashSet<String> doneNodeKeysFromAct(Long bladeInstId) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        if (bladeInstId == null) {
            return out;
        }
        StringBuilder sql = new StringBuilder(
            "SELECT h.TASK_DEF_KEY_ FROM ACT_HI_TASKINST h "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND h.BUSINESS_STATUS_ = 'DONE' "
                + "AND h.END_TIME_ IS NOT NULL AND h.TASK_DEF_KEY_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(bladeInstId);
        appendActTenant(sql, args, "h");
        // ⚠️ ORDER BY 必须放在租户条件之后：appendActTenant 是「末尾追加」，
        //    若 ORDER BY 提前拼好，租户条件会被追加到 ORDER BY 之后 → SQL 语法错误。
        sql.append(" ORDER BY h.END_TIME_ DESC");
        for (String k : jdbcTemplate.query(sql.toString(), (rs, i) -> rs.getString(1), args.toArray())) {
            if (k != null && !k.isBlank()) {
                out.add(k);
            }
        }
        return out;
    }

    /**
     * ACT 读源：取某实例某节点的「最近办结人」（子流程提醒接收人「节点操作者本人」用）。
     *
     * <p>等价于 wf_task 口径：同实例、同节点、status=DONE、operate_time 倒序首条的 assignee。
     * ACT 侧取 {@code ACT_HI_TASKINST}（{@code BUSINESS_STATUS_='DONE'} 且 {@code END_TIME_} 非空）
     * 经 {@code ACT_HI_PROCINST.BUSINESS_ID_} 关联 blade 实例，按 {@code END_TIME_} 倒序取首条
     * {@code COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_)}。</p>
     */
    public Long nodeOperatorFromAct(Long bladeInstId, String nodeKey) {
        if (bladeInstId == null || nodeKey == null || nodeKey.isBlank()) {
            return null;
        }
        StringBuilder sql = new StringBuilder(
            "SELECT COALESCE(h.BIZ_ASSIGNEE_, h.ASSIGNEE_) AS assignee FROM ACT_HI_TASKINST h "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND h.TASK_DEF_KEY_ = ? "
                + "AND h.BUSINESS_STATUS_ = 'DONE' AND h.END_TIME_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(bladeInstId);
        args.add(nodeKey);
        appendActTenant(sql, args, "h");
        sql.append(" ORDER BY h.END_TIME_ DESC LIMIT 1");
        List<Long> rs = jdbcTemplate.query(sql.toString(), (rs2, i) -> getLong(rs2, "assignee"), args.toArray());
        return rs.isEmpty() ? null : rs.get(0);
    }

    /**
     * ACT 读源：本实例「当前活跃（未办）任务」对应的 {@code wf_task.id} 集合。
     *
     * <p>供监管动作（强制收回 / 监控回退）关待办前的对账：紧随其后的 {@code moveActivity} 会删除这些
     * {@code ACT_RU_TASK} 行，须在删除前先把 {@code wf_task} 台账对应行收尾为 DONE 并
     * {@code sync} 到 {@code ACT_HI_TASKINST}（否则 HI 行的 {@code BUSINESS_STATUS_} 会停在 TODO 漂移）。</p>
     *
     * <p>取 {@code ACT_RU_TASK r JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_}
     * where {@code p.BUSINESS_ID_ = bladeInstId} 且 {@code r.BIZ_TASK_ID_ IS NOT NULL}
     * （纯引擎任务无 {@code wf_task} 对应，跳过）。{@code BIZ_TASK_ID_} 是 bigint 列。</p>
     */
    public java.util.LinkedHashSet<Long> pendingCloseBizIdsFromAct(Long bladeInstId) {
        java.util.LinkedHashSet<Long> set = new java.util.LinkedHashSet<>();
        if (bladeInstId == null) {
            return set;
        }
        StringBuilder sql = new StringBuilder(
            "SELECT r.BIZ_TASK_ID_ FROM ACT_RU_TASK r "
                + "JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND r.BIZ_TASK_ID_ IS NOT NULL");
        List<Object> args = new ArrayList<>();
        args.add(bladeInstId);
        appendActTenant(sql, args, "r");
        for (Object v : jdbcTemplate.query(sql.toString(), (rs, i) -> rs.getObject(1), args.toArray())) {
            if (v != null) {
                try {
                    set.add(Long.parseLong(String.valueOf(v)));
                } catch (NumberFormatException ignore) {
                    // bigint 列不会是非数字；异常值跳过，不污染待关闭集合
                }
            }
        }
        return set;
    }

    /**
     * ACT 读源：判断 {@code assignee} 是否为本实例的「办理参与人」（ACT_RU_TASK / ACT_HI_TASKINST 的
     * {@code ASSIGNEE_} 或 blade 扩展列 {@code BIZ_ASSIGNEE_} 命中）。
     *
     * <p>供 {@code WfInstanceServiceImpl#canVisible} 的「参与人可见」判定（办理人分支）翻源。
     * 抄送/传阅/退回发起人等<b>纯台账行</b>（引擎侧无任务、{@code WfTaskActWriter#sync} 会跳过）由调用方回退查
     * {@code wf_task} 兜底，本方法只覆盖「引擎侧有记录」的办理人。</p>
     */
    public boolean isParticipantFromAct(Long bladeInstId, Long assignee) {
        if (bladeInstId == null || assignee == null) {
            return false;
        }
        String a = String.valueOf(assignee);
        // 运行中任务（RU）
        StringBuilder ru = new StringBuilder(
            "SELECT 1 FROM ACT_RU_TASK r JOIN ACT_HI_PROCINST p ON p.ID_ = r.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND (r.ASSIGNEE_ = ? OR r.BIZ_ASSIGNEE_ = ?)");
        List<Object> ruArgs = new ArrayList<>();
        ruArgs.add(bladeInstId);
        ruArgs.add(a);
        ruArgs.add(a);
        appendActTenant(ru, ruArgs, "r");
        if (!jdbcTemplate.query(ru.toString(), (rs, i) -> 1, ruArgs.toArray()).isEmpty()) {
            return true;
        }
        // 已办任务（HI）
        StringBuilder hi = new StringBuilder(
            "SELECT 1 FROM ACT_HI_TASKINST h JOIN ACT_HI_PROCINST p ON p.ID_ = h.PROC_INST_ID_ "
                + "WHERE p.BUSINESS_ID_ = ? AND (h.ASSIGNEE_ = ? OR h.BIZ_ASSIGNEE_ = ?)");
        List<Object> hiArgs = new ArrayList<>();
        hiArgs.add(bladeInstId);
        hiArgs.add(a);
        hiArgs.add(a);
        appendActTenant(hi, hiArgs, "h");
        return !jdbcTemplate.query(hi.toString(), (rs, i) -> 1, hiArgs.toArray()).isEmpty();
    }

    /** 追加实例范围约束（{@code BUSINESS_ID_ IN (...)}）；范围为空则不加（= 不限定）。 */
    private static void appendInstScope(StringBuilder sql, List<Object> args, Collection<Long> scopeInstIds) {
        if (scopeInstIds == null || scopeInstIds.isEmpty()) {
            return;
        }
        sql.append(" AND p.BUSINESS_ID_ IN (")
            .append(String.join(",", java.util.Collections.nCopies(scopeInstIds.size(), "?")))
            .append(")");
        args.addAll(scopeInstIds);
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
     * 按业务任务ID 还原 {@link WfTask}（填 {@code canOperate} 消费的 id/nodeKey/assignee/originalUser/status），
     * 供 {@code render} 在 ACT 读源下判定「当前用户是否本节点待办人」。找不到返回 null。
     *
     * <p><b>必须读 {@code ORIGINAL_USER_}</b>（2026-11-08 修复）：{@code WfAuthUtil#canOperateTask} 的判定是
     * {@code me.equals(assignee) || me.equals(originalUser)}，其中 {@code originalUser} 是
     * <b>代理代办的原办理人</b>（转办时写入，见 {@code WfTaskServiceImpl#forward}）。
     * 此前本方法没 SELECT 这一列 → 读源=act 时 {@code originalUser} 恒 null →
     * 代理代办的人既看不到操作按钮（前端 readonly 判定），后端 {@code requireOperateTask} 还会直接 403。
     * ACT 侧该列由 {@code WfTaskActWriter} 双写（VARCHAR 存数字ID，用 {@code getLong} 解析）。</p>
     */
    public WfTask toWfTaskByBizIdFromAct(Long bizTaskId) {
        if (bizTaskId == null) {
            return null;
        }
        WfTask ru = jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee, "
                + "ORIGINAL_USER_ AS originalUser "
                + "FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            (rs, i) -> {
                WfTask t = new WfTask();
                t.setId(bizTaskId);
                t.setNodeKey(rs.getString("nodeKey"));
                t.setAssignee(getLong(rs, "assignee"));
                t.setOriginalUser(getLong(rs, "originalUser"));
                t.setStatus(WfTask.STATUS_TODO);
                return t;
            }, String.valueOf(bizTaskId)).stream().findFirst().orElse(null);
        if (ru != null) {
            return ru;
        }
        return jdbcTemplate.query(
            "SELECT TASK_DEF_KEY_ AS nodeKey, COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) AS assignee, "
                + "ORIGINAL_USER_ AS originalUser, BUSINESS_STATUS_ AS taskStatus "
                + "FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ = ? LIMIT 1",
            (rs, i) -> {
                WfTask t = new WfTask();
                t.setId(bizTaskId);
                t.setNodeKey(rs.getString("nodeKey"));
                t.setAssignee(getLong(rs, "assignee"));
                t.setOriginalUser(getLong(rs, "originalUser"));
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
