package org.springblade.formmode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.formmode.entity.FieldOption;

import java.util.List;

/**
 * 字段选项配置服务接口
 * 对标泛微E9 selectItem 表
 */
public interface IFieldOptionService extends IService<FieldOption> {

    /**
     * 根据字段ID获取选项列表
     */
    List<FieldOption> getByFieldId(Long fieldId);

    /**
     * 根据表单ID获取选项列表
     */
    List<FieldOption> getByFormId(Long formId);

    /**
     * 保存字段选项列表（先删除再插入）
     */
    boolean saveOptions(Long fieldId, Long formId, List<FieldOption> options);

    /**
     * 删除字段选项（根据字段ID）
     */
    boolean deleteByFieldId(Long fieldId);
}
