package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FieldExtend;

import java.util.List;

/**
 * 字段扩展属性服务接口
 * 对标泛微E9 ModeFormFieldExtend 表
 */
public interface IFieldExtendService extends IService<FieldExtend> {

    /**
     * 根据字段ID获取扩展属性
     */
    FieldExtend getByFieldId(Long fieldId);

    /**
     * 根据表单ID获取扩展属性列表
     */
    List<FieldExtend> getByFormId(String formId);

    /**
     * 保存或更新字段扩展属性
     */
    boolean saveOrUpdateByFieldId(FieldExtend fieldExtend);

    /**
     * 物理删除字段扩展属性（根据字段ID）
     * 仅在物理清理时使用
     */
    boolean deleteByFieldId(Long fieldId);

    /**
     * 逻辑删除字段扩展属性（根据字段ID）
     * 设置 is_deleted=1, status=-1
     */
    boolean deleteByFieldIdLogical(Long fieldId);

    /**
     * 根据表单ID逻辑删除扩展属性
     */
    boolean deleteByFormIdLogical(String formId);
}
