package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.mapper.WorkflowBillFieldMapper;
import org.springblade.formmode.service.IFormDataService;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.utils.TableNameContextHolder;
import org.springblade.formmode.utils.TableNameUtil;
import org.springblade.formmode.vo.FormDataVO;
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

    /** 动态表名白名单：仅字母/数字/下划线，避免拼 SQL 被注入 */
    private static final Pattern SAFE_IDENT = Pattern.compile("^[A-Za-z0-9_]+$");

    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final IFormModeService formModeService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFormData(FormDataDTO formDataDTO) {
        ModeInfo modeInfo = modeInfoMapper.selectById(formDataDTO.getModeid());
        if (modeInfo == null) {
            throw new RuntimeException("模块不存在: " + formDataDTO.getModeid());
        }

        Integer billId = formDataDTO.getBillid() != null ? formDataDTO.getBillid() : modeInfo.getBillid();
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
            return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
                String deleteSql = "DELETE FROM `" + mainTableName + "` WHERE id = ?";
                jdbcTemplate.update(deleteSql, formDataDTO.getDataid());
                return formDataDTO.getDataid();
            });
        }

        // 解析字段值
        Map<String, Object> fieldValues = formDataDTO.getFieldValues();
        if (fieldValues == null) fieldValues = new HashMap<>();

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
        int userId = 0;
        try {
            userId = SecureUtil.getUserId().intValue();
        } catch (Exception ignored) {}

        if (isCreate) {
            columns.append(",modedatacreater,modedatacreatedate,modedatacreatetime,lastModDate,lastModTime");
            placeholders.append(",?,?,?,?,?");
            values.add(userId);
            values.add(currentDate);
            values.add(currentTime);
            values.add(currentDate);
            values.add(currentTime);
        } else {
            columns.append(",modedatamodifier,modedatamodifydate,modedatamodifytime,lastModDate,lastModTime");
            placeholders.append(",?,?,?,?,?");
            values.add(userId);
            values.add(currentDate);
            values.add(currentTime);
            values.add(currentDate);
            values.add(currentTime);
        }

        // 用户自定义字段值
        for (WorkflowBillField field : fields) {
            String dbName = field.getFielddbname();
            if (dbName == null) continue;

            columns.append(",").append(dbName);
            placeholders.append(",?");
            Object value = fieldValues.get(dbName);
            if (value == null) {
                value = fieldValues.get(field.getFieldname());
            }
            values.add(value != null ? value.toString() : null);
        }

        // 执行SQL
        final Long finalUserId = Long.valueOf(userId);
        final String finalCurrentDate = currentDate;
        final String finalCurrentTime = currentTime;
        final Long finalDataId = dataId;
        final Map<String, Object> finalFieldValues = fieldValues;

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            if (isCreate) {
                String insertSql = "INSERT INTO `" + mainTableName + "` (" + columns + ") VALUES (" + placeholders + ")";
                jdbcTemplate.update(insertSql, values.toArray());
            } else {
                // 构建UPDATE语句
                StringBuilder updateSql = new StringBuilder("UPDATE `" + mainTableName + "` SET ");
                List<Object> updateValues = new ArrayList<>();

                updateSql.append("modedatamodifier=?,modedatamodifydate=?,modedatamodifytime=?,lastModDate=?,lastModTime=?");
                updateValues.add(finalUserId);
                updateValues.add(finalCurrentDate);
                updateValues.add(finalCurrentTime);
                updateValues.add(finalCurrentDate);
                updateValues.add(finalCurrentTime);

                for (WorkflowBillField field : fields) {
                    String dbName = field.getFielddbname();
                    if (dbName == null) continue;
                    updateSql.append(",").append(dbName).append("=?");
                    Object value = finalFieldValues.get(dbName);
                    if (value == null) value = finalFieldValues.get(field.getFieldname());
                    updateValues.add(value != null ? value.toString() : null);
                }

                updateSql.append(" WHERE id=?");
                updateValues.add(finalDataId);

                jdbcTemplate.update(updateSql.toString(), updateValues.toArray());
            }
            return finalDataId;
        });
    }

    @Override
    public FormDataVO getFormDataById(Long modeId, Long dataId) {
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) return null;

        String mainTableName = TableNameUtil.getMainTableName(modeInfo.getBillid().longValue());

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            String sql = "SELECT * FROM `" + mainTableName + "` WHERE id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dataId);
            if (rows.isEmpty()) return null;

            Map<String, Object> row = rows.get(0);
            FormDataVO vo = new FormDataVO();
            vo.setId(dataId);
            vo.setModeid(modeId);

            // 过滤系统字段，只保留自定义字段值
            Map<String, Object> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (!isSystemField(key)) {
                    fieldValues.put(key, entry.getValue());
                }
            }
            vo.setFieldValues(fieldValues);

            vo.setCreater((Integer) row.get("modedatacreater"));
            vo.setCreatedate((String) row.get("modedatacreatedate"));
            vo.setCreatetime((String) row.get("modedatacreatetime"));

            return vo;
        });
    }

    @Override
    public Page<Map<String, Object>> pageFormData(Long modeId, Page<?> page, Map<String, Object> params) {
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) {
            return new Page<>(page.getCurrent(), page.getSize());
        }

        String mainTableName = TableNameUtil.getMainTableName(modeInfo.getBillid().longValue());

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            // 构建查询
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM `" + mainTableName + "` WHERE 1=1");
            StringBuilder dataSql = new StringBuilder("SELECT * FROM `" + mainTableName + "` WHERE 1=1");
            List<Object> queryParams = new ArrayList<>();

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
     * </ul>
     */
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

        // 目标表真实列（小写 → 实际列名），后续只按它取值/写值
        Map<String, String> columns = new LinkedHashMap<>();
        for (Map<String, Object> col : jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
            tableName)) {
            Object name = col.get("COLUMN_NAME");
            if (name != null) {
                columns.put(String.valueOf(name).toLowerCase(), String.valueOf(name));
            }
        }
        if (columns.isEmpty()) {
            throw new RuntimeException("业务表不存在: " + tableName);
        }

        int userId = 0;
        try {
            userId = SecureUtil.getUserId().intValue();
        } catch (Exception ignored) {
            // 无登录态（如内部调用）时不写创建人
        }
        String today = LocalDate.now().toString();
        String now = LocalTime.now().toString().substring(0, 8);
        boolean create = (dto.getDataId() == null);
        Long dataId = create ? IdWorker.getId() : dto.getDataId();

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
                String col = columns.get(k.toLowerCase());
                // 表里没这列 / 是系统列（由审计逻辑接管，不被业务值覆盖）→ 忽略
                if (col == null || isSystemField(col)) {
                    return;
                }
                row.put(col, normalizeValue(v));
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

    /** 统一值类型：基本类型原样（交给 JDBC），复杂对象（明细行/富文本结构等）落 JSON 串 */
    private static Object normalizeValue(Object v) {
        if (v == null || v instanceof CharSequence || v instanceof Number
            || v instanceof Boolean || v instanceof java.util.Date) {
            return v;
        }
        try {
            return JsonUtil.toJson(v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    /**
     * 生成新的数据ID
     */
    private Long generateDataId(String tableName) {
        Long maxId = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(id), 0) + 1 FROM `" + tableName + "`", Long.class
        );
        return maxId;
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
