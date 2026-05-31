package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FieldDefinition;
import java.util.List;

/**
 * 字段定义服务接口
 */
public interface IFieldDefinitionService extends IService<FieldDefinition> {

    /**
     * 根据表单ID获取字段列表
     */
    List<FieldDefinition> getByFormId(Long billId);

    /**
     * 创建字段并添加列到数据库表
     */
    boolean createFieldWithColumn(FieldDefinition field);

    /**
     * 更新字段并更新数据库列
     */
    boolean updateFieldWithColumn(FieldDefinition field);

    /**
     * 删除字段并删除数据库列
     */
    boolean deleteFieldWithColumn(Long fieldId);
}
