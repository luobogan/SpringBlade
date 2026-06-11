package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.FieldExtend;
import org.springblade.formmode.service.IFieldExtendService;
import org.springframework.web.bind.annotation.*;

/**
 * 字段扩展属性控制器
 * 对标泛微E9 ModeFormFieldExtend 表
 */
@RestController
@RequestMapping("/api/formmode/field-extend")
@Tag(name = "字段扩展属性管理", description = "字段扩展属性管理（对标泛微E9）")
@RequiredArgsConstructor
public class FieldExtendController {

    private final IFieldExtendService fieldExtendService;

    /**
     * 根据字段ID获取扩展属性
     */
    @GetMapping("/by-field/{fieldId}")
    @Operation(summary = "根据字段ID获取扩展属性", description = "根据字段ID获取字段扩展属性")
    public R<FieldExtend> getByFieldId(
            @Parameter(description = "字段ID") @PathVariable Long fieldId) {
        FieldExtend fieldExtend = fieldExtendService.getByFieldId(fieldId);
        return R.data(fieldExtend);
    }

    /**
     * 根据表单ID获取扩展属性列表
     */
    @GetMapping("/by-form/{formId}")
    @Operation(summary = "根据表单ID获取扩展属性列表", description = "根据表单ID获取字段扩展属性列表")
    public R<java.util.List<FieldExtend>> getByFormId(
            @Parameter(description = "表单ID") @PathVariable String formId) {
        java.util.List<FieldExtend> list = fieldExtendService.getByFormId(formId);
        return R.data(list);
    }

    /**
     * 保存或更新字段扩展属性
     */
    @PostMapping("/save")
    @Operation(summary = "保存字段扩展属性", description = "保存或更新字段扩展属性")
    public R<FieldExtend> save(@RequestBody FieldExtend fieldExtend) {
        boolean result = fieldExtendService.saveOrUpdateByFieldId(fieldExtend);
        if (result) {
            return R.data(fieldExtend, "保存成功");
        } else {
            return R.fail("保存失败");
        }
    }

    /**
     * 删除字段扩展属性（根据字段ID）
     */
    @DeleteMapping("/by-field/{fieldId}")
    @Operation(summary = "删除字段扩展属性", description = "根据字段ID删除字段扩展属性")
    public R<Boolean> deleteByFieldId(
            @Parameter(description = "字段ID") @PathVariable Long fieldId) {
        boolean result = fieldExtendService.deleteByFieldId(fieldId);
        return result ? R.success("删除成功") : R.fail("删除失败");
    }
}
