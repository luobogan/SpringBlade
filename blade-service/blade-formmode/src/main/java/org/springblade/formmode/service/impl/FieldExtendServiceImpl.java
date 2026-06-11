package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.mapper.FieldExtendMapper;
import org.springblade.formmode.service.IFieldExtendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 过滤逻辑删除的记录
        wrapper.ne(FieldExtend::getIsDeleted, 1);
        wrapper.ne(FieldExtend::getStatus, -1);
        return this.getOne(wrapper);
    }

    @Override
    public List<FieldExtend> getByFormId(String formId) {
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFormId, formId);
        // 过滤逻辑删除的记录
        wrapper.ne(FieldExtend::getIsDeleted, 1);
        wrapper.ne(FieldExtend::getStatus, -1);
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
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFieldId(Long fieldId) {
        // 物理删除（向后兼容）
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFieldId, fieldId);
        return this.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFieldIdLogical(Long fieldId) {
        // 逻辑删除：设置 isDeleted=1, status=-1
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFieldId, fieldId);
        List<FieldExtend> records = this.list(wrapper);
        if (records == null || records.isEmpty()) {
            return true; // 没有记录，视为删除成功
        }
        for (FieldExtend record : records) {
            record.setIsDeleted(1);
            record.setStatus(-1);
        }
        return this.updateBatchById(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFormIdLogical(String formId) {
        // 逻辑删除：按表单ID批量逻辑删除
        LambdaQueryWrapper<FieldExtend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldExtend::getFormId, formId);
        List<FieldExtend> records = this.list(wrapper);
        if (records == null || records.isEmpty()) {
            return true;
        }
        for (FieldExtend record : records) {
            record.setIsDeleted(1);
            record.setStatus(-1);
        }
        return this.updateBatchById(records);
    }
}
