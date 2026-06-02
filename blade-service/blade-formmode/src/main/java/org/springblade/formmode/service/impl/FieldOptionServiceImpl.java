package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.mapper.FieldOptionMapper;
import org.springblade.formmode.service.IFieldOptionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字段选项配置服务实现类
 */
@Slf4j
@Service
public class FieldOptionServiceImpl extends ServiceImpl<FieldOptionMapper, FieldOption> implements IFieldOptionService {

    @Override
    public List<FieldOption> getByFieldId(Long fieldId) {
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        wrapper.orderByAsc(FieldOption::getListOrder);
        return this.list(wrapper);
    }

    @Override
    public List<FieldOption> getByFormId(Long formId) {
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFormId, formId);
        wrapper.orderByAsc(FieldOption::getListOrder);
        return this.list(wrapper);
    }

    @Override
    public boolean saveOptions(Long fieldId, Long formId, List<FieldOption> options) {
        // 1. 删除该字段的所有选项
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        this.remove(wrapper);

        // 2. 批量插入新选项
        if (options != null && !options.isEmpty()) {
            for (FieldOption option : options) {
                option.setFieldId(fieldId);
                option.setFormId(formId);
            }
            return this.saveBatch(options);
        }
        return true;
    }

    @Override
    public boolean deleteByFieldId(Long fieldId) {
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        return this.remove(wrapper);
    }
}
