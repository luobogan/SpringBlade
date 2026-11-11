package org.springblade.formmode.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.springblade.formmode.entity.WorkflowFormula;

import java.util.List;

/**
 * 自定义函数服务接口
 * 对标泛微E9 workflow_formula 表
 */
public interface IWorkflowFormulaService extends IService<WorkflowFormula> {

    /**
     * 查询可用自定义函数列表（全局 + 指定表单作用域），过滤逻辑删除
     * @param formId 表单ID（可为空，仅用于作用域过滤）
     */
    List<WorkflowFormula> getAvailable(String formId);

    /**
     * 根据ID获取自定义函数
     */
    WorkflowFormula getById(Long id);

    /**
     * 保存或更新自定义函数（按ID判断新增/更新，自动维护时间字段）
     */
    boolean saveOrUpdateFormula(WorkflowFormula formula, String formId);

    /**
     * 逻辑删除自定义函数
     */
    boolean deleteById(Long id);
}
