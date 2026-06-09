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
     * 根据字段ID获取选项列表（过滤逻辑删除）
     */
    List<FieldOption> getByFieldId(Long fieldId);

    /**
     * 根据表单ID获取选项列表（过滤逻辑删除）
     */
    List<FieldOption> getByFormId(Long formId);

    /**
     * 保存字段选项列表（先删除再插入，逻辑删除模式下使用更新）
     */
    boolean saveOptions(Long fieldId, Long formId, List<FieldOption> options);

    /**
     * 物理删除字段选项（根据字段ID，向后兼容）
     */
    boolean deleteByFieldId(Long fieldId);

    /**
     * 逻辑删除字段选项（根据字段ID）
     * 设置 is_deleted=1, status=-1
     */
    boolean deleteByFieldIdLogical(Long fieldId);

    /**
     * 逻辑删除字段选项（根据表单ID）
     */
    boolean deleteByFormIdLogical(Long formId);

    /**
     * 恢复已逻辑删除的选项
     */
    boolean restoreByFieldId(Long fieldId);
}
