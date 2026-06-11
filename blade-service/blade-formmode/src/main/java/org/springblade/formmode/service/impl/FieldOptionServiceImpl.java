package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.mapper.FieldOptionMapper;
import org.springblade.formmode.service.IFieldOptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 过滤逻辑删除的记录
        wrapper.ne(FieldOption::getIsDeleted, 1);
        wrapper.ne(FieldOption::getStatus, -1);
        wrapper.orderByAsc(FieldOption::getListOrder);
        return this.list(wrapper);
    }

    @Override
    public List<FieldOption> getByFormId(String formId) {
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFormId, formId);
        // 过滤逻辑删除的记录
        wrapper.ne(FieldOption::getIsDeleted, 1);
        wrapper.ne(FieldOption::getStatus, -1);
        wrapper.orderByAsc(FieldOption::getListOrder);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOptions(Long fieldId, String formId, List<FieldOption> options) {
        // 1. 逻辑删除该字段的所有旧选项
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        List<FieldOption> oldList = this.list(wrapper);
        if (oldList != null && !oldList.isEmpty()) {
            for (FieldOption old : oldList) {
                old.setIsDeleted(1);
                old.setStatus(-1);
            }
            this.updateBatchById(oldList);
        }

        // 2. 批量插入新选项
        if (options != null && !options.isEmpty()) {
            for (FieldOption option : options) {
                option.setFieldId(fieldId);
                option.setFormId(formId);
                option.setIsDeleted(0);
                option.setStatus(1);
            }
            return this.saveBatch(options);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFieldId(Long fieldId) {
        // 物理删除（向后兼容）
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        return this.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFieldIdLogical(Long fieldId) {
        // 逻辑删除：设置 isDeleted=1, status=-1
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        List<FieldOption> records = this.list(wrapper);
        if (records == null || records.isEmpty()) {
            return true;
        }
        for (FieldOption record : records) {
            record.setIsDeleted(1);
            record.setStatus(-1);
        }
        return this.updateBatchById(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByFormIdLogical(String formId) {
        // 逻辑删除：按表单ID批量逻辑删除
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFormId, formId);
        List<FieldOption> records = this.list(wrapper);
        if (records == null || records.isEmpty()) {
            return true;
        }
        for (FieldOption record : records) {
            record.setIsDeleted(1);
            record.setStatus(-1);
        }
        return this.updateBatchById(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreByFieldId(Long fieldId) {
        // 恢复已逻辑删除的选项
        LambdaQueryWrapper<FieldOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldOption::getFieldId, fieldId);
        wrapper.eq(FieldOption::getIsDeleted, 1);
        List<FieldOption> records = this.list(wrapper);
        if (records == null || records.isEmpty()) {
            return true;
        }
        for (FieldOption record : records) {
            record.setIsDeleted(0);
            record.setStatus(1);
        }
        return this.updateBatchById(records);
    }
}
