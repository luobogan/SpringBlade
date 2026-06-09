package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.mapper.FieldDefinitionMapper;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springblade.formmode.service.IFieldExtendService;
import org.springblade.formmode.service.IFieldOptionService;
import org.springblade.formmode.service.IWorkflowBillService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final IFieldExtendService fieldExtendService;
    private final IFieldOptionService fieldOptionService;

    // ==================== 查询方法 ====================

    @Override
    public List<FieldDefinition> getByFormId(Long billId) {
        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldDefinition::getBillId, billId);
        // 过滤逻辑删除的字段：is_deleted != 1 且 status != -1
        wrapper.ne(FieldDefinition::getIsDeleted, 1);
        wrapper.ne(FieldDefinition::getStatus, -1);
        wrapper.orderByAsc(FieldDefinition::getDsOrder);
        return this.list(wrapper);
    }

    @Override
    public List<FieldDefinition> getByFormIdIncludeDeleted(Long billId) {
        // 包含所有字段（用于管理后台）
        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldDefinition::getBillId, billId);
        wrapper.orderByAsc(FieldDefinition::getDsOrder);
        return this.list(wrapper);
    }

    /**
     * 获取字段详情（包含扩展属性和选项数据）
     */
    public java.util.Map<String, Object> getFieldDetail(Long fieldId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // 1. 获取字段定义（过滤逻辑删除）
        FieldDefinition field = this.getById(fieldId);
        if (field == null) {
            return null;
        }
        // 如果已逻辑删除，返回 null
        if (Integer.valueOf(1).equals(field.getIsDeleted()) || Integer.valueOf(-1).equals(field.getStatus())) {
            return null;
        }
        result.put("field", field);

        // 2. 获取扩展属性
        FieldExtend fieldExtend = fieldExtendService.getByFieldId(fieldId);
        result.put("extend", fieldExtend);

        // 3. 获取选项数据
        List<FieldOption> options = fieldOptionService.getByFieldId(fieldId);
        result.put("options", options);

        return result;
    }

    // ==================== 新增方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createFieldWithColumn(FieldDefinition field) {
        return createFieldWithColumn(field, null, null);
    }

    /**
     * 创建字段并添加列到数据库表（支持扩展属性和选项数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createFieldWithColumn(FieldDefinition field, FieldExtend fieldExtend, java.util.List<FieldOption> options) {
        // 1. 保存字段定义（初始化逻辑删除字段）
        if (field.getIsDeleted() == null) {
            field.setIsDeleted(0);
        }
        if (field.getStatus() == null) {
            field.setStatus(1);
        }
        boolean saved = this.save(field);

        if (saved) {
            // 2. 添加列到数据库表
            String tableName = getTableName(field.getBillId());
            if (tableName != null) {
                if (dynamicTableService.tableExists(tableName)) {
                    String columnName = field.getFieldDbName();
                    String columnType = getSqlType(field);
                    Integer length = field.getFieldLen();

                    if (!columnExists(tableName, columnName)) {
                        dynamicTableService.addColumnToTable(tableName, columnName, columnType, length);
                    }
                } else {
                    log.warn("表 {} 不存在，跳过添加列，由 createTable 统一处理", tableName);
                }
            }

            // 3. 保存扩展属性
            if (fieldExtend != null) {
                fieldExtend.setFieldId(field.getId());
                fieldExtend.setFormId(field.getBillId());
                fieldExtendService.save(fieldExtend);
            }

            // 4. 保存选项数据
            if (options != null && !options.isEmpty()) {
                for (FieldOption option : options) {
                    option.setFieldId(field.getId());
                    option.setFormId(field.getBillId());
                }
                fieldOptionService.saveBatch(options);
            }
        }

        return saved;
    }

    // ==================== 更新方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFieldWithColumn(FieldDefinition field) {
        return updateFieldWithColumn(field, null, null);
    }

    /**
     * 更新字段并更新数据库列（支持扩展属性和选项数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFieldWithColumn(FieldDefinition field, FieldExtend fieldExtend, java.util.List<FieldOption> options) {
        // 1. 更新字段定义
        boolean updated = this.updateById(field);

        if (updated) {
            // 2. 更新数据库列（如果需要）
            String tableName = getTableName(field.getBillId());
            if (tableName != null) {
                String columnName = field.getFieldDbName();
                if (!columnExists(tableName, columnName)) {
                    String columnType = getSqlType(field);
                    Integer length = field.getFieldLen();
                    dynamicTableService.addColumnToTable(tableName, columnName, columnType, length);
                }
            }

            // 3. 更新扩展属性
            if (fieldExtend != null) {
                fieldExtend.setFieldId(field.getId());
                fieldExtend.setFormId(field.getBillId());
                fieldExtendService.saveOrUpdateByFieldId(fieldExtend);
            }

            // 4. 更新选项数据
            if (options != null) {
                fieldOptionService.saveOptions(field.getId(), field.getBillId(), options);
            }
        }

        return updated;
    }

    // ==================== 删除方法（逻辑删除）====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFieldWithColumn(Long fieldId) {
        FieldDefinition field = this.getById(fieldId);
        if (field == null) {
            log.warn("字段不存在：fieldId={}", fieldId);
            return false;
        }

        // 检查是否已逻辑删除
        if (Integer.valueOf(1).equals(field.getIsDeleted())) {
            log.warn("字段已删除：fieldId={}", fieldId);
            return true;
        }

        // 1. 逻辑删除扩展属性（不物理删除）
        fieldExtendService.deleteByFieldIdLogical(fieldId);

        // 2. 逻辑删除选项数据（不物理删除）
        fieldOptionService.deleteByFieldIdLogical(fieldId);

        // 3. 【可选】是否删除数据库列（根据业务需求决定）
        // 注意：逻辑删除模式下，通常保留数据库列，只标记字段为删除状态
        // 如果确实需要删除列，取消下面注释：
        /*
        String tableName = getTableName(field.getBillId());
        if (tableName != null) {
            String columnName = field.getFieldDbName();
            if (columnExists(tableName, columnName)) {
                dynamicTableService.dropColumnFromTable(tableName, columnName);
            }
        }
        */

        // 4. 逻辑删除字段定义（设置 isDeleted=1, status=-1）
        FieldDefinition updateField = new FieldDefinition();
        updateField.setId(fieldId);
        updateField.setIsDeleted(1);
        updateField.setStatus(-1);
        boolean deleted = this.updateById(updateField);

        if (deleted) {
            log.info("字段逻辑删除成功：fieldId={}, fieldName={}", fieldId, field.getFieldName());
        }

        return deleted;
    }

    /**
     * 物理删除字段（慎用！用于数据清理）
     * 只有管理员在清理已逻辑删除的数据时才调用此方法
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean physicalDeleteField(Long fieldId) {
        FieldDefinition field = this.getById(fieldId);
        if (field == null) {
            return false;
        }

        // 1. 物理删除扩展属性
        fieldExtendService.deleteByFieldId(fieldId);

        // 2. 物理删除选项数据
        fieldOptionService.deleteByFieldId(fieldId);

        // 3. 物理删除数据库列
        String tableName = getTableName(field.getBillId());
        if (tableName != null) {
            String columnName = field.getFieldDbName();
            if (columnExists(tableName, columnName)) {
                dynamicTableService.dropColumnFromTable(tableName, columnName);
            }
        }

        // 4. 物理删除字段定义记录
        return this.removeById(fieldId);
    }

    // ==================== 恢复删除的字段 ====================

    /**
     * 恢复已逻辑删除的字段
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreField(Long fieldId) {
        FieldDefinition field = new FieldDefinition();
        field.setId(fieldId);
        field.setIsDeleted(0);
        field.setStatus(1);
        boolean restored = this.updateById(field);

        if (restored) {
            log.info("字段恢复成功：fieldId={}", fieldId);
        }

        return restored;
    }

    // ==================== 批量删除方法 ====================

    /**
     * 批量逻辑删除字段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteFieldWithColumn(List<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return true;
        }
        
        for (Long fieldId : fieldIds) {
            try {
                deleteFieldWithColumn(fieldId);
            } catch (Exception e) {
                log.error("批量删除字段异常：fieldId=" + fieldId, e);
                throw e; // 重新抛出异常以触发回滚
            }
        }
        
        return true;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据表单ID获取配置的表名
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
