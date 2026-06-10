package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.entity.FieldOption;

import java.util.List;
import java.util.Map;

/**
 * 字段定义服务接口
 */
public interface IFieldDefinitionService extends IService<FieldDefinition> {

    /**
     * 根据表单ID获取字段列表（过滤逻辑删除的字段）
     */
    List<FieldDefinition> getByFormId(Long billId);

    /**
     * 根据表单ID获取字段列表（包含已删除的字段，用于管理后台）
     */
    List<FieldDefinition> getByFormIdIncludeDeleted(Long billId);

    /**
     * 根据表单ID和 isMain 获取字段列表
     * @param billId 表单ID
     * @param isMain 是否主表字段（1-主表，0-明细表）
     * @return 字段列表
     */
    List<FieldDefinition> getByFormIdAndIsMain(Long billId, Integer isMain);

    /**
     * 创建字段并添加列到数据库表
     */
    boolean createFieldWithColumn(FieldDefinition field);

    /**
     * 创建字段并添加列到数据库表（支持扩展属性和选项数据）
     * @param field 字段定义
     * @param fieldExtend 扩展属性（可为 null）
     * @param options 选项列表（可为 null，用于选择框/单选/复选）
     */
    boolean createFieldWithColumn(FieldDefinition field, FieldExtend fieldExtend, List<FieldOption> options);

    /**
     * 更新字段并更新数据库列
     */
    boolean updateFieldWithColumn(FieldDefinition field);

    /**
     * 更新字段并更新数据库列（支持扩展属性和选项数据）
     */
    boolean updateFieldWithColumn(FieldDefinition field, FieldExtend fieldExtend, List<FieldOption> options);

    /**
     * 逻辑删除字段（设置 is_deleted=1, status=-1）
     * 同时逻辑删除关联的扩展属性和选项数据
     */
    boolean deleteFieldWithColumn(Long fieldId);

    /**
     * 物理删除字段（慎用！用于数据清理）
     * 只有管理员在清理已逻辑删除的数据时才调用此方法
     */
    boolean physicalDeleteField(Long fieldId);

    /**
     * 恢复已逻辑删除的字段（设置 is_deleted=0, status=1）
     */
    boolean restoreField(Long fieldId);

    /**
     * 获取字段详情（包含扩展属性和选项数据）
     * 过滤逻辑删除的字段
     * @param fieldId 字段ID
     * @return 包含扩展属性和选项数据的 Map
     */
    Map<String, Object> getFieldDetail(Long fieldId);

    /**
     * 批量逻辑删除字段
     * @param fieldIds 字段ID列表
     * @return 是否全部删除成功
     */
    boolean batchDeleteFieldWithColumn(List<Long> fieldIds);

    /**
     * 根据表单ID批量逻辑删除字段定义
     * @param billId 表单ID
     * @return 删除的字段数量
     */
    int deleteByFormIdLogical(Long billId);
}
