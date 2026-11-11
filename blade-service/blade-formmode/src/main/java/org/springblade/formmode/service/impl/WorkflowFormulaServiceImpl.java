package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.WorkflowFormula;
import org.springblade.formmode.mapper.WorkflowFormulaMapper;
import org.springblade.formmode.service.IWorkflowFormulaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义函数服务实现类
 */
@Slf4j
@Service
public class WorkflowFormulaServiceImpl extends ServiceImpl<WorkflowFormulaMapper, WorkflowFormula> implements IWorkflowFormulaService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<WorkflowFormula> getAvailable(String formId) {
        LambdaQueryWrapper<WorkflowFormula> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(WorkflowFormula::getIsDeleted, 1);
        wrapper.eq(WorkflowFormula::getStatus, 1);
        // 作用域：全局（formscope 为空）或匹配指定表单
        // ⚠️ formId 为空时只取全局：**不要**用 eq(false, ...) 拼 OR（会生成可疑 SQL），
        //    前端在拿不到表单ID时（如审批态未注入）会不带 formId 调用 list。
        wrapper.and(w -> {
            w.isNull(WorkflowFormula::getFormScope);
            if (org.springframework.util.StringUtils.hasText(formId)) {
                w.or().eq(WorkflowFormula::getFormScope, formId);
            }
        });
        wrapper.orderByAsc(WorkflowFormula::getId);
        return this.list(wrapper);
    }

    /**
     * 根据ID获取自定义函数（委托 MyBatis-Plus 实现，勿写成 this.getById(id) 以免无限递归）
     */
    @Override
    public WorkflowFormula getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateFormula(WorkflowFormula formula, String formId) {
        String now = LocalDateTime.now().format(DTF);
        if (formula.getId() != null) {
            WorkflowFormula existing = this.getById(formula.getId());
            if (existing == null) {
                return false;
            }
            formula.setModifyTime(now);
            return this.updateById(formula);
        } else {
            formula.setCreateTime(now);
            formula.setModifyTime(now);
            formula.setIsDeleted(0);
            if (formula.getStatus() == null) {
                formula.setStatus(1);
            }
            if (!org.springframework.util.StringUtils.hasText(formula.getFormScope()) && org.springframework.util.StringUtils.hasText(formId)) {
                formula.setFormScope(formId);
            }
            return this.save(formula);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        WorkflowFormula formula = this.getById(id);
        if (formula == null) {
            return true;
        }
        formula.setIsDeleted(1);
        formula.setStatus(-1);
        return this.updateById(formula);
    }
}
