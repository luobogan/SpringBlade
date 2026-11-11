package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.WorkflowFormula;
import org.springblade.formmode.service.IWorkflowFormulaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义函数控制器
 * 对标泛微E9 workflow_formula 表（新建自定义函数）
 */
@RestController
@RequestMapping("/formula")
@Tag(name = "自定义函数管理", description = "自定义函数管理（对标泛微E9 workflow_formula）")
@RequiredArgsConstructor
public class WorkflowFormulaController {

    private final IWorkflowFormulaService workflowFormulaService;

    /**
     * 获取可用自定义函数列表（全局 + 表单作用域）
     */
    @GetMapping("/list")
    @Operation(summary = "获取自定义函数列表", description = "返回全局及指定表单作用域下可用的自定义函数")
    public R<List<WorkflowFormula>> list(
            @Parameter(description = "表单ID（可选，用于作用域过滤）") @RequestParam(required = false) String formId) {
        return R.data(workflowFormulaService.getAvailable(formId));
    }

    /**
     * 保存或更新自定义函数
     */
    @PostMapping("/save")
    @Operation(summary = "保存自定义函数", description = "新增或更新自定义函数（按ID判断）")
    public R<WorkflowFormula> save(
            @Parameter(description = "表单ID（可选，新建时作为作用域）") @RequestParam(required = false) String formId,
            @RequestBody WorkflowFormula formula) {
        boolean result = workflowFormulaService.saveOrUpdateFormula(formula, formId);
        return result ? R.data(formula, "保存成功") : R.fail("保存失败");
    }

    /**
     * 删除自定义函数（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除自定义函数", description = "逻辑删除自定义函数")
    public R<Boolean> delete(
            @Parameter(description = "函数ID") @PathVariable Long id) {
        boolean result = workflowFormulaService.deleteById(id);
        return result ? R.success("删除成功") : R.fail("删除失败");
    }
}
