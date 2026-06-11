package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.FieldOption;
import org.springblade.formmode.service.IFieldOptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字段选项配置控制器
 * 对标泛微E9 selectItem 表
 */
@RestController
@RequestMapping("/api/formmode/field-option")
@Tag(name = "字段选项配置管理", description = "字段选项配置管理（对标泛微E9）")
@RequiredArgsConstructor
public class FieldOptionController {

    private final IFieldOptionService fieldOptionService;

    /**
     * 根据字段ID获取选项列表
     */
    @GetMapping("/by-field/{fieldId}")
    @Operation(summary = "根据字段ID获取选项列表", description = "根据字段ID获取字段选项列表")
    public R<List<FieldOption>> getByFieldId(
            @Parameter(description = "字段ID") @PathVariable Long fieldId) {
        List<FieldOption> list = fieldOptionService.getByFieldId(fieldId);
        return R.data(list);
    }

    /**
     * 根据表单ID获取选项列表
     */
    @GetMapping("/by-form/{formId}")
    @Operation(summary = "根据表单ID获取选项列表", description = "根据表单ID获取字段选项列表")
    public R<List<FieldOption>> getByFormId(
            @Parameter(description = "表单ID") @PathVariable String formId) {
        List<FieldOption> list = fieldOptionService.getByFormId(formId);
        return R.data(list);
    }

    /**
     * 保存字段选项列表（先删除再插入）
     */
    @PostMapping("/save")
    @Operation(summary = "保存字段选项列表", description = "保存字段选项列表（先删除再插入）")
    public R<Boolean> saveOptions(
            @Parameter(description = "字段ID") @RequestParam Long fieldId,
            @Parameter(description = "表单ID") @RequestParam String formId,
            @RequestBody List<FieldOption> options) {
        boolean result = fieldOptionService.saveOptions(fieldId, formId, options);
        return result ? R.success("保存成功") : R.fail("保存失败");
    }

    /**
     * 删除字段选项（根据字段ID）
     */
    @DeleteMapping("/by-field/{fieldId}")
    @Operation(summary = "删除字段选项", description = "根据字段ID删除字段选项")
    public R<Boolean> deleteByFieldId(
            @Parameter(description = "字段ID") @PathVariable Long fieldId) {
        boolean result = fieldOptionService.deleteByFieldId(fieldId);
        return result ? R.success("删除成功") : R.fail("删除失败");
    }
}
