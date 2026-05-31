package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.mapper.FieldDefinitionMapper;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springblade.formmode.service.IWorkflowBillService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字段定义服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldDefinitionServiceImpl extends ServiceImpl<FieldDefinitionMapper, FieldDefinition> implements IFieldDefinitionService {

    private final JdbcTemplate jdbcTemplate;
    private final IDynamicTableService dynamicTableService;
    private final IWorkflowBillService workflowBillService;

    @Override
    public List<FieldDefinition> getByFormId(Long billId) {
        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldDefinition::getBillId, billId);
        wrapper.orderByAsc(FieldDefinition::getDsOrder);
        return this.list(wrapper);
    }

    @Override
    public boolean createFieldWithColumn(FieldDefinition field) {
        // 1. 保存字段定义
        boolean saved = this.save(field);

        if (saved) {
            // 2. 添加列到数据库表
            String tableName = getTableName(field.getBillId());
            if (tableName != null) {
                // 先检查表是否存在
                if (dynamicTableService.tableExists(tableName)) {
                    String columnName = field.getFieldDbName();
                    String columnType = getSqlType(field);
                    Integer length = field.getFieldLen();

                    // 检查列是否已存在
                    if (!columnExists(tableName, columnName)) {
                        dynamicTableService.addColumnToTable(tableName, columnName, columnType, length);
                    }
                } else {
                    // 表尚未创建，由 "创建数据库表" 流程统一处理
                    log.warn("表 {} 不存在，跳过添加列，由 createTable 统一处理", tableName);
                }
            }
        }

        return saved;
    }

    @Override
    public boolean updateFieldWithColumn(FieldDefinition field) {
        // 1. 更新字段定义
        boolean updated = this.updateById(field);

        if (updated) {
            // 2. 更新数据库列（如果需要）
            // 注意：修改列名或类型需要 ALTER TABLE MODIFY COLUMN
            // 这里简化实现，仅处理新增列的情况
            String tableName = getTableName(field.getBillId());
            if (tableName != null) {
                String columnName = field.getFieldDbName();
                if (!columnExists(tableName, columnName)) {
                    String columnType = getSqlType(field);
                    Integer length = field.getFieldLen();
                    dynamicTableService.addColumnToTable(tableName, columnName, columnType, length);
                }
            }
        }

        return updated;
    }

    @Override
    public boolean deleteFieldWithColumn(Long fieldId) {
        FieldDefinition field = this.getById(fieldId);
        if (field == null) {
            return false;
        }

        // 1. 删除数据库列
        String tableName = getTableName(field.getBillId());
        if (tableName != null) {
            String columnName = field.getFieldDbName();
            if (columnExists(tableName, columnName)) {
                dynamicTableService.dropColumnFromTable(tableName, columnName);
            }
        }

        // 2. 删除字段定义
        return this.removeById(fieldId);
    }

    /**
     * 根据表单ID获取配置的表名
     * 从 workflow_bill 表查询真实的 table_name，避免使用旧的 formtable_main_{billId} 规则
     */
    private String getTableName(Long billId) {
        try {
            WorkflowBill workflowBill = workflowBillService.getById(billId);
            if (workflowBill != null && workflowBill.getTableName() != null) {
                return workflowBill.getTableName();
            }
        } catch (Exception e) {
            log.warn("查询表单定义失败: billId={}", billId, e);
        }
        return null;
    }

    /**
     * 获取字段的 SQL 类型
     */
    private String getSqlType(FieldDefinition field) {
        String fieldDbType = field.getFieldDbType();
        Integer fieldHtmlType = field.getFieldHtmlType();

        if (fieldDbType == null) {
            return "VARCHAR(255)";
        }

        // 根据 fieldDbType 映射到 SQL 类型
        switch (fieldDbType.toLowerCase()) {
            case "varchar":
                Integer length = field.getFieldLen();
                if (length != null && length > 0) {
                    return "VARCHAR(" + length + ")";
                }
                return "VARCHAR(255)";
            case "int":
                return "INT";
            case "bigint":
                return "BIGINT";
            case "decimal":
                return "DECIMAL(10,2)";
            case "date":
                return "DATE";
            case "datetime":
                return "DATETIME";
            case "text":
                return "TEXT";
            case "float":
                return "FLOAT";
            case "double":
                return "DOUBLE";
            default:
                return "VARCHAR(255)";
        }
    }

    /**
     * 检查列是否存在
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT `" + columnName + "` FROM `" + tableName + "` LIMIT 0";
            jdbcTemplate.execute(sql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
