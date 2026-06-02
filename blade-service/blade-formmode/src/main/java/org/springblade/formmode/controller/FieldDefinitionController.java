package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字段定义控制器
 * 提供字段定义的 CRUD 操作
 */
@RestController
@RequestMapping("/field-definition")
@Tag(name = "字段定义管理", description = "字段定义管理")
@RequiredArgsConstructor
public class FieldDefinitionController {

    private final IFieldDefinitionService fieldDefinitionService;

    /**
     * 获取字段列表
     */
    @GetMapping
    @Operation(summary = "获取字段列表", description = "根据表单ID获取字段定义列表")
    public R<List<FieldDefinition>> list(
            @Parameter(description = "表单ID") @RequestParam Long formId) {
        List<FieldDefinition> list = fieldDefinitionService.getByFormId(formId);
        return R.data(list);
    }

    /**
     * 获取字段详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取字段详情", description = "根据ID获取字段定义详情")
    public R<FieldDefinition> getById(
            @Parameter(description = "字段ID") @PathVariable Long id) {
        FieldDefinition fieldDefinition = fieldDefinitionService.getById(id);
        return R.data(fieldDefinition);
    }

    /**
     * 创建字段
     */
    @PostMapping
    @Operation(summary = "创建字段", description = "创建新的字段定义")
    public R<FieldDefinition> create(@RequestBody FieldDefinition fieldDefinition) {
        boolean result = fieldDefinitionService.createFieldWithColumn(fieldDefinition);
        if (result) {
            return R.data(fieldDefinition, "字段创建成功");
        } else {
            return R.fail("字段创建失败");
        }
    }

    /**
     * 更新字段
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新字段", description = "更新字段定义")
    public R<FieldDefinition> update(
            @Parameter(description = "字段ID") @PathVariable Long id,
            @RequestBody FieldDefinition fieldDefinition) {
        fieldDefinition.setId(id);
        boolean result = fieldDefinitionService.updateFieldWithColumn(fieldDefinition);
        if (result) {
            return R.data(fieldDefinition, "字段更新成功");
        } else {
            return R.fail("字段更新失败");
        }
    }

    /**
     * 删除字段
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除字段", description = "删除字段定义")
    public R<Boolean> delete(
            @Parameter(description = "字段ID") @PathVariable Long id) {
        boolean result = fieldDefinitionService.deleteFieldWithColumn(id);
        return result ? R.success("字段删除成功") : R.fail("字段删除失败");
    }

    /**
     * 根据表单ID获取字段列表（前端调用）
     */
    @GetMapping("/by-form/{formId}")
    @Operation(summary = "根据表单ID获取字段", description = "根据表单ID获取字段定义列表")
    public R<List<FieldDefinition>> getByFormId(
            @Parameter(description = "表单ID") @PathVariable String formId) {
        try {
            Long billId = Long.parseLong(formId);
            List<FieldDefinition> list = fieldDefinitionService.getByFormId(billId);
            return R.data(list);
        } catch (NumberFormatException e) {
            return R.fail("无效的表单ID");
        }
    }
}
