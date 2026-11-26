package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
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
import org.springblade.formmode.service.IFormLayoutService;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.service.IFormRightService;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.utils.BizValueCoercer;
import org.springblade.formmode.utils.LayoutFormulaResolver;
import org.springblade.formmode.utils.MatrixSubjectResolver;
import org.springblade.formmode.utils.TableNameContextHolder;
import org.springblade.formmode.utils.TableNameUtil;
import org.springblade.formmode.vo.DataScope;
import org.springblade.formmode.vo.FieldPermVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.system.datascope.DataScopeValueResolver;
import org.springframework.beans.factory.annotation.Value;
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

    /** 列名白名单：与前端 Datascope.tsx 的 SAFE_SQL_COLUMN 保持一致（防注入） */
    private static final Pattern SAFE_COLUMN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_$]*$");

    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final IFormModeService formModeService;
    private final IApprovalTriggerService approvalTriggerService;
    private final IFormRightService formRightService;
    private final IFormLayoutService formLayoutService;
    private final JdbcTemplate jdbcTemplate;
    private final MatrixSubjectResolver matrixSubjectResolver;

    /**
     * 行级「写」权限校验开关（编辑/删除他人创建的数据）。
     *
     * <p>对齐 E9：创建人对自己创建的数据有完全控制权；命中共享规则（canViewAll）可改删全部。
     * 默认开启；若历史流程场景需要「审批人代改他人数据」，可配置为 false 关闭本校验。</p>
     */
    @Value("${blade.formmode.row-write-scope.enforce:true}")
    private boolean rowWriteScopeEnforce;

    /**
     * 行级写校验严格模式（默认 false 保持兼容）。
     *
     * <p>true：无创建人列 / 空创建人 / 校验异常一律拒绝写入（fail-closed）；
     * false：这些情况跳过校验（历史表兼容）。安全要求高的环境建议开启。</p>
     */
    @Value("${blade.formmode.row-write-scope.strict:false}")
    private boolean rowWriteScopeStrict;

    /** 字段级「写」权限裁剪开关（编辑/新建/单元格写入不可编辑字段）；默认开启 */
    @Value("${blade.formmode.field-perm.write-enforce:true}")
    private boolean fieldWritePermEnforce;

    /**
     * 安全档位：strict 时强制所有 fail-closed 开关为 true（除非单独显式关闭）。
     * 用于通用化/高安全环境一键收紧；compat（默认）保持既有兼容行为。
     */
    @Value("${blade.formmode.security.profile:compat}")
    private String securityProfile;

    @jakarta.annotation.PostConstruct
    public void applySecurityProfile() {
        if ("strict".equalsIgnoreCase(securityProfile)) {
            rowWriteScopeEnforce = true;
            rowWriteScopeStrict = true;
            fieldWritePermEnforce = true;
        }
    }

    /**
     * 按表单布局重算公式，返回「目标字段 → 计算结果」。
     *
     * <p>布局读取失败 / 无公式时返回空 Map（调用方按「无公式字段」处理，行为不变）。</p>
     */
    private Map<String, Object> resolveLayoutFormulas(Long formId, Map<String, Object> fieldValues) {
        if (formId == null) {
            return Collections.emptyMap();
        }
        try {
            List<FormLayout> layouts = formLayoutService.listByFormId(formId, null, null);
            if (layouts == null || layouts.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> merged = new LinkedHashMap<>();
            for (FormLayout layout : layouts) {
                String layoutJson = layout == null ? null : layout.getLayoutJson();
                if (layoutJson == null || layoutJson.isEmpty()) {
                    continue;
                }
                Map<String, Object> computed = LayoutFormulaResolver.resolve(layoutJson, fieldValues);
                computed.forEach((k, v) -> merged.putIfAbsent(k, v));
            }
            return merged;
        } catch (Exception e) {
            log.warn("[formmode] 布局公式重算失败，跳过公式字段. formId={}, err={}", formId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 该字段是否为公式赋值字段（目标字段清单命中字段名或物理列名） */
    private boolean isFormulaDestField(Set<String> destFields, WorkflowBillField field) {
        if (destFields == null || destFields.isEmpty() || field == null) {
            return false;
        }
        if (field.getFieldname() != null && destFields.contains(field.getFieldname())) {
            return true;
        }
        return field.getFielddbname() != null && destFields.contains(field.getFielddbname().toLowerCase());
    }


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
            // 行级写权限：仅本人创建 / 命中共享规则可删除
            assertRowWriteScope(mainTableName, formDataDTO.getModeid(), formDataDTO.getDataid());
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

        // ★ 公式赋值字段：按布局服务端重算（权威口径），并纳入「可保存字段清单」
        //   （对齐 E9 FieldInfo.getFormulaNew：把公式 destfield 并入可保存字段列表）。
        //   公式目标字段在布局里通常是「只读」——若沿用只读裁剪，算好的值会被丢掉；
        //   故这里单独标记：不被裁剪，且写入的是服务端重算的值（不信任客户端传值，防篡改）。
        Map<String, Object> formulaValues = resolveLayoutFormulas(billId, fieldValues);
        Set<String> formulaDestFields = new HashSet<>(formulaValues.keySet());


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
                if ("strict".equalsIgnoreCase(securityProfile)) {
                    log.error("[formmode] 字段权限拉取失败，strict 档位拒绝写入. modeId={}, err={}", permModeId, e.getMessage());
                    throw new ServiceException("字段权限校验失败，拒绝写入");
                }
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
            // 建模权限的数据范围可能用 create_user / create_dept 作为权限列：
            // 新建时必须由服务端按当前登录用户写入，不能依赖前端传值。
            appendAuditColumn(columns, placeholders, values, tableCols,
                new String[]{"create_user"}, userId);
            Long deptId = currentPrimaryDeptId();
            if (deptId != null) {
                appendAuditColumn(columns, placeholders, values, tableCols,
                    new String[]{"create_dept"}, deptId);
            }
            Long tenantId = currentTenantId();
            if (tenantId != null) {
                appendAuditColumn(columns, placeholders, values, tableCols,
                    new String[]{"tenant_id"}, tenantId);
            }
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

            // 公式赋值字段：服务端算出来的值，优先于客户端传值
            boolean isFormulaField = isFormulaDestField(formulaDestFields, field);

            // 字段权限裁剪：隐藏/只读字段受控（公式赋值字段例外：它是算出来的，不是用户填的）
            FieldPermVO perm = permMap.get(field.getFieldname());
            boolean controlled = perm != null
                && (!Boolean.TRUE.equals(perm.getVisible()) || !Boolean.TRUE.equals(perm.getEditable()))
                && !isFormulaField;
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
            Object value = null;
            if (isFormulaField) {
                value = formulaValues.get(field.getFieldname());
                if (value == null) value = formulaValues.get(dbName.toLowerCase());
            }
            if (value == null) {
                value = fieldValues.get(dbName);
            }
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
                // 行级写权限：仅本人创建 / 命中共享规则可编辑
                assertRowWriteScope(mainTableName, formDataDTO.getModeid(), finalDataId);
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

                    boolean isFormulaField = isFormulaDestField(formulaDestFields, field);

                    FieldPermVO perm = permMap.get(field.getFieldname());
                    boolean controlled = perm != null
                        && (!Boolean.TRUE.equals(perm.getVisible()) || !Boolean.TRUE.equals(perm.getEditable()))
                        && !isFormulaField;
                    if (controlled) {
                        // 只读/隐藏：保留 DB 原值（不取请求值），防篡改
                        Object dbVal = originalRow != null ? originalRow.get(dbName.toLowerCase()) : null;
                        setParts.add("`" + dbName + "`=?");
                        updateValues.add(BizValueCoercer.coerce(colMeta.get(dbName.toLowerCase()), dbVal));
                        continue;
                    }

                    setParts.add("`" + dbName + "`=?");
                    Object value = null;
                    if (isFormulaField) {
                        value = formulaValues.get(field.getFieldname());
                        if (value == null) value = formulaValues.get(dbName.toLowerCase());
                    }
                    if (value == null) value = finalFieldValues.get(dbName);
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
     * 行级「写」权限校验（编辑/删除他人创建的数据）。
     *
     * <p>对齐 E9：创建人对本人数据有完全控制权；命中共享规则（canViewAll）可改删全部。
     * 仅当目标表存在创建人列时校验 —— 无 creator 列的历史表无法判定归属，跳过以免阻断。</p>
     *
     * @throws ServiceException 当前用户不在该行的数据范围内
     */
    private void assertRowWriteScope(String tableName, Long formId, Long dataId) {
        assertRowWriteScope(tableName, formId, dataId, null);
    }

    /**
     * 行级写校验（含数据权限行规则，S2.1）。
     *
     * @param resourceCode 资源码（来自通用列表请求）；为空则只套建模权限，不套 blade_scope
     */
    private void assertRowWriteScope(String tableName, Long formId, Long dataId, String resourceCode) {
        if (!rowWriteScopeEnforce || dataId == null || formId == null) {
            return;
        }
        try {
            Map<String, BizValueCoercer.ColumnMeta> colMeta = BizValueCoercer.loadColumnMeta(jdbcTemplate, tableName);
            String creatorCol = colMeta.containsKey("modedatacreator") ? "modedatacreator"
                : (colMeta.containsKey("modedatacreater") ? "modedatacreater" : null);
            if (creatorCol == null) {
                if (rowWriteScopeStrict) {
                    throw new ServiceException("数据表缺少创建人列，无法判定数据归属，拒绝写入");
                }
                return; // 无创建人列：宽松模式跳过（历史表兼容）
            }
            List<Object> creatorVals = jdbcTemplate.queryForList(
                "SELECT `" + creatorCol + "` FROM `" + tableName + "` WHERE id = ?", Object.class, dataId);
            if (creatorVals == null || creatorVals.isEmpty() || creatorVals.get(0) == null) {
                if (rowWriteScopeStrict) {
                    throw new ServiceException("数据行创建人为空，无法判定数据归属，拒绝写入");
                }
                return; // 空创建人：宽松模式跳过
            }
            Object cv = creatorVals.get(0);
            Long creatorId = (cv instanceof Number) ? ((Number) cv).longValue() : null;
            if (creatorId == null) {
                if (rowWriteScopeStrict) {
                    throw new ServiceException("数据行创建人格式非法，拒绝写入");
                }
                return;
            }
            Long realModeId = formRightService.resolveModeIdByFormId(formId);
            if (realModeId == null) {
                realModeId = formId;
            }
            // 数据范围权限列（如 create_user）可能与 modedatacreator 不一致：
            // 列表/详情已按 RowScope 放行，写链路也必须按同一行范围判断，
            // 否则会出现「列表看得到、详情进得去、保存却提示他人数据」。
            DataScope currentScope = formRightService.resolveDataScope(realModeId);
            boolean inEditableScope = currentScope != null
                && currentScope.getEditableRowScopes() != null
                && !currentScope.getEditableRowScopes().isEmpty()
                && rowMatchesScopes(currentScope, tableName, dataId, currentScope.getEditableRowScopes());
            if (!inEditableScope && !formRightService.canEditDataRow(realModeId, creatorId, dataId)) {
                throw new ServiceException("无权编辑他人创建的数据（仅查看共享）");
            }
            // 数据权限优先级：仅「未配建模权限」时回退套 blade_scope 行规则；
            // 配了建模权限则已由 canEditDataRow 校验，不再叠加（与读侧 pageFormDataByScope 同口径）。
            if (!StringUtil.isBlank(resourceCode) && !formRightService.hasModelRightConfig(realModeId)) {
                assertDataScopeWrite(tableName, resourceCode, dataId);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            if (rowWriteScopeStrict) {
                log.error("[formmode] 行级写权限校验异常，严格模式拒绝写入. formId={}, dataId={}, err={}",
                    formId, dataId, e.getMessage());
                throw new ServiceException("行级数据权限校验失败，拒绝写入");
            }
            // 宽松模式：校验链路异常 fail-open 放行（历史兼容），留存告警
            log.warn("[formmode] 行级写权限校验异常，按放行处理. formId={}, dataId={}, err={}",
                formId, dataId, e.getMessage());
        }
    }

    /**
     * 写链路数据权限校验（S2.1）：目标行的权限列值必须落在数据权限允许的值域内。
     *
     * <p>读侧用 SQL 过滤行，写侧没有 SQL 改写，故显式取目标行的 scopeColumn 值，
     * 用同一个 DataScopeValueResolver 解析允许集合并做包含判定。无规则 / 规则放行 → 通过；
     * 规则拒绝 / 列非法 / 行值不在集合 → 拒绝。</p>
     */
    private void assertDataScopeWrite(String tableName, String resourceCode, Long dataId) {
        ScopeRule rule = loadScopeRule(resourceCode,
            AuthUtil.getUser() == null ? null : AuthUtil.getUser().getRoleId());
        if (rule == null) {
            return; // 该资源未配置数据权限
        }
        if (rule.isLoadFailed()) {
            throw new ServiceException("数据权限规则读取失败，拒绝写入");
        }
        DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
            rule.getScopeType(), rule.getScopeColumn(), rule.getScopeValue(),
            AuthUtil.getUser(), this::queryChildDeptIds);
        if (value.isUnrestricted()) {
            return;
        }
        if (value.isRejected()) {
            throw new ServiceException("数据权限规则无法应用，拒绝写入");
        }
        String col = value.getColumn() == null ? "" : value.getColumn().trim();
        if (!SAFE_COLUMN.matcher(col).matches()) {
            throw new ServiceException("数据权限权限列非法，拒绝写入");
        }
        // 取目标行的权限列值
        List<Object> vals = jdbcTemplate.queryForList(
            "SELECT `" + col + "` FROM `" + tableName + "` WHERE id = ?", Object.class, dataId);
        if (vals == null || vals.isEmpty() || !(vals.get(0) instanceof Number)) {
            throw new ServiceException("数据行权限列无有效值，拒绝写入");
        }
        long rowVal = ((Number) vals.get(0)).longValue();
        if (value.getIds() == null || !value.getIds().contains(rowVal)) {
            log.warn("[formmode][AUDIT] 写链路数据权限拦截. table={}, dataId={}, col={}, rowVal={}, allowed={}",
                tableName, dataId, col, rowVal, value.getIds());
            throw new ServiceException("目标数据不在当前用户的数据权限范围内");
        }
    }

    /**
     * 矩阵主体（objtype=7）单条共享：扫描共享表中矩阵规则，按数据行字段值解析当前用户是否命中。
     *
     * <p>返回命中的 dataId 集合，与常规单条共享合并。运行时求值、不物化，异常 fail-closed（不计入）。</p>
     */
    private List<Long> matrixSharedDataIds(Long modeId, String tableName, Long userId) {
        List<Long> hit = new ArrayList<>();
        if (modeId == null || userId == null || tableName == null) {
            return hit;
        }
        try {
            List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT data_id, condition_field, matrix_value_field, objid FROM modedatashare_single "
                    + "WHERE modeid=? AND objtype=7 AND tenant_id=?",
                modeId, Func.toStr(SecureUtil.getTenantId(), "000000"));
            for (Map<String, Object> r : rules) {
                Object dataIdObj = r.get("data_id");
                Object matrixIdObj = r.get("objid");
                Object bizFieldObj = r.get("condition_field");
                if (!(dataIdObj instanceof Number) || !(matrixIdObj instanceof Number) || bizFieldObj == null) {
                    continue;
                }
                long dataId = ((Number) dataIdObj).longValue();
                long matrixId = ((Number) matrixIdObj).longValue();
                String bizField = String.valueOf(bizFieldObj).trim().toLowerCase();
                String valueField = r.get("matrix_value_field") == null ? null : String.valueOf(r.get("matrix_value_field"));
                // 取该数据行的字段值（含列名大小写归一）
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM `" + tableName + "` WHERE id=?", dataId);
                if (rows == null || rows.isEmpty()) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                rows.get(0).forEach((k, v) -> row.put(k.toLowerCase(), v));
                if (matrixSubjectResolver.matches(matrixId, bizField, valueField, row, userId)) {
                    hit.add(dataId);
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 矩阵共享扫描失败，忽略. modeId={}, table={}, err={}", modeId, tableName, e.getMessage());
        }
        return hit;
    }

    /** 单行矩阵共享判定（详情读取用）：该 dataId 是否有 objtype=7 且当前用户命中的矩阵共享 */
    private boolean matrixRowShared(Long modeId, Long dataId, Map<String, Object> row, Long userId) {
        if (modeId == null || dataId == null || row == null || userId == null) {
            return false;
        }
        try {
            List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT objid, condition_field, matrix_value_field FROM modedatashare_single "
                    + "WHERE modeid=? AND data_id=? AND objtype=7 AND tenant_id=?",
                modeId, dataId, Func.toStr(SecureUtil.getTenantId(), "000000"));
            for (Map<String, Object> r : rules) {
                Object matrixIdObj = r.get("objid");
                Object bizFieldObj = r.get("condition_field");
                if (!(matrixIdObj instanceof Number) || bizFieldObj == null) continue;
                String valueField = r.get("matrix_value_field") == null ? null : String.valueOf(r.get("matrix_value_field"));
                if (matrixSubjectResolver.matches(((Number) matrixIdObj).longValue(),
                    String.valueOf(bizFieldObj), valueField, row, userId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 单行矩阵共享判定失败，按无权限. modeId={}, dataId={}, err={}",
                modeId, dataId, e.getMessage());
        }
        return false;
    }

    /** 把矩阵共享命中的 dataId 合并进 scope.sharedDataIds（去重） */
    private void mergeMatrixShared(DataScope scope, Long modeId, String tableName) {
        if (scope == null || scope.isCanViewAll()) {
            return;
        }
        List<Long> hit = matrixSharedDataIds(modeId, tableName, currentUserId());
        if (hit.isEmpty()) {
            return;
        }
        List<Long> merged = new ArrayList<>();
        if (scope.getSharedDataIds() != null) merged.addAll(scope.getSharedDataIds());
        for (Long id : hit) if (!merged.contains(id)) merged.add(id);
        scope.setSharedDataIds(merged);
    }

    /**
     * 行级范围：本人创建 OR 单条共享给我的数据行。
     *
     * <p>仅在非全量可见时调用。sharedDataIds 来自服务端查询（受控数字 ID），参数化拼接无注入风险。</p>
     */
    private void appendCreatorAndSharedFilter(DataScope scope, Set<String> tableCols,
                                              StringBuilder countSql, StringBuilder dataSql, List<Object> params) {
        List<Long> shared = scope.getSharedDataIds();
        boolean hasShared = shared != null && !shared.isEmpty();
        String creatorCol = tableCols.contains("modedatacreator") ? "modedatacreator"
            : (tableCols.contains("modedatacreater") ? "modedatacreater" : null);
        boolean hasCreator = creatorCol != null && scope.getCreatorId() != null;
        if (hasCreator && hasShared) {
            String marks = String.join(",", java.util.Collections.nCopies(shared.size(), "?"));
            String clause = " AND (`" + creatorCol + "` = ? OR `id` IN (" + marks + "))";
            countSql.append(clause);
            dataSql.append(clause);
            params.add(scope.getCreatorId());
            params.addAll(shared);
        } else if (hasCreator) {
            String clause = " AND `" + creatorCol + "` = ?";
            countSql.append(clause);
            dataSql.append(clause);
            params.add(scope.getCreatorId());
        } else if (hasShared) {
            String marks = String.join(",", java.util.Collections.nCopies(shared.size(), "?"));
            String clause = " AND `id` IN (" + marks + ")";
            countSql.append(clause);
            dataSql.append(clause);
            params.addAll(shared);
        } else {
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
        }
    }

    /** 五档数据范围：多个 RowScope 之间 OR；同列 ids 已由权限服务合并。 */
    private void appendRowScopes(DataScope scope, Set<String> tableCols,
                                 StringBuilder countSql, StringBuilder dataSql, List<Object> params) {
        List<DataScope.RowScope> scopes = scope.getRowScopes();
        StringBuilder or = new StringBuilder();
        boolean appended = false;
        for (DataScope.RowScope rs : scopes) {
            String col = rs.getColumn() == null ? "" : rs.getColumn().trim().toLowerCase();
            if (!SAFE_COLUMN.matcher(col).matches() || !tableCols.contains(col)
                || rs.getIds() == null || rs.getIds().isEmpty()) continue;
            if (appended) or.append(" OR ");
            String marks = String.join(",", java.util.Collections.nCopies(rs.getIds().size(), "?"));
            or.append('`').append(col).append("` IN (").append(marks).append(')');
            params.addAll(rs.getIds());
            appended = true;
        }
        if (!appended) {
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
        } else {
            String clause = " AND (" + or + ")";
            countSql.append(clause);
            dataSql.append(clause);
        }
    }

    /**
     * 条件化授权：把结构化行条件安全拼入 SQL（字段名白名单 + 操作符枚举 + 值参数化）。
     *
     * <p>任一校验不通过一律追加 {@code AND 1=0}（fail-closed），绝不拼接未校验的字段/值。</p>
     *
     * @return true 表示成功追加条件
     */
    private boolean appendRowCondition(DataScope scope, Set<String> tableCols,
                                       StringBuilder countSql, StringBuilder dataSql, List<Object> params) {
        DataScope.RowCondition cond = scope == null ? null : scope.getRowCondition();
        if (cond == null || cond.getField() == null || cond.getOp() == null || cond.getValue() == null) {
            return false;
        }
        String field = cond.getField().trim().toLowerCase();
        String op = cond.getOp().trim().toLowerCase();
        String value = cond.getValue();
        // 字段名白名单：必须匹配安全列名且为目标表真实存在的列
        if (!SAFE_COLUMN.matcher(field).matches() || !tableCols.contains(field)) {
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
            log.warn("[formmode] 条件化授权字段名非法，拒绝. field={}", cond.getField());
            return true;
        }
        String col = "`" + field + "`";
        switch (op) {
            case "eq": case "ne": case "gt": case "ge": case "lt": case "le": case "like":
                String sqlOp = switch (op) {
                    case "eq" -> "="; case "ne" -> "<>"; case "gt" -> ">";
                    case "ge" -> ">="; case "lt" -> "<"; case "le" -> "<=";
                    default -> "LIKE";
                };
                countSql.append(" AND ").append(col).append(" ").append(sqlOp).append(" ?");
                dataSql.append(" AND ").append(col).append(" ").append(sqlOp).append(" ?");
                params.add("like".equals(op) ? "%" + value + "%" : value);
                return true;
            case "in":
                String[] parts = value.split(",");
                List<String> clean = new ArrayList<>();
                for (String p : parts) {
                    String t = p == null ? "" : p.trim();
                    if (!t.isEmpty()) clean.add(t);
                }
                if (clean.isEmpty()) {
                    countSql.append(" AND 1=0");
                    dataSql.append(" AND 1=0");
                    return true;
                }
                String marks = String.join(",", java.util.Collections.nCopies(clean.size(), "?"));
                countSql.append(" AND ").append(col).append(" IN (").append(marks).append(")");
                dataSql.append(" AND ").append(col).append(" IN (").append(marks).append(")");
                params.addAll(clean);
                return true;
            default:
                countSql.append(" AND 1=0");
                dataSql.append(" AND 1=0");
                log.warn("[formmode] 条件化授权操作符非法，拒绝. op={}", op);
                return true;
        }
    }

    /** 按表名 + 数据ID 判断当前行是否命中指定行范围（写链路没有完整 row map 时使用） */
    private boolean rowMatchesScopes(DataScope scope, String tableName, Long dataId,
                                     List<DataScope.RowScope> rowScopes) {
        if (scope == null || rowScopes == null || rowScopes.isEmpty() || dataId == null) return false;
        try {
            for (DataScope.RowScope rs : rowScopes) {
                String col = rs.getColumn() == null ? "" : rs.getColumn().trim().toLowerCase();
                if (!SAFE_COLUMN.matcher(col).matches() || rs.getIds() == null || rs.getIds().isEmpty()) continue;
                List<Object> vals = jdbcTemplate.queryForList(
                    "SELECT `" + col + "` FROM `" + tableName + "` WHERE id = ?", Object.class, dataId);
                if (!vals.isEmpty() && vals.get(0) instanceof Number
                    && rs.getIds().contains(((Number) vals.get(0)).longValue())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 写链路行范围校验失败. table={}, dataId={}, err={}", tableName, dataId, e.getMessage());
        }
        return false;
    }

    /** 判断当前行是否命中第五步数据范围（与 appendRowScopes 的 SQL 同口径） */
    private boolean matchesRowScopes(DataScope scope, Map<String, Object> rowLower) {
        if (scope == null || scope.getRowScopes() == null || scope.getRowScopes().isEmpty()) return false;
        for (DataScope.RowScope rs : scope.getRowScopes()) {
            String col = rs.getColumn() == null ? "" : rs.getColumn().trim().toLowerCase();
            if (!SAFE_COLUMN.matcher(col).matches() || rs.getIds() == null || rs.getIds().isEmpty()) continue;
            Object actual = rowLower.get(col);
            if (!(actual instanceof Number)) continue;
            long value = ((Number) actual).longValue();
            if (rs.getIds().contains(value)) return true;
        }
        return false;
    }

    /**
     * 条件化授权：判断单行是否满足结构化条件（供单条详情读取用，与 SQL 过滤同口径）。
     *
     * <p>字段不存在 / 操作符非法 / 值无法转换 → 视为不满足（fail-closed）。</p>
     */
    private boolean matchesRowCondition(DataScope scope, Map<String, Object> row) {
        DataScope.RowCondition cond = scope == null ? null : scope.getRowCondition();
        if (cond == null || cond.getField() == null || cond.getOp() == null || cond.getValue() == null) {
            return true; // 无条件 → 放行
        }
        String field = cond.getField().trim().toLowerCase();
        String op = cond.getOp().trim().toLowerCase();
        Object actual = row.get(field);
        if (actual == null) {
            return false;
        }
        try {
            String av = String.valueOf(actual);
            String cv = cond.getValue();
            int cmp = compareNumeric(av, cv);
            return switch (op) {
                case "eq" -> av.equals(cv) || cmp == 0;
                case "ne" -> !av.equals(cv) && cmp != 0;
                case "gt" -> cmp > 0;
                case "ge" -> cmp >= 0;
                case "lt" -> cmp < 0;
                case "le" -> cmp <= 0;
                case "like" -> av.contains(cv.replace("%", ""));
                case "in" -> java.util.Arrays.stream(cv.split(","))
                    .map(String::trim).anyMatch(av::equals);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /** 数值比较（两边都能转 double 时返回比较结果，否则返回 Integer.MIN_VALUE 表示不可比） */
    private static int compareNumeric(String a, String b) {
        try {
            return Double.compare(Double.parseDouble(a.trim()), Double.parseDouble(b.trim()));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
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

    private Long currentPrimaryDeptId() {
        try {
            String deptId = AuthUtil.getUser() == null ? null : AuthUtil.getUser().getDeptId();
            if (StringUtil.isBlank(deptId)) return null;
            String first = deptId.split(",")[0].trim();
            return first.matches("\\d+") ? Long.valueOf(first) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long currentTenantId() {
        try {
            // 优先取创建人在 blade_user 中的租户，确保业务行归属创建人租户；
            // 用户查不到时再回退当前请求租户。
            Long userId = currentUserId();
            if (userId != null) {
                List<String> rows = jdbcTemplate.queryForList(
                    "SELECT tenant_id FROM blade_user WHERE id=? AND is_deleted=0", String.class, userId);
                if (!rows.isEmpty()) {
                    String userTenant = rows.get(0);
                    if (!StringUtil.isBlank(userTenant)) {
                        String normalized = userTenant.trim();
                        return normalized.matches("\\d+") ? Long.valueOf(normalized) : null;
                    }
                }
            }
            String tenantId = Func.toStr(SecureUtil.getTenantId(), "");
            return tenantId.matches("\\d+") ? Long.valueOf(tenantId) : null;
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
        Long resolvedModeId = formRightService.resolveModeIdByFormId(formId);
        final Long realModeId = (resolvedModeId == null) ? formId : resolvedModeId;
        final DataScope scope = formRightService.resolveDataScope(realModeId);

        final String finalTable = tableName;
        return TableNameContextHolder.executeWithTableName(finalTable, () -> {
            String sql = "SELECT * FROM `" + finalTable + "` WHERE id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dataId);
            if (rows.isEmpty()) return null;

            Map<String, Object> row = rows.get(0);

            // 数据范围：与动态列表 SQL 同口径——先判断第五步配置的行范围
            // （scopeColumn/scopeValue），再回退本人创建 / 单条共享 / 矩阵共享。
            // 旧逻辑只按 modedatacreator 判断本人，配置 create_user 等权限列时会出现
            // 「列表看得到，点详情报数据不存在」。
            if (!scope.isCanViewAll()) {
                if (scope.isNoData()) {
                    return null;
                }
                Long creatorOfRow = parseRowCreator(row);
                boolean inRowScope = false;
                boolean selfCreated = Objects.equals(creatorOfRow, scope.getCreatorId());
                boolean shared = scope.getSharedDataIds() != null && scope.getSharedDataIds().contains(dataId);
                Map<String, Object> rowLower = new LinkedHashMap<>();
                row.forEach((k, v) -> rowLower.put(k.toLowerCase(), v));
                inRowScope = matchesRowScopes(scope, rowLower);
                boolean matrixShared = (!inRowScope && !selfCreated && !shared)
                    && matrixRowShared(realModeId, dataId, rowLower, currentUserId());
                if (!inRowScope && !selfCreated && !shared && !matrixShared) {
                    return null;
                }
            }
            // 条件化授权：单行必须满足条件（与列表过滤同口径）
            if (!matchesRowCondition(scope, row)) {
                return null;
            }
            FormDataVO vo = new FormDataVO();
            vo.setId(dataId);
            vo.setModeid(formId);

            // 过滤系统字段 + T1.1 脱敏（移除当前用户不可见字段）
            Set<String> invisibleCols = loadInvisibleColumnNames(realModeId, formId);
            if (!invisibleCols.isEmpty()) {
                log.info("[formmode][AUDIT] 详情字段脱敏. modeId={}, userId={}, maskedCols={}",
                    realModeId, currentUserId(), invisibleCols);
            }
            Map<String, Object> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (!isSystemField(key) && !invisibleCols.contains(key.toLowerCase())) {
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
        Long resolvedModeId = formRightService.resolveModeIdByFormId(modeId);
        final Long realModeId = (resolvedModeId == null) ? modeId : resolvedModeId;
        final DataScope scope = formRightService.resolveDataScope(realModeId);
        // 矩阵主体（objtype=7）：按行字段值解析的单条共享，合并进 scope.sharedDataIds
        mergeMatrixShared(scope, realModeId, mainTableName);

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            Map<String, BizValueCoercer.ColumnMeta> colMeta =
                BizValueCoercer.loadColumnMeta(jdbcTemplate, mainTableName);
            Set<String> tableCols = new HashSet<>(colMeta.keySet());

            // 构建查询
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM `" + mainTableName + "` WHERE 1=1");
            StringBuilder dataSql = new StringBuilder("SELECT * FROM `" + mainTableName + "` WHERE 1=1");
            List<Object> queryParams = new ArrayList<>();

            // 数据范围：命中方案但未配规则 → 无可见数据；五档行范围优先；
            // 完全未命中方案时才回退本人创建 OR 单条共享。
            if (!scope.isCanViewAll()) {
                if (scope.isNoData()) {
                    countSql.append(" AND 1=0");
                    dataSql.append(" AND 1=0");
                } else if (scope.getRowScopes() != null && !scope.getRowScopes().isEmpty()) {
                    appendRowScopes(scope, tableCols, countSql, dataSql, queryParams);
                } else {
                    appendCreatorAndSharedFilter(scope, tableCols, countSql, dataSql, queryParams);
                }
            }
            // 条件化授权：命中共享规则时行还需满足结构化条件（与范围过滤 AND 叠加）
            appendRowCondition(scope, tableCols, countSql, dataSql, queryParams);

            // 添加查询条件（白名单校验：防止列名逃逸注入，与 pageFormDataByScope 同口径）
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String key = entry.getKey();
                    if (key == null || entry.getValue() == null || isSystemField(key)) {
                        continue;
                    }
                    if (!SAFE_COLUMN.matcher(key).matches() || !tableCols.contains(key.toLowerCase())) {
                        continue;
                    }
                    countSql.append(" AND `").append(key).append("` LIKE ?");
                    dataSql.append(" AND `").append(key).append("` LIKE ?");
                    queryParams.add("%" + entry.getValue() + "%");
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

            // T1.1 读路径脱敏：按当前用户字段权限剔除不可见列（防前端隐藏但接口裸奔）
            maskInvisibleFields(records, loadInvisibleColumnNames(realModeId, modeId), realModeId, currentUserId());

            Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), total);
            result.setRecords(records);
            return result;
        });
    }

    @Override
    public Page<Map<String, Object>> pageFormDataByScope(Long formId, Page<?> page,
                                                          Map<String, Object> params, String resourceCode) {
        String mainTableName = resolveMainTableName(formId);
        if (mainTableName == null) {
            return new Page<>(page.getCurrent(), page.getSize());
        }
        // formmode 自身的行级数据范围（modedatasharerule）：与 pageFormData 同口径，叠加生效
        Long resolvedModeId = formRightService.resolveModeIdByFormId(formId);
        final Long realModeId = (resolvedModeId == null) ? formId : resolvedModeId;
        final DataScope scope = formRightService.resolveDataScope(realModeId);
        final ScopeRule rule = loadScopeRule(resourceCode,
            AuthUtil.getUser() == null ? null : AuthUtil.getUser().getRoleId());
        // 权限优先级：配了建模权限（三表任一有记录）→ 建模权限优先，不叠加 blade_scope；
        // 没配建模权限 → 回退数据权限（blade_scope 五档 + scope_field 列裁剪）。
        final boolean useScopeRule = !formRightService.hasModelRightConfig(realModeId);
        final String table = mainTableName;
        // 矩阵主体（objtype=7）：合并单条共享中按行字段值解析出的可访问行
        mergeMatrixShared(scope, realModeId, table);

        return TableNameContextHolder.executeWithTableName(table, () -> {
            Map<String, BizValueCoercer.ColumnMeta> colMeta =
                BizValueCoercer.loadColumnMeta(jdbcTemplate, table);
            Set<String> tableCols = new HashSet<>(colMeta.keySet());

            // 列清单：建模权限优先时不按 blade_scope.scope_field 裁剪（前端按建模字段权限控制）；
            // 仅回退数据权限时才用 scope_field 裁剪，非法/不存在的列剔除后回落 SELECT *
            String selectCols = "*";
            if (useScopeRule && rule != null && StringUtil.isNotBlank(rule.getScopeField())) {
                String sf = rule.getScopeField().trim();
                if (!"*".equals(sf)) {
                    List<String> cols = Arrays.stream(sf.split(","))
                        .map(String::trim)
                        .filter(StringUtil::isNotBlank)
                        .filter(c -> SAFE_COLUMN.matcher(c).matches())
                        .filter(tableCols::contains)
                        .distinct()
                        .collect(Collectors.toList());
                    if (!cols.isEmpty()) {
                        // 主键必须保留：通用列表靠 id 做行主键（行编辑/行选择/新增行都依赖它）。
                        // scope_field 若不含 id，裁剪后数据行将失去主键（前端 data-row-key 为空、
                        // 行编辑态会因行键全等而串到所有行），故这里强制补回。
                        boolean hasId = cols.stream().anyMatch(c -> "id".equalsIgnoreCase(c));
                        if (!hasId) {
                            cols = new ArrayList<>(cols);
                            cols.add("id");
                        }
                        selectCols = cols.stream().map(c -> "`" + c + "`").collect(Collectors.joining(","));
                    }
                }
            }

            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM `" + table + "` WHERE 1=1");
            StringBuilder dataSql = new StringBuilder("SELECT " + selectCols + " FROM `" + table + "` WHERE 1=1");
            List<Object> queryParams = new ArrayList<>();

            // ① formmode 行级范围：命中方案但未配规则 → 无可见数据；五档行范围优先；
            //    完全未命中方案时才回退本人创建 OR 单条共享。
            if (!scope.isCanViewAll()) {
                if (scope.isNoData()) {
                    countSql.append(" AND 1=0");
                    dataSql.append(" AND 1=0");
                } else if (scope.getRowScopes() != null && !scope.getRowScopes().isEmpty()) {
                    appendRowScopes(scope, tableCols, countSql, dataSql, queryParams);
                } else {
                    appendCreatorAndSharedFilter(scope, tableCols, countSql, dataSql, queryParams);
                }
            }

            // ①-b 条件化授权：命中共享规则时行还需满足结构化条件
            appendRowCondition(scope, tableCols, countSql, dataSql, queryParams);

            // ② 数据权限规则行级过滤（blade_scope_data）——仅「未配建模权限」时回退使用；
            //    配了建模权限则已由 ① 过滤，不再叠加，避免两套规则互相收窄导致配置难懂。
            if (useScopeRule) {
                applyScopeRuleFilter(rule, tableCols, countSql, dataSql);
            }

            // ③ 查询条件（与 pageFormData 同口径：LIKE 模糊）
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String key = entry.getKey();
                    if (key == null || entry.getValue() == null || isSystemField(key)) {
                        continue;
                    }
                    if (!SAFE_COLUMN.matcher(key).matches() || !tableCols.contains(key.toLowerCase())) {
                        continue;
                    }
                    countSql.append(" AND `").append(key).append("` LIKE ?");
                    dataSql.append(" AND `").append(key).append("` LIKE ?");
                    queryParams.add("%" + entry.getValue() + "%");
                }
            }

            Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, queryParams.toArray());

            long offset = (page.getCurrent() - 1) * page.getSize();
            dataSql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
            List<Object> pageParams = new ArrayList<>(queryParams);
            pageParams.add(page.getSize());
            pageParams.add(offset);

            List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql.toString(), pageParams.toArray());

            // T1.1 读路径脱敏：pageByScope 同样按用户字段权限剔除不可见列
            maskInvisibleFields(records, loadInvisibleColumnNames(realModeId, formId), realModeId, currentUserId());

            Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), total);
            result.setRecords(records);
            return result;
        });
    }

    /**
     * 按单元格更新单条业务数据（通用列表行内编辑）。
     *
     * <p>只改一格、其余字段保留库中原值（整体覆盖会把未渲染字段清空）；
     * 字段名必须是合法标识符且真实存在于主表；值按目标列类型转换（复用 BizValueCoercer）。</p>
     */
    @Override
    public Boolean updateFormDataCell(Long formId, Long dataId, String fieldName, Object value) {
        return updateFormDataCell(formId, dataId, fieldName, value, null);
    }

    @Override
    public Boolean updateFormDataCell(Long formId, Long dataId, String fieldName, Object value, String resourceCode) {
        if (formId == null || dataId == null) {
            throw new ServiceException("参数不完整（formId / dataId）");
        }
        String col = fieldName == null ? "" : fieldName.trim();
        if (!SAFE_COLUMN.matcher(col).matches()) {
            throw new ServiceException("字段名非法: " + fieldName);
        }
        // 字段权限：显式配置了且不可编辑 → 拒绝行内编辑（防越权写隐藏/只读字段）
        if (loadNonEditableFields(formId).contains(col.toLowerCase())) {
            throw new ServiceException("无权编辑该字段: " + fieldName);
        }
        String table = resolveMainTableName(formId);
        if (table == null) {
            throw new ServiceException("未找到该表单的主表");
        }
        final String finalCol = col;
        final Object rawValue = value;
        return Boolean.TRUE.equals(TableNameContextHolder.executeWithTableName(table, () -> {
            Map<String, BizValueCoercer.ColumnMeta> colMeta = BizValueCoercer.loadColumnMeta(jdbcTemplate, table);
            BizValueCoercer.ColumnMeta meta = colMeta.get(finalCol.toLowerCase());
            if (meta == null) {
                throw new ServiceException("字段不存在于主表: " + finalCol);
            }
            Set<String> tableCols = new HashSet<>(colMeta.keySet());
            List<String> setParts = new ArrayList<>();
            List<Object> setValues = new ArrayList<>();
            setParts.add("`" + finalCol + "`=?");
            setValues.add(BizValueCoercer.coerce(meta, rawValue));

            // 修改人/修改时间审计列（只写主表真实存在的列；取不到用户时不动修改人，避免清空）
            Long editor = currentUserId();
            if (editor != null) {
                appendAuditSet(setParts, setValues, tableCols, new String[]{"modedatamodifier"}, editor);
            }
            appendAuditSet(setParts, setValues, tableCols, new String[]{"modedatamodifydate"}, LocalDate.now().toString());
            appendAuditSet(setParts, setValues, tableCols, new String[]{"modedatamodifytime"},
                LocalTime.now().toString().substring(0, 8));
            appendAuditSet(setParts, setValues, tableCols, new String[]{"lastModDate"}, LocalDate.now().toString());
            appendAuditSet(setParts, setValues, tableCols, new String[]{"lastModTime"},
                LocalTime.now().toString().substring(0, 8));

            // 行级写权限：仅本人创建 / 命中共享规则可编辑 + 通用数据权限行规则
            assertRowWriteScope(table, formId, dataId, resourceCode);

            String sql = "UPDATE `" + table + "` SET " + String.join(",", setParts) + " WHERE id=?";
            setValues.add(dataId);
            return jdbcTemplate.update(sql, setValues.toArray()) > 0;
        }));
    }

    /**
     * 应用数据权限规则的行级过滤：{@code `col` IN (值域)}。
     *
     * <p>值域语义统一由 {@link DataScopeValueResolver} 给出（与上游
     * {@code BladeDataScopeHandler#sqlCondition} 对齐）：全部(1)/管理员 → 不过滤；
     * 本人(2) → 当前用户；本部门(3) → 当前部门；本部门及子级(4) → 当前部门 + 其子孙部门；
     * 自定义(5) → 受限形态的 scope_value。</p>
     *
     * <p><b>安全口径</b>：规则存在但无法安全应用时（类型未知 / 自定义值域不是受限形态 /
     * 权限列非法或不在表内），一律按「无可见数据」处理（1=0），
     * <b>绝不降级为「不过滤」</b>——否则「配了权限」会变成「实际没限制」，属越权。
     * 上游对同样情形的表现是拼出非法 SQL 让查询报错（同样 fail-closed）。</p>
     */
    private void applyScopeRuleFilter(ScopeRule rule, Set<String> tableCols,
                                      StringBuilder countSql, StringBuilder dataSql) {
        if (rule == null) {
            return;
        }
        // S2.2 fail-closed：规则读取异常时拒绝全部（而非 fail-open 放行）
        if (rule.isLoadFailed()) {
            log.error("[formmode] 数据权限规则读取失败，按无可见数据处理. resourceCode={}", rule.getResourceCode());
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
            return;
        }
        DataScopeValueResolver.ScopeValue value = DataScopeValueResolver.resolve(
            rule.getScopeType(), rule.getScopeColumn(), rule.getScopeValue(),
            AuthUtil.getUser(), this::queryChildDeptIds);

        // 全部数据 / 管理员：不过滤
        if (value.isUnrestricted()) {
            return;
        }
        // 规则无法安全应用：拒绝（安全优先），不降级为「不过滤」
        if (value.isRejected()) {
            log.error("[formmode] 数据权限规则无法安全应用，按无可见数据处理. resourceCode={}, scopeType={}, reason={}",
                rule.getResourceCode(), rule.getScopeType(), value.getReason());
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
            return;
        }
        String col = value.getColumn() == null ? "" : value.getColumn().trim();
        if (!SAFE_COLUMN.matcher(col).matches() || !tableCols.contains(col)) {
            log.error("[formmode] 数据权限规则的权限列非法，按无可见数据处理. resourceCode={}, scopeColumn={}",
                rule.getResourceCode(), value.getColumn());
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
            return;
        }
        appendInCondition(countSql, dataSql, col, value.getIds());
    }

    /** 追加 {@code AND `col` IN (...)}；值域为空 → 追加 1=0（无可见数据，安全优先） */
    private void appendInCondition(StringBuilder countSql, StringBuilder dataSql, String col, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            countSql.append(" AND 1=0");
            dataSql.append(" AND 1=0");
            return;
        }
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        countSql.append(" AND `").append(col).append("` IN (").append(in).append(")");
        dataSql.append(" AND `").append(col).append("` IN (").append(in).append(")");
    }

    /**
     * 查询某部门的全部子孙部门 id。
     *
     * <p><b>为什么不能用 {@code ancestors LIKE '%,deptId,%'}</b>：本项目 {@code blade_dept.ancestors}
     * 的真实形态是 {@code 0,1000000008}（顶级部门为 {@code 0}，<b>末尾没有逗号</b>），
     * 该模式要求 deptId 前后都带逗号，会<b>漏掉所有直属子部门</b>（子部门的 ancestors 恰好结尾无逗号）。
     * 这里改成「两端补逗号再匹配」：既能命中直属子部门（{@code ,0,1000000008,}），
     * 又不会像上游的 {@code LIKE '%deptId%'} 那样把 {@code 10000000081} 误判为子部门。</p>
     */
    private List<Long> queryChildDeptIds(Long deptId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM blade_dept WHERE is_deleted = 0 AND CONCAT(',', ancestors, ',') LIKE ?",
                "%," + deptId + ",%");
            List<Long> ids = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                if (id instanceof Number) {
                    ids.add(((Number) id).longValue());
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("[formmode] 查询子孙部门失败，仅按本部门过滤. deptId={}, err={}", deptId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 按资源编号读取数据权限规则（blade_scope_data）；无规则返回 null（不套规则） */
    /**
     * 读取数据权限规则（S2.2）。
     *
     * <p>优先按「角色绑定」（blade_role_scope）取，复刻框架 DataScopeClient#getDataScopeByMapper 的口径；
     * 无角色绑定时回退 resource_code。查询异常返回 loadFailed=true 的规则，由
     * applyScopeRuleFilter 追加 AND 1=0（fail-closed），不再 fail-open 放行。</p>
     */
    private ScopeRule loadScopeRule(String resourceCode, String roleId) {
        if (StringUtil.isBlank(resourceCode)) {
            return null; // 无资源码 = 该接口不接数据权限，保持放行
        }
        try {
            List<Long> roleIds = new ArrayList<>();
            if (!StringUtil.isBlank(roleId)) {
                for (String part : roleId.split(",")) {
                    try {
                        long id = Long.parseLong(part.trim());
                        if (id > 0) roleIds.add(id);
                    } catch (NumberFormatException ignored) { }
                }
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            // ① 按角色绑定取（复刻 dataByMapper：role_scope.scope_id = scope_data.id）
            if (!roleIds.isEmpty()) {
                String marks = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                rows = jdbcTemplate.queryForList(
                    "SELECT d.resource_code, d.scope_field, d.scope_column, d.scope_type, d.scope_value "
                        + "FROM blade_scope_data d INNER JOIN blade_role_scope r ON r.scope_id = d.id "
                        + "WHERE d.resource_code = ? AND r.role_id IN (" + marks + ") AND d.is_deleted = 0 "
                        + "ORDER BY d.id LIMIT 1",
                    resourceCode);
            }
            // ② 回退：按 resourceCode 取（兼容未配角色绑定的存量规则）
            if (rows.isEmpty()) {
                rows = jdbcTemplate.queryForList(
                    "SELECT resource_code, scope_field, scope_column, scope_type, scope_value "
                        + "FROM blade_scope_data WHERE resource_code = ? AND is_deleted = 0 ORDER BY id LIMIT 1",
                    resourceCode);
            }
            if (rows.isEmpty()) {
                return null; // 无规则 = 该资源未配置数据权限，放行
            }
            Map<String, Object> row = rows.get(0);
            ScopeRule rule = new ScopeRule();
            rule.setResourceCode(toStringOrNull(row.get("resource_code")));
            rule.setScopeField(toStringOrNull(row.get("scope_field")));
            rule.setScopeColumn(toStringOrNull(row.get("scope_column")));
            rule.setScopeValue(toStringOrNull(row.get("scope_value")));
            Object scopeType = row.get("scope_type");
            rule.setScopeType(scopeType instanceof Number ? ((Number) scopeType).intValue() : null);
            return rule;
        } catch (Exception e) {
            log.error("[formmode] 读取数据权限规则失败，按无可见数据处理(fail-closed). resourceCode={}, err={}",
                resourceCode, e.getMessage());
            ScopeRule failed = new ScopeRule();
            failed.setResourceCode(resourceCode);
            failed.setLoadFailed(true);
            return failed;
        }
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 数据权限规则（blade_scope_data 的一行） */
    @Data
    private static class ScopeRule {
        private String resourceCode;
        /** 可见字段（列清单，逗号串或 *） */
        private String scopeField;
        /** 行级过滤列 */
        private String scopeColumn;
        /** 1全部 2本人 3本部门 4本部门及子级 5自定义 */
        private Integer scopeType;
        /** 自定义值域 */
        private String scopeValue;
        /** 规则读取失败（fail-closed：查询异常时拒绝全部，而非放行） */
        private boolean loadFailed;
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
        // 行级写权限：仅本人创建 / 命中共享规则可删除
        assertRowWriteScope(tableName, formId, dataId);
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

    /**
     * 加载当前用户「不可见」的表列名集合（小写），供读路径脱敏（T1.1）。
     *
     * <p>权限来源：`listFieldPerm(modeId)` 按当前用户返回的三态；无配置 / 管理员豁免返回空 → 不过滤。
     * 只要返回了权限列表，就按 visible=false 的字段查出 dbName 并从响应中剔除，
     * 避免「前端隐藏但接口裸奔」。</p>
     */
    private Set<String> loadInvisibleColumnNames(Long modeId, Long billId) {
        Set<String> invisible = new HashSet<>();
        if (modeId == null) {
            return invisible;
        }
        try {
            List<FieldPermVO> perms = formRightService.listFieldPerm(modeId);
            if (perms == null || perms.isEmpty()) {
                return invisible; // 未配置 / admin 豁免
            }
            // fieldName → visible=false 集合
            Set<String> deniedNames = new HashSet<>();
            for (FieldPermVO p : perms) {
                if (p != null && p.getFieldName() != null && Boolean.FALSE.equals(p.getVisible())) {
                    deniedNames.add(p.getFieldName().toLowerCase());
                }
            }
            if (deniedNames.isEmpty()) {
                return invisible;
            }
            // fieldname → fielddbname 映射（直查，避开 mapper 兼容问题）
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT fieldname, fielddbname FROM workflow_billfield WHERE billid=? AND is_deleted=0", billId);
            for (Map<String, Object> row : rows) {
                Object name = row.get("fieldname");
                Object db = row.get("fielddbname");
                if (name == null || db == null) continue;
                String n = String.valueOf(name).toLowerCase();
                if (deniedNames.contains(n)) {
                    invisible.add(String.valueOf(db).toLowerCase());
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 加载不可见字段失败，按不脱敏处理. modeId={}, billId={}, err={}", modeId, billId, e.getMessage());
            return new HashSet<>();
        }
        return invisible;
    }

    /** 对查询结果行做字段脱敏（移除不可见列），原地修改并记录审计 */
    private void maskInvisibleFields(List<Map<String, Object>> records, Set<String> invisibleCols,
                                     Long formId, Long userId) {
        if (records == null || records.isEmpty() || invisibleCols == null || invisibleCols.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : records) {
            if (row == null) continue;
            // 主键永远保留：列表行选择、查看/编辑链接都依赖它；字段权限只控制业务列。
            row.keySet().removeIf(k -> k != null && !"id".equalsIgnoreCase(k) && invisibleCols.contains(k.toLowerCase()));
        }
        log.info("[formmode][AUDIT] 读路径字段脱敏. formId={}, userId={}, maskedCols={}, rows={}",
            formId, userId, invisibleCols, records.size());
    }

    /**
     * 加载当前用户「不可编辑」的字段名集合（小写 fieldName + dbName），供 saveBusinessData 裁剪。
     *
     * <p>无权限配置 / 查询失败 → 返回空集合（fail-open，保持流程场景兼容）；
     * 管理员由 listFieldPerm 返回空 → 不裁剪。</p>
     */
    private Set<String> loadNonEditableFields(Long formId) {
        Set<String> denied = new HashSet<>();
        if (!fieldWritePermEnforce || formId == null) {
            return denied;
        }
        try {
            Long modeId = formRightService.resolveModeIdByFormId(formId);
            if (modeId == null) {
                return denied;
            }
            List<FieldPermVO> perms = formRightService.listFieldPerm(modeId);
            if (perms == null || perms.isEmpty()) {
                return denied;
            }
            Map<String, FieldPermVO> byName = new LinkedHashMap<>();
            for (FieldPermVO p : perms) {
                if (p != null && p.getFieldName() != null) {
                    byName.put(p.getFieldName().toLowerCase(), p);
                }
            }
            // 显式配置了权限且该字段不可编辑 → 拒绝写入；同时按 dbName 索引
            List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(formId);
            if (fields != null) {
                for (WorkflowBillField f : fields) {
                    if (f == null || f.getFieldname() == null) continue;
                    FieldPermVO p = byName.get(f.getFieldname().toLowerCase());
                    if (p != null && !Boolean.TRUE.equals(p.getEditable())) {
                        denied.add(f.getFieldname().toLowerCase());
                        if (f.getFielddbname() != null) denied.add(f.getFielddbname().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[formmode] 字段权限裁剪加载失败，按 fail-open 跳过. formId={}, err={}", formId, e.getMessage());
            return new HashSet<>();
        }
        return denied;
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
        Set<String> nonEditable = loadNonEditableFields(dto.getFormId());
        if (fieldValues != null) {
            fieldValues.forEach((k, v) -> {
                if (StringUtil.isBlank(k)) {
                    return;
                }
                String ck = k.toLowerCase();
                // 字段权限：显式配置了且不可编辑 → 忽略客户端值（防越权写隐藏/只读字段）
                if (nonEditable.contains(ck)) {
                    return;
                }
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
        if (create) {
            // 数据范围权限列服务端写入，防止前端注入，也保证新建行对「本人可见」立即可见。
            putIfColumn(row, columns, "create_user", userId);
            Long deptId = currentPrimaryDeptId();
            if (deptId != null) putIfColumn(row, columns, "create_dept", deptId);
            Long tenantId = currentTenantId();
            if (tenantId != null) putIfColumn(row, columns, "tenant_id", tenantId);
        }
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
            // 行级写权限：仅本人创建 / 命中共享规则可编辑（新建分支无需校验）
            assertRowWriteScope(tableName, dto.getFormId(), dataId);
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
