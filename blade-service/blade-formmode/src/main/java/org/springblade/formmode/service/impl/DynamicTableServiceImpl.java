package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springblade.formmode.utils.TableNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 动态表管理服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicTableServiceImpl implements IDynamicTableService {

    private final JdbcTemplate jdbcTemplate;

    // 通过 ApplicationContext 手动获取，打破循环依赖
    private IFieldDefinitionService fieldDefinitionService;

    @Autowired
    private ApplicationContext applicationContext;

    // 延迟获取 fieldDefinitionService（打破循环依赖）
    private IFieldDefinitionService getFieldDefinitionService() {
        if (fieldDefinitionService == null) {
            fieldDefinitionService = applicationContext.getBean(IFieldDefinitionService.class);
        }
        return fieldDefinitionService;
    }

    @Override
    public boolean createMainTable(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID'," +
                "`request_id` INT DEFAULT NULL COMMENT '关联流程ID'," +
                "`modedatacreator` INT DEFAULT NULL," +
                "`modedatacreatedate` VARCHAR(10) DEFAULT NULL," +
                "`modedatacreatetime` VARCHAR(8) DEFAULT NULL," +
                "`modedatamodifier` INT DEFAULT NULL," +
                "`modedatamodifydate` VARCHAR(10) DEFAULT NULL," +
                "`modedatamodifytime` VARCHAR(8) DEFAULT NULL," +
                "`mode_uuid` VARCHAR(100) DEFAULT NULL," +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态主数据表 " + tableName + "'";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean createMainTable(Long billId) {
        String tableName = getMainTableName(billId);
        return createMainTable(tableName);
    }

    @Override
    public boolean createDetailTable(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`main_id` BIGINT DEFAULT NULL COMMENT '主表ID'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_main_id` (`main_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态明细表 " + tableName + "'";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean createDetailTable(Integer billId, int detailIndex) {
        String tableName = getDetailTableName(billId.longValue(), detailIndex);
        return createDetailTable(tableName);
    }

    @Override
    public boolean addColumnToTable(String tableName, String columnName, String columnType, Integer length) {
        String type = columnType != null ? columnType : "VARCHAR(255)";
        String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + type + " DEFAULT NULL";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean dropColumnFromTable(String tableName, String columnName) {
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`");
        return true;
    }

    @Override
    public boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean dropTable(String tableName) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
        return true;
    }

    @Override
    public String getMainTableName(Long billId) {
        return TableNameUtil.getMainTableName(billId);
    }

    @Override
    public String getDetailTableName(Long billId, int detailIndex) {
        String mainTableName = getMainTableName(billId);
        return TableNameUtil.getDetailTableName(mainTableName, detailIndex);
    }

    // ==================== 表结构同步实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncTableStructure(String billId, String tableName) {
        log.info("========== 同步表结构（从数据库获取字段定义）==========");
        // 从数据库获取字段定义，然后调用带字段列表的方法
        List<FieldDefinition> fields = getFieldDefinitionService().getByFormId(billId);
        log.info("从数据库获取到 {} 个字段定义", fields.size());
        return syncTableStructure(billId, tableName, fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncTableStructure(String billId, String tableName, List<FieldDefinition> fields) {
        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> executedSql = new ArrayList<>();

        try {
            log.info("========== 开始同步表结构（使用前端传入的字段定义）==========");
            log.info("billId: {}, tableName: {}", billId, tableName);

            // 打印字段信息
            for (FieldDefinition field : fields) {
                log.info("字段: id={}, name={}, dbName={}, dbType={}, isMain={}, detailTable={}",
                    field.getId(), field.getFieldName(), field.getFieldDbName(), field.getFieldDbType(),
                    field.getIsMain(), field.getDetailTable());
            }

            // 2. 检查表是否存在数据
            Long rowCount = 0L;
            try {
                rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Long.class);
                log.info("表 {} 中存在 {} 条数据", tableName, rowCount);
            } catch (Exception e) {
                // 表不存在，数据行数为0
                log.info("表 {} 不存在，数据行数为0", tableName);
                rowCount = 0L;
            }

            // 3. 如果存在数据，提示用户清理数据，不执行删表和建表操作，也不删除字段定义
            if (rowCount != null && rowCount > 0) {
                result.put("success", false);
                result.put("msg", "表 " + tableName + " 中已有 " + rowCount + " 条数据，请先清理数据后再执行同步操作");
                result.put("dataCount", rowCount);
                log.warn("表 {} 存在数据，同步终止", tableName);
                return result;
            }

            // 4. 如果不存在数据，直接执行删表和创建表操作，无需判断表是否存在
            // 同时删除字段定义
            log.info("========== 开始删除旧字段定义 ==========");
            // 在删除前再次确认当前字段数量
            int beforeDeleteCount = (int) getFieldDefinitionService().count(new LambdaQueryWrapper<FieldDefinition>().eq(FieldDefinition::getBillId, billId));
            log.info("删除前字段数量: {}", beforeDeleteCount);
            deleteFieldDefinitionsByBillId(billId);
            // 在删除后确认字段数量
            int afterDeleteCount = (int) getFieldDefinitionService().count(new LambdaQueryWrapper<FieldDefinition>().eq(FieldDefinition::getBillId, billId));
            log.info("删除后字段数量: {}", afterDeleteCount);
            log.info("========== 旧字段定义删除完成 ==========");
            log.info("表 {} 无数据，开始执行删表和建表操作", tableName);

            // 区分主表字段和明细表字段
            List<FieldDefinition> mainFields = new ArrayList<>();
            List<FieldDefinition> detailFields = new ArrayList<>();
            Set<Integer> detailTableIndices = new HashSet<>();

            for (FieldDefinition field : fields) {
                Integer isMain = field.getIsMain();
                if (isMain == null || isMain == 1) {
                    // 主表字段
                    mainFields.add(field);
                } else if (isMain == 0) {
                    // 明细表字段
                    detailFields.add(field);
                    Integer detailTable = field.getDetailTable();
                    // 如果 detailTable 为 null，设置默认值为 1
                    if (detailTable == null) {
                        detailTable = 1;
                        field.setDetailTable(detailTable); // 更新字段对象中的值
                        log.warn("明细表字段 {} 的 detailTable 为 null，已设置默认值为 1", field.getFieldName());
                    }
                    log.info("明细表字段: fieldName={}, isMain={}, detailTable={}", field.getFieldName(), isMain, detailTable);
                    detailTableIndices.add(detailTable);
                }
            }

            log.info("主表字段: {}, 明细表字段: {}, 明细表索引集合: {}", mainFields.size(), detailFields.size(), detailTableIndices);

            // ========== 处理主表 ==========
            // 2. 删除主表（如果存在）
            log.info("========== 删除主表 {} ==========", tableName);
            dropTable(tableName);
            log.info("已删除主表（如果存在）: {}", tableName);

            // 3. 创建主表（只包含主表字段）
            log.info("========== 创建主表 {} ==========", tableName);
            createTableWithAllColumns(tableName, mainFields, executedSql, true);
            log.info("已创建主表（包含主表字段）: {}", tableName);

            // ========== 处理明细表（与主表一并处理）==========
            for (Integer detailIndex : detailTableIndices) {
                // 使用 TableNameUtil 根据主表名称生成明细表名称
                // 格式为：{主表名}_dt{索引}，例如：formtable_main_1_dt1, formtable_main_1_dt2
                String detailTableName = TableNameUtil.getDetailTableName(tableName, detailIndex);

                // 4. 删除明细表（如果存在）
                log.info("========== 删除明细表 {} ==========", detailTableName);
                dropTable(detailTableName);
                log.info("已删除明细表 {}（如果存在）: {}", detailIndex, detailTableName);

                // 5. 创建明细表（包含明细表字段）
                log.info("========== 创建明细表 {} ==========", detailTableName);
                List<FieldDefinition> currentDetailFields = detailFields.stream()
                        .filter(f -> detailIndex.equals(f.getDetailTable()))
                        .collect(Collectors.toList());
                createTableWithAllColumns(detailTableName, currentDetailFields, executedSql, false);
                log.info("已创建明细表 {}（包含明细表字段）: {}", detailIndex, detailTableName);
            }

            // 6. 创建新字段定义（重新保存传入的字段列表）
            // 使用Set去重，避免重复创建字段
            Set<String> createdFieldNames = new HashSet<>();
            int createdFieldCount = 0;
            int skippedDuplicateCount = 0;

            for (FieldDefinition field : fields) {
                String fieldKey = field.getFieldName() + "_" + (field.getIsMain() != null ? field.getIsMain() : 1) + "_" + (field.getDetailTable() != null ? field.getDetailTable() : 0);

                // 跳过重复字段
                if (createdFieldNames.contains(fieldKey)) {
                    log.warn("跳过重复字段定义: fieldName={}, isMain={}, detailTable={}",
                        field.getFieldName(), field.getIsMain(), field.getDetailTable());
                    skippedDuplicateCount++;
                    continue;
                }
                createdFieldNames.add(fieldKey);

                try {
                    // 重置ID为null，以便创建新记录
                    field.setId(null);
                    field.setIsDeleted(0);
                    field.setStatus(1);
                    getFieldDefinitionService().save(field);
                    createdFieldCount++;
                    log.info("创建字段定义成功: fieldName={}, isMain={}, detailTable={}",
                        field.getFieldName(), field.getIsMain(), field.getDetailTable());
                } catch (Exception e) {
                    log.warn("创建字段定义失败: fieldName={}, error={}", field.getFieldName(), e.getMessage());
                }
            }
            log.info("已重新创建 {} 个字段定义，跳过 {} 个重复字段", createdFieldCount, skippedDuplicateCount);

            result.put("success", true);
            result.put("msg", "数据库表同步成功");
            result.put("warnings", warnings);
            result.put("executedSql", executedSql);
            result.put("createdFieldCount", createdFieldCount);
            log.info("========== 表结构同步成功 ==========");

        } catch (Exception e) {
            log.error("同步表结构失败: " + tableName, e);
            result.put("success", false);
            result.put("msg", "数据库表同步失败: " + e.getMessage());
            log.info("========== 表结构同步失败 ==========");
            throw new RuntimeException("同步表结构失败", e); // 触发事务回滚
        }

        return result;
    }

    /**
     * 创建包含所有字段的表（系统字段 + 用户定义的字段）
     * @param tableName 表名
     * @param fields 字段列表
     * @param executedSql 执行的SQL记录列表
     * @param isMainTable 是否为主表
     */
    private void createTableWithAllColumns(String tableName, List<FieldDefinition> fields, List<String> executedSql, boolean isMainTable) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (");

        if (isMainTable) {
            // 主表系统字段
            sql.append("`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID',");
            sql.append("`request_id` INT DEFAULT NULL COMMENT '关联流程ID',");
            sql.append("`modedatacreator` INT DEFAULT NULL,");
            sql.append("`modedatacreatedate` VARCHAR(10) DEFAULT NULL,");
            sql.append("`modedatacreatetime` VARCHAR(8) DEFAULT NULL,");
            sql.append("`modedatamodifier` INT DEFAULT NULL,");
            sql.append("`modedatamodifydate` VARCHAR(10) DEFAULT NULL,");
            sql.append("`modedatamodifytime` VARCHAR(8) DEFAULT NULL,");
            sql.append("`mode_uuid` VARCHAR(100) DEFAULT NULL,");
        } else {
            // 明细表系统字段
            sql.append("`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID',");
            sql.append("`main_id` BIGINT DEFAULT NULL COMMENT '主表ID',");
        }

        // 使用Set去重，避免重复字段
        Set<String> addedColumns = new HashSet<>();

        // 添加用户定义的字段
        for (FieldDefinition field : fields) {
            String columnName = field.getFieldDbName();
            if (columnName == null || columnName.isEmpty()) {
                columnName = field.getFieldName();
            }

            // 如果字段名是中文，自动生成英文列名
            if (isChinese(columnName)) {
                String generatedName = generateEnglishColumnName(columnName, addedColumns.size() + 1);
                log.info("字段名 {} 是中文，自动生成英文列名: {}", columnName, generatedName);
                columnName = generatedName;
            }

            // 只跳过已经在表中定义的系统字段（如 id），其他系统字段允许用户自定义
            // 这样用户可以添加 create_time, update_time, create_user, update_user, create_dept, tenant_id 等字段
            if ("id".equals(columnName.toLowerCase())) {
                log.info("跳过已存在的主键字段: {}.{}", tableName, columnName);
                continue;
            }

            // 跳过重复字段
            if (addedColumns.contains(columnName)) {
                log.warn("跳过重复字段: {}.{}", tableName, columnName);
                continue;
            }
            addedColumns.add(columnName);

            String sqlType = getSqlType(field);
            sql.append("`").append(columnName).append("` ").append(sqlType).append(" DEFAULT NULL COMMENT '").append(field.getFieldName()).append("',");
            log.info("添加字段到表: {}.{} ({})", tableName, columnName, sqlType);
        }

        // 添加主键
        sql.append("PRIMARY KEY (`id`)");

        // 明细表添加主表ID索引
        if (!isMainTable) {
            sql.append(", KEY `idx_main_id` (`main_id`)");
        }

        String comment = isMainTable ? "动态主数据表 " : "动态明细表 ";
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='").append(comment).append(tableName).append("'");

        log.info("执行 CREATE TABLE SQL: {}", sql.toString());
        jdbcTemplate.execute(sql.toString());
        executedSql.add("CREATE TABLE " + tableName + " (包含所有字段)");
    }

    @Override
    public List<Map<String, Object>> getTableColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
                "NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, tableName);
    }

    @Override
    public Map<String, Object> checkDataCompatibility(String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // 检查表是否有数据
            Long rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Long.class);
            if (rowCount != null && rowCount > 0) {
                warnings.add("表 " + tableName + " 中已有 " + rowCount + " 条数据，删除重建将清空所有数据");
            }
            result.put("compatible", true);
        } catch (Exception e) {
            // 表可能不存在
            result.put("compatible", true);
        }

        result.put("warnings", warnings);
        result.put("errors", errors);
        return result;
    }

    /**
     * 恢复备份数据到新表
     */
    private int restoreData(String tableName, List<Map<String, Object>> backupData, List<FieldDefinition> fields) {
        if (backupData.isEmpty()) {
            return 0;
        }

        // 获取新表的列名（排除自增 ID）
        List<Map<String, Object>> newColumns = getTableColumns(tableName);
        Set<String> newColumnNames = newColumns.stream()
                .map(col -> ((String) col.get("COLUMN_NAME")).toLowerCase())
                .collect(Collectors.toSet());

        // 构建字段定义中的列名集合
        Set<String> fieldColumnNames = new HashSet<>();
        for (FieldDefinition field : fields) {
            String colName = field.getFieldDbName();
            if (colName == null || colName.isEmpty()) {
                colName = field.getFieldName();
            }
            fieldColumnNames.add(colName.toLowerCase());
        }

        int restoredCount = 0;

        for (Map<String, Object> row : backupData) {
            try {
                // 构建 INSERT 语句（只插入新表中存在的列）
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();
                List<Object> params = new ArrayList<>();

                for (String colName : newColumnNames) {
                    // 跳过自增 ID
                    if (colName.equals("id")) {
                        continue;
                    }

                    // 只恢复字段定义中存在的列
                    if (!fieldColumnNames.contains(colName) && !isSystemColumn(colName)) {
                        continue;
                    }

                    if (row.containsKey(colName)) {
                        if (columns.length() > 0) {
                            columns.append(", ");
                            values.append(", ");
                        }
                        columns.append("`").append(colName).append("`");
                        values.append("?");
                        params.add(row.get(colName));
                    }
                }

                if (columns.length() > 0) {
                    String sql = "INSERT INTO `" + tableName + "` (" + columns + ") VALUES (" + values + ")";
                    jdbcTemplate.update(sql, params.toArray());
                    restoredCount++;
                }
            } catch (Exception e) {
                log.warn("恢复数据行失败: {}", row, e);
            }
        }

        return restoredCount;
    }

    /**
     * 检查是否为系统保留列
     */
    private boolean isSystemColumn(String columnName) {
        Set<String> systemColumns = new HashSet<>(Arrays.asList(
                "id", "request_id", "modedatacreator", "modedatamodifier",
                "modedatacreatedate", "modedatacreatetime", "modedatamodifydate",
                "modedatamodifytime", "mode_uuid", "main_id", "is_deleted",
                "create_time", "update_time", "create_user", "update_user",
                "create_dept", "tenant_id"
        ));
        return systemColumns.contains(columnName.toLowerCase());
    }

    /**
     * 检查字符串是否包含中文
     */
    private boolean isChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据中文名称生成英文列名
     */
    private String generateEnglishColumnName(String chineseName, int index) {
        // 移除特殊字符，只保留中文和字母数字
        StringBuilder sb = new StringBuilder();
        for (char c : chineseName.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }

        // 如果处理后为空，使用默认名称
        String result = sb.toString();
        if (result.isEmpty()) {
            result = "field_" + index;
        } else {
            // 如果开头是数字，添加前缀
            if (Character.isDigit(result.charAt(0))) {
                result = "field_" + result;
            }
            // 确保长度不超过64个字符（MySQL列名最大长度）
            if (result.length() > 60) {
                result = result.substring(0, 60);
            }
        }

        return result.toLowerCase();
    }

    /**
     * 删除指定表单的所有字段定义
     * 用于同步表结构时清空旧的字段定义
     */
    private void deleteFieldDefinitionsByBillId(String billId) {
        log.info("[deleteFieldDefinitionsByBillId] 开始删除表单 {} 的所有字段定义", billId);
        try {
            // 使用新的高效批量删除方法，直接根据表单ID删除，不需要先查询
            int deletedCount = getFieldDefinitionService().deleteByFormIdLogical(billId);

            if (deletedCount > 0) {
                log.info("[deleteFieldDefinitionsByBillId] 已成功删除表单 {} 的 {} 个字段定义", billId, deletedCount);
            } else {
                log.info("[deleteFieldDefinitionsByBillId] 表单 {} 没有字段定义需要删除", billId);
            }
        } catch (Exception e) {
            log.error("[deleteFieldDefinitionsByBillId] 删除表单 {} 的字段定义失败", billId, e);
        }
    }

    /**
     * 将字段定义转换为 SQL 类型
     */
    private String getSqlType(FieldDefinition field) {
        String fieldDbType = field.getFieldDbType();
        Integer fieldLen = field.getFieldLen();
        Integer decimalDigit = field.getDecimalDigit();

        if (fieldDbType == null) {
            return "VARCHAR(255)";
        }

        // 根据 fieldDbType 返回对应的 SQL 类型
        switch (fieldDbType.toLowerCase()) {
            case "int":
            case "integer":
                return "INT";
            case "bigint":
                return "BIGINT";
            case "varchar":
                if (fieldLen != null && fieldLen > 0) {
                    return "VARCHAR(" + fieldLen + ")";
                }
                return "VARCHAR(255)";
            case "text":
                return "TEXT";
            case "decimal":
                if (fieldLen != null && decimalDigit != null) {
                    return "DECIMAL(" + fieldLen + "," + decimalDigit + ")";
                }
                return "DECIMAL(10,2)";
            case "date":
                return "DATE";
            case "datetime":
                return "DATETIME";
            case "timestamp":
                return "TIMESTAMP";
            default:
                return "VARCHAR(255)";
        }
    }

}
