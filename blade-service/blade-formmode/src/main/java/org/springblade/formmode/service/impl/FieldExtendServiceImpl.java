package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.mapper.FieldExtendMapper;
import org.springblade.formmode.service.IFieldExtendService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字段扩展属性服务实现类
 */
@Slf4j
@Service
public class FieldExtendServiceImpl extends ServiceImpl<FieldExtendMapper, FieldExtend> implements IFieldExtendService {

    @Override
    public FieldExtend getByFieldId(Long fieldId) {
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFieldId, fieldId);
        return this.getOne(wrapper);
    }

    @Override
    public List<FieldExtend> getByFormId(Long formId) {
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFormId, formId);
        return this.list(wrapper);
    }

    @Override
    public boolean saveOrUpdateByFieldId(FieldExtend fieldExtend) {
        FieldExtend existing = getByFieldId(fieldExtend.getFieldId());
        if (existing != null) {
            fieldExtend.setId(existing.getId());
            return this.updateById(fieldExtend);
        } else {
            return this.save(fieldExtend);
        }
    }

    @Override
    public boolean deleteByFieldId(Long fieldId) {
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFieldId, fieldId);
        return this.remove(wrapper);
    }
}
