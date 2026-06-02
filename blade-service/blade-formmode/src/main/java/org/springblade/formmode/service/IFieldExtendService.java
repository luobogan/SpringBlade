package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FieldExtend;

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
    java.util.List<FieldExtend> getByFormId(Long formId);

    /**
     * 保存或更新字段扩展属性
     */
    boolean saveOrUpdateByFieldId(FieldExtend fieldExtend);

    /**
     * 删除字段扩展属性（根据字段ID）
     */
    boolean deleteByFieldId(Long fieldId);
}
