package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.mapper.WorkflowBillFieldMapper;
import org.springblade.formmode.service.IApprovalTriggerService;
import org.springblade.formmode.service.IFormDataService;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.utils.BizValueCoercer;
import org.springblade.formmode.utils.TableNameContextHolder;
import org.springblade.formmode.utils.TableNameUtil;
import org.springblade.formmode.vo.DataScope;
import org.springblade.formmode.vo.FieldPermVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 表单数据管理服务实现
 *
 * 核心逻辑对应 ecology 的 ModeDataManager.saveModeData()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormDataServiceImpl implements IFormDataService {

    /** 动态表名白名单：字母/数字/下划线/负号，避免拼 SQL 被注入。
     *  允许负号是因为表单表名形如 formtable_main_{-N}（A 方案新建表单 billid 为负数，
     *  表名天然带负号）；所有表名均反引号包裹，'-' 非 SQL 元字符，放开无注入风险。 */
    private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final IFormModeService formModeService;
    private final IApprovalTriggerService approvalTriggerService;
    private final IFormRightService formRightService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFormData(FormDataDTO formDataDTO) {
        ModeInfo modeInfo = modeInfoMapper.selectById(formDataDTO.getModeid());
        if (modeInfo == null) {
            throw new RuntimeException("模块不存在: " + formDataDTO.getModeid());
        }

        // billid 对齐 DDL bigint：FormDataDTO.billid 与 modeinfo.billid 均为 Long
        Long billId = formDataDTO.getBillid() != null ? formDataDTO.getBillid() : modeInfo.getBillid();
        if (billId == null) {
            throw new RuntimeException("模块未关联表单");
        }

        String mainTableName = TableNameUtil.getMainTableName(billId.longValue());
        String currentDate = LocalDate.now().toString();
        String currentTime = LocalTime.now().toString().substring(0, 8);

        // 获取字段定义
        List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(billId);

        // 操作类型判断
        String src = formDataDTO.getSrc();
        if (src == null) src = "save";

        // 删除操作
        if ("del".equals(src) && formDataDTO.getDataid() != null) {
            Long deletedId = TableNameContextHolder.executeWithTableName(mainTableName, () -> {
                String deleteSql = "DELETE FROM `" + mainTableName + "` WHERE id = ?";
                jdbcTemplate.update(deleteSql, formDataDTO.getDataid());
                return formDataDTO.getDataid();
            });
            fireApprovalTrigger(formDataDTO.getModeid(), deletedId, "del", Collections.emptyMap());
            return deletedId;
        }

        // 解析字段值
        Map<String, Object> fieldValues = formDataDTO.getFieldValues();
        if (fieldValues == null) fieldValues = new HashMap<>();

        // ── 字段级权限裁剪（P2-2 Step3：防越权写隐藏/只读字段）──
        // 解析权限所属模块：优先 DTO.modeid，否则由 formId/billId 反查；
        // 拉取失败 / 无配置 → fail-open，不裁剪（保持向后兼容，零权限配置时行为不变）。
        Map<String, FieldPermVO> permMap = new HashMap<>();
        Long permModeId = formDataDTO.getModeid();
        if (permModeId == null) {
            try {
                Long fb = formDataDTO.getBillid() != null ? formDataDTO.getBillid() : billId;
                permModeId = formRightService.resolveModeIdByFormId(fb);
            } catch (Exception e) {
                log.warn("[formmode] 字段权限：模块反查失败，跳过裁剪. formId={}, err={}",
                    formDataDTO.getBillid(), e.getMessage());
            }
        }
        if (permModeId != null) {
            try {
                List<FieldPermVO> perms = formRightService.listFieldPerm(permModeId);
                if (perms != null) {
                    for (FieldPermVO p : perms) {
                        if (p == null || p.getFieldName() == null) continue;
                        permMap.put(p.getFieldName(), p);
                    }
                }
                // 反向索引：明细行 key 多为 dbName，故把权限按 dbName 也索引一份
                for (WorkflowBillField f : fields) {
                    FieldPermVO p = permMap.get(f.getFieldname());
                    if (p != null && f.getFielddbname() != null) {
                        permMap.put(f.getFielddbname().toLowerCase(), p);
                    }
                }
            } catch (Exception e) {
                log.warn("[formmode] 字段权限拉取失败，按 fail-open 跳过裁剪. modeId={}, err={}",
                    permModeId, e.getMessage());
                permMap.clear();
            }
        }

        // 组装列名和值
        StringBuilder columns = new StringBuilder("id");
        StringBuilder placeholders = new StringBuilder("?");
        List<Object> values = new ArrayList<>();

        Long dataId = formDataDTO.getDataid();
        boolean isCreate = (dataId == null);

        // 生成新ID（新建时）
        if (isCreate) {
            dataId = generateDataId(mainTableName);
        }

        values.add(dataId);

        // 系统字段
        Long userId = currentUserId();

        // 目标表真实列（小写集合）：审计列/业务列只写表中存在的，
        // 兼容 modedatacreator(ecology 迁移) / modedatacreater / lastMod* 等命名差异。
        // 列类型元信息按表动态读取（对所有业务表生效），写值时按列类型兜底。
        Map<String, BizValueCoercer.ColumnMeta> colMeta = BizValueCoercer.loadColumnMeta(jdbcTemplate, mainTableName);
        Set<String> tableCols = new HashSet<>(colMeta.keySet());

        if (isCreate) {
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatacreator", "modedatacreater"}, userId);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatacreatedate"}, currentDate);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatacreatetime"}, currentTime);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"lastModDate"}, currentDate);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"lastModTime"}, currentTime);
        } else {
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatamodifier"}, userId);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatamodifydate"}, currentDate);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"modedatamodifytime"}, currentTime);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"lastModDate"}, currentDate);
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"lastModTime"}, currentTime);
        }

        // 编辑且有权限时，取原行用于「只读/隐藏字段保留 DB 原值」（防篡改）
        final Map<String, Object> originalRow = (!isCreate && !permMap.isEmpty())
            ? selectRowById(mainTableName, dataId) : null;

        // 用户自定义字段值
        for (WorkflowBillField field : fields) {
            String dbName = field.getFielddbname();
            if (dbName == null || !tableCols.contains(dbName.toLowerCase())) continue;

            // 字段权限裁剪：隐藏/只读字段受控
            FieldPermVO perm = permMap.get(field.getFieldname());
            boolean controlled = perm != null
                && (!Boolean.TRUE.equals(perm.getVisible()) || !Boolean.TRUE.equals(perm.getEditable()));
            if (controlled) {
                // 新建时跳过写入（防恶意注入隐藏字段）；编辑时保留 DB 原值（防篡改）
                if (!isCreate && originalRow != null) {
                    Object dbVal = originalRow.get(dbName.toLowerCase());
                    columns.append(",").append(dbName);
                    placeholders.append(",?");
                    values.add(BizValueCoercer.coerce(colMeta.get(dbName.toLowerCase()), dbVal));
                }
                continue;
            }

            columns.append(",").append(dbName);
            placeholders.append(",?");
            Object value = fieldValues.get(dbName);
            if (value == null) {
                value = fieldValues.get(field.getFieldname());
            }
            values.add(BizValueCoercer.coerce(colMeta.get(dbName.toLowerCase()), value));
        }

        // ── 唯一性校验（对标 ecology RepeatVerifyService）──
        // 按字段定义 uniquevalue=1 的列在保存前查重；确认重复则抛异常阻断保存。
        // 注：编辑时排除自身 id，避免「不改该字段也报重复」。
        Map<String, String> colNameMap = new LinkedHashMap<>();
        colMeta.forEach((k, m) -> colNameMap.put(k, m.name));
        checkUniqueFields(fields, mainTableName, colNameMap, fieldValues, isCreate ? null : dataId);

        // 执行SQL
        final Long finalUserId = Long.valueOf(userId);
        final String finalCurrentDate = currentDate;
        final String finalCurrentTime = currentTime;
        final Long finalDataId = dataId;
        final Map<String, Object> finalFieldValues = fieldValues;

        Long savedId = TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            if (isCreate) {
                String insertSql = "INSERT INTO `" + mainTableName + "` (" + columns + ") VALUES (" + placeholders + ")";
                jdbcTemplate.update(insertSql, values.toArray());
            } else {
                // 构建UPDATE语句（审计列只写目标表存在的，命名兼容同上）
                StringBuilder updateSql = new StringBuilder("UPDATE `" + mainTableName + "` SET ");
                List<String> setParts = new ArrayList<>();
                List<Object> updateValues = new ArrayList<>();

                appendAuditSet(setParts, updateValues, tableCols, new String[]{"modedatamodifier"}, finalUserId);
                appendAuditSet(setParts, updateValues, tableCols, new String[]{"modedatamodifydate"}, finalCurrentDate);
                appendAuditSet(setParts, updateValues, tableCols, new String[]{"modedatamodifytime"}, finalCurrentTime);
                appendAuditSet(setParts, updateValues, tableCols, new String[]{"lastModDate"}, finalCurrentDate);
                appendAuditSet(setParts, updateValues, tableCols, new String[]{"lastModTime"}, finalCurrentTime);

                for (WorkflowBillField field : fields) {
                    String dbName = field.getFielddbname();
                    if (dbName == null || !tableCols.contains(dbName.toLowerCase())) continue;

                    FieldPermVO perm = permMap.get(field.getFieldname());
                    boolean controlled = perm != null
                        && (!Boolean.TRUE.equals(perm.getVisible()) || !Boolean.TRUE.equals(perm.getEditable()));
                    if (controlled) {
                        // 只读/隐藏：保留 DB 原值（不取请求值），防篡改
                        Object dbVal = originalRow != null ? originalRow.get(dbName.toLowerCase()) : null;
                        setParts.add("`" + dbName + "`=?");
                        updateValues.add(BizValueCoercer.coerce(colMeta.get(dbName.toLowerCase()), dbVal));
                        continue;
                    }

                    setParts.add("`" + dbName + "`=?");
                    Object value = finalFieldValues.get(dbName);
                    if (value == null) value = finalFieldValues.get(field.getFieldname());
                    updateValues.add(BizValueCoercer.coerce(colMeta.get(dbName.toLowerCase()), value));
                }
                if (setParts.isEmpty()) {
                    setParts.add("`id`=`id`");
                }

                updateSql.append(String.join(",", setParts));
                updateSql.append(" WHERE id=?");
                updateValues.add(finalDataId);

                jdbcTemplate.update(updateSql.toString(), updateValues.toArray());
            }
            return finalDataId;
        });
        // 明细表数据落库：此前 FormDataDTO.detailTables 已接收但从未消费，
        // 导致明细只活在 wf_form_snapshot.dataJson 里（无法查询/统计/关联）；物理明细表已建却无写入。
        saveDetailData(formDataDTO.getDetailTables(), savedId, mainTableName, permMap);
        fireApprovalTrigger(formDataDTO.getModeid(), savedId, isCreate ? "save" : "update", finalFieldValues);
        return savedId;
    }

    /**
     * 按主键取业务表单行（编辑时字段权限裁剪用：只读/隐藏字段保留 DB 原值）。
     *
     * @return 行数据（key 小写列名）；不存在 / 表名非法返回 null
     */
    private Map<String, Object> selectRowById(String tableName, Long id) {
        if (id == null || !SAFE_IDENT.matcher(tableName).matches()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForMap("SELECT * FROM `" + tableName + "` WHERE id = ?", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 从行数据取创建人ID（兼容 modedatacreator / modedatacreater 两种命名）。
     */
    private Long parseRowCreator(Map<String, Object> row) {
        Object v = row.get("modedatacreator");
        if (v == null) {
            v = row.get("modedatacreater");
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return null;
    }

    /**
     * 明细表数据落库（**先删后插**，对齐 ecology「非自定义明细表 delete where 关联列=主表ID 后 insert」）。
     *
     * <p><b>背景（最大缺口）</b>：物理明细表 {@code formtable_main_N_dtM} 已由
     * {@code DynamicTableServiceImpl} 创建，但 {@code FormDataDTO.detailTables} 此前只接收不消费，
     * 流程里的明细仅存在于快照 JSON，无法查询 / 统计 / 关联。</p>
     *
     * <p><b>策略</b>：对每个明细表 → {@code DELETE WHERE 关联列 = 主表ID} → 逐行 INSERT
     * （id 雪花 + 关联列 + 业务列 + 审计列）。先删后插可天然覆盖「删除行 / 减少行」场景。</p>
     *
     * <p><b>关联列</b>：优先 {@code main_id}（本项目建表列名），兼容 {@code mainid}（ecology 迁移表）。</p>
     *
     * <p><b>容错</b>：单表失败仅记录 error 日志，<b>不阻断主表保存事务</b>（与审批触发同一策略）；
     * 主表已保存成功，明细失败可后续对账补偿。</p>
     *
     * @param detailTables 明细表名 → 行列表（来自 FormDataDTO.detailTables）
     * @param mainDataId   主表数据ID
     * @param mainTableName 主表名（仅用于日志，便于排查）
     */
    private void saveDetailData(Map<String, List<Map<String, Object>>> detailTables, Long mainDataId,
                                String mainTableName, Map<String, FieldPermVO> permMap) {
        if (detailTables == null || detailTables.isEmpty() || mainDataId == null) {
            return;
        }
        Long userId = currentUserId();
        String today = LocalDate.now().toString();
        String now = LocalTime.now().toString().substring(0, 8);

        for (Map.Entry<String, List<Map<String, Object>>> entry : detailTables.entrySet()) {
            String detailTable = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();
            if (StringUtil.isBlank(detailTable) || !SAFE_IDENT.matcher(detailTable).matches()) {
                log.warn("[formmode] 明细表名不合法，跳过. table={}", detailTable);
                continue;
            }
            try {
                Map<String, BizValueCoercer.ColumnMeta> colMeta =
                    BizValueCoercer.loadColumnMeta(jdbcTemplate, detailTable);
                if (colMeta == null || colMeta.isEmpty()) {
                    log.warn("[formmode] 明细表不存在或无列，跳过. table={}", detailTable);
                    continue;
                }
                Map<String, String> columns = new LinkedHashMap<>();
                colMeta.forEach((k, m) -> columns.put(k, m.name));

                // 关联主表的列：main_id（本项目）> mainid（ecology 迁移）
                String foundLinkCol = null;
                for (String candidate : new String[]{"main_id", "mainid"}) {
                    String col = columns.get(candidate.toLowerCase());
                    if (col != null) {
                        foundLinkCol = col;
                        break;
                    }
                }
                final String linkCol = foundLinkCol;
                if (linkCol == null) {
                    log.warn("[formmode] 明细表缺少关联列(main_id/mainid)，跳过. table={}", detailTable);
                    continue;
                }

                // 先删后插（保证与本次提交一致，含删行场景）
                jdbcTemplate.update("DELETE FROM `" + detailTable + "` WHERE `" + linkCol + "` = ?", mainDataId);
                if (rows == null || rows.isEmpty()) {
                    continue;
                }

                int ok = 0;
                for (Map<String, Object> src : rows) {
                    if (src == null || src.isEmpty()) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", IdWorker.getId());
                    row.put(linkCol, mainDataId);
                    src.forEach((k, v) -> {
                        if (StringUtil.isBlank(k)) {
                            return;
                        }
                        String ck = k.toLowerCase();
                        String col = columns.get(ck);
                        // 忽略：表中不存在的列、系统列（id/审计列由本方法接管）、关联列（已单独设置）
                        if (col == null || isSystemField(col) || col.equalsIgnoreCase(linkCol)) {
                            return;
                        }
                        // 字段权限裁剪：明细先删后插=新建语义，受控字段直接跳过写入（防越权注入）
                        FieldPermVO dPerm = permMap.get(ck);
                        if (dPerm != null
                            && (!Boolean.TRUE.equals(dPerm.getVisible())
                                || !Boolean.TRUE.equals(dPerm.getEditable()))) {
                            return;
                        }
                        row.put(col, BizValueCoercer.coerce(colMeta.get(ck), v));
                    });
                    // 审计列（目标表存在才写，兼容多种历史命名）
                    putIfColumn(row, columns, "modedatacreator", userId);
                    putIfColumn(row, columns, "modedatacreater", userId);
                    putIfColumn(row, columns, "modedatacreatedate", today);
                    putIfColumn(row, columns, "modedatacreatetime", now);
                    putIfColumn(row, columns, "modedatamodifier", userId);
                    putIfColumn(row, columns, "modedatamodifydate", today);
                    putIfColumn(row, columns, "modedatamodifytime", now);

                    StringBuilder cols = new StringBuilder();
                    StringBuilder ph = new StringBuilder();
                    List<Object> vals = new ArrayList<>();
                    for (Map.Entry<String, Object> e : row.entrySet()) {
                        if (cols.length() > 0) {
                            cols.append(",");
                            ph.append(",");
                        }
                        cols.append("`").append(e.getKey()).append("`");
                        ph.append("?");
                        vals.add(e.getValue());
                    }
                    jdbcTemplate.update(
                        "INSERT INTO `" + detailTable + "` (" + cols + ") VALUES (" + ph + ")", vals.toArray());
                    ok++;
                }
                log.info("[formmode] 明细表落库完成. main={}, detail={}, rows={}", mainTableName, detailTable, ok);
            } catch (Exception e) {
                log.error("[formmode] 明细表落库失败（不阻断主表保存，可后续对账补偿）. main={}, detail={}, dataId={}, err={}",
                    mainTableName, detailTable, mainDataId, e.getMessage());
            }
        }
    }

    /**
     * 表单保存/删除成功后触发审批流程（接线 ApprovalTriggerServiceImpl）。
     *
     * <p>触发配置存于 mode_triggerworkflowset（modeid + status=1 命中）；
     * 流程发起失败不阻断表单保存主流程——ApprovalTriggerServiceImpl 内部已兜底，
     * 此处再兜一层，保证保存事务不被触发链路异常回滚。</p>
     */
    private void fireApprovalTrigger(Long modeId, Long dataId, String action, Map<String, Object> fieldValues) {
        if (modeId == null || dataId == null) {
            return;
        }
        try {
            approvalTriggerService.triggerApproval(modeId, dataId, action, fieldValues);
        } catch (Exception e) {
            log.error("[formmode] 审批触发异常（不阻断保存主流程）: modeId={}, dataId={}, action={}",
                modeId, dataId, action, e);
        }
    }

    /**
     * 取当前登录用户 ID。
     *
     * <p>必须与数据范围 {@code resolveDataScope} 同源（均用 {@link AuthUtil}），否则同一请求下
     * {@code SecureUtil} 与 {@code AuthUtil} 取到的用户 ID 可能不一致，导致新建数据写入的创建人
     * 与列表数据范围过滤的 {@code creatorId} 对不上，表现为「本人刚录入的数据在列表里看不到」。</p>
     */
    private Long currentUserId() {
        try {
            return AuthUtil.getUser() != null ? AuthUtil.getUser().getUserId() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 审计列（INSERT 分支）：目标表存在该列（候选名任一命中，大小写不敏感）才追加。
     * 兼容 modedatacreator(ecology 迁移命名) / modedatacreater / lastMod* 等历史差异。
     */
    private void appendAuditColumn(StringBuilder columns, StringBuilder placeholders, List<Object> values,
                                   Set<String> tableCols, String[] candidates, Object value) {
        for (String name : candidates) {
            if (tableCols.contains(name.toLowerCase())) {
                columns.append(",").append(name);
                placeholders.append(",?");
                values.add(value);
                return;
            }
        }
    }

    /**
     * 审计列（UPDATE 分支）：目标表存在该列才加入 SET 子句。
     */
    private void appendAuditSet(List<String> setParts, List<Object> updateValues,
                                Set<String> tableCols, String[] candidates, Object value) {
        for (String name : candidates) {
            if (tableCols.contains(name.toLowerCase())) {
                setParts.add("`" + name + "`=?");
                updateValues.add(value);
                return;
            }
        }
    }

    /**
     * 解析业务主表名（<b>双口径</b>，formId / modeId 均可）。
     *
     * <ol>
     *   <li>入参按「<b>表单ID</b>」→ {@code workflow_bill.table_name}；
     *       覆盖 ecology 迁移表单（其表名与表单ID不同，如 {@code 2064530495200337922 → formtable_main_5}）</li>
     *   <li>取不到再按「<b>模块ID</b>」→ {@code modeinfo.billid} → {@code formtable_main_{billid}}</li>
     * </ol>
     *
     * <p><b>为什么需要双口径</b>：不同端点入参口径不一（{@code /{formId}} 系列传 formId、
     * {@code /{modeId}/{dataId}} 与 {@code /page/{modeId}} 按 modeId 命名），
     * 单一口径会把另一种入参静默解析失败（表现为「查不到数据/空页」）。
     * 统一在此收敛，调用方无需关心传的是哪一种。</p>
     *
     * @param idOrFormId 表单ID 或 模块ID
     * @return 合法表名；解析不到返回 null
     */
    private String resolveMainTableName(Long idOrFormId) {
        if (idOrFormId == null) {
            return null;
        }
        String tableName = null;
        WorkflowBill bill = workflowBillMapper.selectById(idOrFormId);
        if (bill != null) {
            tableName = StringUtil.isBlank(bill.getTableName())
                ? TableNameUtil.getMainTableName(idOrFormId)
                : bill.getTableName();
        }
        if (tableName == null || !SAFE_IDENT.matcher(tableName).matches()) {
            ModeInfo modeInfo = modeInfoMapper.selectById(idOrFormId);
            if (modeInfo != null && modeInfo.getBillid() != null) {
                tableName = TableNameUtil.getMainTableName(modeInfo.getBillid());
            }
        }
        if (tableName == null || !SAFE_IDENT.matcher(tableName).matches()) {
            return null;
        }
        return tableName;
    }

    @Override
    public FormDataVO getFormDataById(Long formId, Long dataId) {
        // 双口径解析主表名（见 resolveMainTableName）：
        // 迁移表单的表名与表单ID并不相同（如 表单 2064530495200337922 → formtable_main_5），
        // 且这些表单 module_id 为空（旧 modeinfo 链路第一步即"模块不存在"）。
        String tableName = resolveMainTableName(formId);
        if (tableName == null) {
            return null;
        }

        // 行级数据范围（P2-2 Step4）：越权行返回 null（仅本人创建可见）
        Long realModeId = formRightService.resolveModeIdByFormId(formId);
        if (realModeId == null) realModeId = formId;
        final DataScope scope = formRightService.resolveDataScope(realModeId);

        final String finalTable = tableName;
        return TableNameContextHolder.executeWithTableName(finalTable, () -> {
            String sql = "SELECT * FROM `" + finalTable + "` WHERE id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dataId);
            if (rows.isEmpty()) return null;

            Map<String, Object> row = rows.get(0);

            // 数据范围：非全量可见且非本人创建 → 视为无权限
            if (!scope.isCanViewAll()) {
                Long creatorOfRow = parseRowCreator(row);
                if (!Objects.equals(creatorOfRow, scope.getCreatorId())) {
                    return null;
                }
            }
            FormDataVO vo = new FormDataVO();
            vo.setId(dataId);
            vo.setModeid(formId);

            // 过滤系统字段，只保留自定义字段值
            Map<String, Object> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (!isSystemField(key)) {
                    fieldValues.put(key, entry.getValue());
                }
            }
            vo.setFieldValues(fieldValues);

            Object createrVal = row.containsKey("modedatacreator") ? row.get("modedatacreator") : row.get("modedatacreater");
            vo.setCreater(createrVal == null ? null : ((Number) createrVal).longValue());
            vo.setCreatedate((String) row.get("modedatacreatedate"));
            vo.setCreatetime((String) row.get("modedatacreatetime"));

            return vo;
        });
    }

    @Override
    public Page<Map<String, Object>> pageFormData(Long modeId, Page<?> page, Map<String, Object> params) {
        // 双口径解析主表名（与 getFormDataById 对齐）：
        //   1) 入参按「表单ID」→ workflow_bill.table_name（覆盖 ecology 迁移表单）；
        //   2) 取不到再按「模块ID」→ modeinfo.billid → formtable_main_{billid}。
        // 说明：前端当前未调用本接口（POST /form-data/page/{modeId} 无调用方），
        //       但「传 formId 被当 modeId」会静默返回空页，故按同一口径兜底，彻底消除该隐患。
        String mainTableName = resolveMainTableName(modeId);
        if (mainTableName == null) {
            return new Page<>(page.getCurrent(), page.getSize());
        }

        // 行级数据范围（P2-2 Step4）：把 formId 反查为 modeId 后解析可见范围
        Long realModeId = formRightService.resolveModeIdByFormId(modeId);
        if (realModeId == null) {
            realModeId = modeId;
        }
        final DataScope scope = formRightService.resolveDataScope(realModeId);

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            Map<String, BizValueCoercer.ColumnMeta> colMeta =
                BizValueCoercer.loadColumnMeta(jdbcTemplate, mainTableName);
            Set<String> tableCols = new HashSet<>(colMeta.keySet());

            // 构建查询
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM `" + mainTableName + "` WHERE 1=1");
            StringBuilder dataSql = new StringBuilder("SELECT * FROM `" + mainTableName + "` WHERE 1=1");
            List<Object> queryParams = new ArrayList<>();

            // 数据范围：非全量可见 → 仅本人创建（无 creator 列/无用户则 1=0 安全空结果）
            if (!scope.isCanViewAll()) {
                String creatorCol = tableCols.contains("modedatacreator") ? "modedatacreator"
                    : (tableCols.contains("modedatacreater") ? "modedatacreater" : null);
                if (creatorCol != null && scope.getCreatorId() != null) {
                    countSql.append(" AND `").append(creatorCol).append("` = ?");
                    dataSql.append(" AND `").append(creatorCol).append("` = ?");
                    queryParams.add(scope.getCreatorId());
                } else {
                    countSql.append(" AND 1=0");
                    dataSql.append(" AND 1=0");
                }
            }

            // 添加查询条件
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (!isSystemField(entry.getKey()) && entry.getValue() != null) {
                        countSql.append(" AND `").append(entry.getKey()).append("` LIKE ?");
                        dataSql.append(" AND `").append(entry.getKey()).append("` LIKE ?");
                        queryParams.add("%" + entry.getValue() + "%");
                    }
                }
            }

            // 查总数
            Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, queryParams.toArray());

            // 查分页数据
            long offset = (page.getCurrent() - 1) * page.getSize();
            dataSql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
            List<Object> pageParams = new ArrayList<>(queryParams);
            pageParams.add(page.getSize());
            pageParams.add(offset);

            List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql.toString(), pageParams.toArray());

            Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), total);
            result.setRecords(records);
            return result;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFormData(Long modeId, Long dataId) {
        FormDataDTO dto = new FormDataDTO();
        dto.setModeid(modeId);
        dto.setDataid(dataId);
        dto.setSrc("del");
        saveFormData(dto);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteFormData(Long modeId, List<Long> dataIds) {
        for (Long dataId : dataIds) {
            deleteFormData(modeId, dataId);
        }
        return true;
    }

    /**
     * 按「表单ID」写入业务数据行（新增/更新），返回业务数据ID。
     *
     * <p>与 {@link #saveFormData} 的关键区别（后者对本项目多数表单根本不成立）：</p>
     * <ul>
     *   <li>表名以 {@code workflow_bill.table_name} 为准，而非 {@code formtable_main_{billId}} 推导：
     *       迁移过来的表单表名与表单ID并不相同（表单 2064530495200337922 → {@code formtable_main_5}）；</li>
     *   <li>不依赖 {@code modeinfo} 模块（这些表单 {@code module_id} 为空，旧链路第一步就"模块不存在"）；</li>
     *   <li>只写目标表<b>真实存在</b>的列（不同表单的审计列名不同：{@code modedatacreator} /
     *       {@code modedatacreater} / {@code lastModDate}…），字段值按<b>字段名</b>（= 列名，
     *       大小写不敏感）匹配，匹配不上的键直接忽略，绝不拼出未知列。</li>
     *   <li>字段值统一经 {@link BizValueCoercer#coerce}按<b>目标列真实类型</b>兜底：数值列
     *       收到空串/对象/越界值降级为 null，字符串列超长截断 —— 单个字段类型不匹配
     *       不会让整行写入失败（否则 workflow 侧会退化成「占位 dataId」）。</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBusinessData(Long formId, Long dataId) {
        if (formId == null || dataId == null) {
            return false;
        }
        WorkflowBill bill = workflowBillMapper.selectById(formId);
        if (bill == null) {
            log.warn("[formmode] 删除业务数据失败：表单不存在: {}", formId);
            return false;
        }
        String tableName = StringUtil.isBlank(bill.getTableName())
            ? TableNameUtil.getMainTableName(formId)
            : bill.getTableName();
        if (!SAFE_IDENT.matcher(tableName).matches()) {
            throw new RuntimeException("表单表名不合法: " + tableName);
        }
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM `" + tableName + "` WHERE id = ?", Integer.class, dataId);
        if (cnt == null || cnt == 0) {
            return false;
        }
        jdbcTemplate.update("DELETE FROM `" + tableName + "` WHERE id = ?", dataId);
        log.info("[formmode] 业务数据已删除. formId={}, table={}, dataId={}", formId, tableName, dataId);
        return true;
    }

    @Override
    public Map<String, List<Map<String, Object>>> getDetailData(Long formId, Long dataId) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (formId == null || dataId == null) {
            return result;
        }
        WorkflowBill bill = workflowBillMapper.selectById(formId);
        if (bill == null) {
            log.warn("[formmode] 读明细失败：表单不存在. formId={}", formId);
            return result;
        }
        String mainTable = StringUtil.isBlank(bill.getTableName())
            ? TableNameUtil.getMainTableName(formId)
            : bill.getTableName();
        if (!SAFE_IDENT.matcher(mainTable).matches()) {
            log.warn("[formmode] 读明细失败：主表名不合法. formId={}, table={}", formId, mainTable);
            return result;
        }

        // 1) 找出该主表下的所有明细物理表：{主表名}_dt{N}
        //    用 information_schema 动态发现，避免硬编码明细表数量（不同表单明细数不同）。
        List<String> detailTables;
        try {
            detailTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema = DATABASE() AND table_name LIKE CONCAT(?, '\\_dt%')",
                String.class, mainTable);
        } catch (Exception e) {
            log.warn("[formmode] 查询明细表清单失败. main={}, err={}", mainTable, e.getMessage());
            return result;
        }
        if (detailTables == null || detailTables.isEmpty()) {
            return result;
        }

        // 2) 逐表按关联列取行；单表失败不阻断其它明细表
        for (String dt : detailTables) {
            if (dt == null || !SAFE_IDENT.matcher(dt).matches()) {
                continue;
            }
            try {
                String linkCol = resolveDetailLinkColumn(dt);
                if (linkCol == null) {
                    log.warn("[formmode] 明细表缺少关联列(main_id/mainid)，跳过. detail={}", dt);
                    continue;
                }
                List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList("SELECT * FROM `" + dt + "` WHERE `" + linkCol + "` = ?", dataId);
                result.put(dt, rows == null ? new ArrayList<>() : rows);
            } catch (Exception e) {
                log.warn("[formmode] 读明细表失败（跳过）. detail={}, err={}", dt, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 探测明细表关联主表的列名：优先 {@code main_id}（本项目建表列名），兼容 {@code mainid}。
     *
     * @return 关联列名；不存在返回 null（调用方跳过该表）
     */
    private String resolveDetailLinkColumn(String detailTable) {
        try {
            List<String> cols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = ?", String.class, detailTable);
            if (cols == null || cols.isEmpty()) {
                return null;
            }
            boolean hasMainId = false;
            boolean hasMainid = false;
            for (String c : cols) {
                if (c == null) {
                    continue;
                }
                String lc = c.toLowerCase();
                if ("main_id".equals(lc)) {
                    hasMainId = true;
                } else if ("mainid".equals(lc)) {
                    hasMainid = true;
                }
            }
            return hasMainId ? "main_id" : (hasMainid ? "mainid" : null);
        } catch (Exception e) {
            log.warn("[formmode] 探测明细表关联列失败. detail={}, err={}", detailTable, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveBusinessData(FormDataSaveDTO dto) {
        if (dto == null || dto.getFormId() == null) {
            throw new RuntimeException("表单ID不能为空");
        }
        WorkflowBill bill = workflowBillMapper.selectById(dto.getFormId());
        if (bill == null) {
            throw new RuntimeException("表单不存在: " + dto.getFormId());
        }
        String tableName = StringUtil.isBlank(bill.getTableName())
            ? TableNameUtil.getMainTableName(dto.getFormId())
            : bill.getTableName();
        if (!SAFE_IDENT.matcher(tableName).matches()) {
            throw new RuntimeException("表单表名不合法: " + tableName);
        }

        // 目标表真实列（小写 → 实际列名）+ 列类型元信息。类型在写入时从 information_schema 动态读取，
        // 因此对**所有**业务表生效（存量自建表 + 前端新建的表），无需按表名硬编码。
        Map<String, String> columns = new LinkedHashMap<>();
        Map<String, BizValueCoercer.ColumnMeta> colMeta = BizValueCoercer.loadColumnMeta(jdbcTemplate, tableName);
        colMeta.forEach((k, m) -> columns.put(k, m.name));
        if (columns.isEmpty()) {
            throw new RuntimeException("业务表不存在: " + tableName);
        }

        Long userId = currentUserId();
        String today = LocalDate.now().toString();
        String now = LocalTime.now().toString().substring(0, 8);
        boolean create = (dto.getDataId() == null);
        Long dataId = create ? IdWorker.getId() : dto.getDataId();

        // ── 幂等保护：同一流程实例（requestId）重复提交时不产生脏行 ──
        // 场景：workflow 发起/保存重试、前端重复点击、Feign 超时重试、消息重投。
        // 策略：新建(create) 且传入 requestId 且业务表存在 request_id 列时，
        //       先查该 requestId 是否已有对应行；有则直接返回既有 dataId（幂等），
        //       查重失败仅告警并降级为普通插入，绝不阻断主流程。
        if (create && dto.getRequestId() != null) {
            String idemCol = requestIdColumn(columns);
            if (idemCol != null) {
                try {
                    java.util.List<Long> existIds = jdbcTemplate.queryForList(
                        "SELECT id FROM `" + tableName + "` WHERE `" + idemCol + "` = ? LIMIT 1",
                        Long.class, dto.getRequestId());
                    if (existIds != null && !existIds.isEmpty()) {
                        log.info("[formmode] 幂等命中：requestId 已存在，直接返回既有数据ID. requestId={}, dataId={}",
                            dto.getRequestId(), existIds.get(0));
                        return existIds.get(0);
                    }
                } catch (Exception e) {
                    log.warn("[formmode] requestId 幂等查重失败，降级为普通插入. requestId={}, err={}",
                        dto.getRequestId(), e.getMessage());
                }
            }
        }

        // ── 唯一性校验（对标 ecology RepeatVerifyService）──
        // 流程侧保存（/save-by-form）同样需要：避免重复提交 / 重复数据产生脏行。
        // 注：本路径没有现成字段列表，需按 formId 加载（billid 为 int，超范围时自动跳过校验）。
        checkUniqueFields(loadFieldsForUniqueCheck(dto.getFormId()), tableName, columns,
            dto.getFieldValues(), create ? null : dataId);

        // 待写列（保持插入顺序）：新增含 id；审计列存在才写
        Map<String, Object> row = new LinkedHashMap<>();
        if (create) {
            row.put("id", dataId);
        }
        Map<String, Object> fieldValues = dto.getFieldValues();
        if (fieldValues != null) {
            fieldValues.forEach((k, v) -> {
                if (StringUtil.isBlank(k)) {
                    return;
                }
                String ck = k.toLowerCase();
                String col = columns.get(ck);
                // 表里没这列 / 是系统列（由审计逻辑接管，不被业务值覆盖）→ 忽略
                if (col == null || isSystemField(col)) {
                    return;
                }
                // ⚠️ 按目标列类型兜底：类型不兼容的单个字段降级（数值列→null、字符串列→截断），
                // 不再让整行 INSERT 失败 —— 整行失败会被 workflow 侧回退成「占位 dataId」，
                // 表现为"业务数据行未创建成功"，比丢一个字段严重得多。
                row.put(col, BizValueCoercer.coerce(colMeta.get(ck), v));
            });
        }
        // 关联流程实例ID（雪花）→ request_id：发起流程后由 workflow 侧回填，用于「单据 ↔ 流程」双向反查。
        // 列名兼容历史命名（request_id / requestId）；本列必须 BIGINT（早期建成 INT 会截断雪花ID）。
        if (dto.getRequestId() != null) {
            String requestCol = requestIdColumn(columns);
            if (requestCol != null) {
                // 显式覆盖：即使 fieldValues 里带了同名键，也以 requestId 参数为准
                row.put(requestCol, dto.getRequestId());
            } else {
                log.warn("[formmode] 业务表无 request_id 列，跳过流程实例ID回填. table={}", tableName);
            }
        }
        // 审计列：只写目标表存在的那些（兼容 modedatacreator / modedatacreater / lastMod* 多种命名）
        putIfColumn(row, columns, "modedatacreator", userId);
        putIfColumn(row, columns, "modedatacreater", userId);
        putIfColumn(row, columns, "modedatacreatedate", today);
        putIfColumn(row, columns, "modedatacreatetime", now);
        putIfColumn(row, columns, "modedatamodifier", userId);
        putIfColumn(row, columns, "modedatamodifydate", today);
        putIfColumn(row, columns, "modedatamodifytime", now);
        putIfColumn(row, columns, "lastModDate", today);
        putIfColumn(row, columns, "lastModTime", now);

        // id 不在更新集合里
        if (!create) {
            row.remove("id");
        }
        if (row.isEmpty()) {
            row.put(columns.get("id"), dataId);
        }

        String colSql = row.keySet().stream().map(c -> "`" + c + "`").collect(Collectors.joining(","));
        if (create) {
            String markSql = row.keySet().stream().map(c -> "?").collect(Collectors.joining(","));
            jdbcTemplate.update("INSERT INTO `" + tableName + "` (" + colSql + ") VALUES (" + markSql + ")",
                row.values().toArray());
        } else {
            String setSql = row.keySet().stream().map(c -> "`" + c + "`=?").collect(Collectors.joining(","));
            List<Object> args = new ArrayList<>(row.values());
            args.add(dataId);
            jdbcTemplate.update("UPDATE `" + tableName + "` SET " + setSql + " WHERE id=?", args.toArray());
        }
        log.info("[formmode] 业务数据已写入. formId={}, table={}, dataId={}, create={}",
            dto.getFormId(), tableName, dataId, create);
        return dataId;
    }

    /** 关联流程实例ID 的列名（兼容 request_id / requestId / requestid 三种历史命名） */
    private static String requestIdColumn(Map<String, String> columns) {
        for (String name : new String[]{"request_id", "requestId", "requestid"}) {
            String col = columns.get(name.toLowerCase());
            if (col != null) {
                return col;
            }
        }
        return null;
    }

    /** 列存在则写入（列名大小写不敏感） */
    private static void putIfColumn(Map<String, Object> row, Map<String, String> columns,
                                    String name, Object value) {
        String col = columns.get(name.toLowerCase());
        if (col != null && !row.containsKey(col)) {
            row.put(col, value);
        }
    }

    /**
     * 唯一性校验（对标 ecology {@code RepeatVerifyService}）。
     *
     * <p>按字段定义 {@code uniquevalue=1} 的列，在保存前查重：
     * {@code SELECT COUNT(1) FROM 表 WHERE 列=? [AND id<>?]}（编辑时排除自身）。
     * 冲突抛 {@link RuntimeException} 阻断保存，避免产生重复数据。</p>
     *
     * <p><b>容错原则</b>：查询异常（如列类型不兼容）<b>仅告警并跳过该列</b>，
     * 不阻断保存——可用性优先于强一致；唯独「确认重复」会阻断。</p>
     *
     * @param fields       字段定义（含 uniquevalue）
     * @param tableName    物理表名（已校验合法）
     * @param columns      小写列名 → 实际列名
     * @param fieldValues  待保存字段值
     * @param excludeId    编辑时排除的主键；新建传 null
     */
    private void checkUniqueFields(List<WorkflowBillField> fields, String tableName,
                                   Map<String, String> columns, Map<String, Object> fieldValues, Long excludeId) {
        if (fields == null || fields.isEmpty() || tableName == null
            || fieldValues == null || fieldValues.isEmpty() || columns == null || columns.isEmpty()) {
            return;
        }
        if (!SAFE_IDENT.matcher(tableName).matches()) {
            return;
        }
        for (WorkflowBillField f : fields) {
            if (f == null || f.getUniquevalue() == null || f.getUniquevalue() != 1) {
                continue;
            }
            String dbName = f.getFielddbname();
            if (StringUtil.isBlank(dbName)) {
                continue;
            }
            String col = columns.get(dbName.toLowerCase());
            if (col == null) {
                continue; // 物理表无此列，跳过
            }
            Object v = fieldValues.get(dbName);
            if (v == null) {
                v = fieldValues.get(f.getFieldname());
            }
            if (v == null || String.valueOf(v).trim().isEmpty()) {
                continue; // 空值不参与唯一校验（与「必填」校验分离）
            }
            try {
                Integer cnt;
                if (excludeId == null) {
                    cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM `" + tableName + "` WHERE `" + col + "` = ?", Integer.class, v);
                } else {
                    cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM `" + tableName + "` WHERE `" + col + "` = ? AND id <> ?",
                        Integer.class, v, excludeId);
                }
                if (cnt != null && cnt > 0) {
                    String label = StringUtil.isBlank(f.getFieldname()) ? dbName : f.getFieldname();
                    throw new RuntimeException(
                        "字段【" + label + "】的值「" + v + "」已存在，不可重复（唯一性校验未通过）");
                }
            } catch (RuntimeException re) {
                throw re; // 确认重复：向外抛，阻断保存
            } catch (Exception e) {
                log.warn("[formmode] 唯一性校验查询失败（跳过该列）. table={}, col={}, err={}",
                    tableName, col, e.getMessage());
            }
        }
    }

    /**
     * 为唯一性校验加载字段定义。
     *
     * <p><b>类型对齐（2026-10-20）</b>：{@code workflow_billfield.billid} DDL 本就是 bigint，
     * 实体/Mapper 已对齐 Long，故雪花 formId 可<b>直接查询</b>——此前因实体是 Integer 而加的
     * 「超出 int 范围则跳过校验」兜底已移除，唯一性校验对迁移表单从此真实生效。</p>
     */
    private List<WorkflowBillField> loadFieldsForUniqueCheck(Long formId) {
        if (formId == null) {
            return new ArrayList<>();
        }
        try {
            List<WorkflowBillField> fs = workflowBillFieldMapper.selectByBillId(formId);
            return fs == null ? new ArrayList<>() : fs;
        } catch (Exception e) {
            log.warn("[formmode] 加载字段定义失败，跳过唯一性校验. formId={}, err={}", formId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 生成新的数据ID
     *
     * <p>⚠️ <b>并发安全修正</b>：原实现为 {@code SELECT COALESCE(MAX(id),0)+1}，
     * 并发下两个请求会算出<b>相同 id</b> 并撞主键（后提交者失败或覆盖前者），
     * 且与「单据主键应全局唯一」的预期不符。</p>
     *
     * <p>改为雪花 ID（{@link IdWorker}），与 {@link #saveBusinessData} 的新建分支
     * （{@code IdWorker.getId()}）保持同一策略。</p>
     *
     * @param tableName 业务表名（仅用于合法性校验，ID 生成不再依赖表内数据）
     */
    private Long generateDataId(String tableName) {
        if (tableName == null || !SAFE_IDENT.matcher(tableName).matches()) {
            throw new RuntimeException("表单表名不合法: " + tableName);
        }
        return IdWorker.getId();
    }

    /**
     * 判断是否为系统字段
     */
    private boolean isSystemField(String fieldName) {
        return fieldName == null
            || "id".equalsIgnoreCase(fieldName)
            || "requestId".equalsIgnoreCase(fieldName)
            || fieldName.toLowerCase().startsWith("modedata")
            || "modeuuid".equalsIgnoreCase(fieldName)
            || "lastModDate".equalsIgnoreCase(fieldName)
            || "lastModTime".equalsIgnoreCase(fieldName);
    }

}
