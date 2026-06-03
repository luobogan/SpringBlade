package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.FormLayout;
import org.springblade.formmode.service.IFormLayoutService;
import org.springframework.web.bind.annotation.*;

/**
 * 表单布局控制器
 * 提供Excel设计器的后端API
 */
@RestController
@RequestMapping("/form-layout")
@Tag(name = "表单布局管理", description = "Excel设计器表单布局管理")
@RequiredArgsConstructor
@Slf4j
public class FormLayoutController extends BladeController {

    private final IFormLayoutService formLayoutService;

    /**
     * 根据表单ID获取布局
     */
    @GetMapping("/{formId}")
    @Operation(summary = "获取表单布局", description = "根据表单ID获取表单布局")
    public R<FormLayout> getByFormId(
            @Parameter(description = "表单ID") @PathVariable Long formId) {
        FormLayout formLayout = formLayoutService.getByFormId(formId);
        return R.data(formLayout);
    }

    /**
     * 保存表单布局
     */
    @PostMapping("/save")
    @Operation(summary = "保存表单布局", description = "保存Excel设计器的表单布局")
    public R<Boolean> save(@RequestBody FormLayout formLayout) {
        boolean result = formLayoutService.saveFormLayout(formLayout);
        return R.data(result);
    }

    /**
     * 解析布局JSON
     */
    @PostMapping("/parse-json")
    @Operation(summary = "解析布局JSON", description = "解析Excel设计器的布局JSON")
    public R<String> parseJson(
            @Parameter(description = "布局JSON") @RequestBody String layoutJson) {
        String result = formLayoutService.parseLayoutJson(layoutJson);
        return R.data(result);
    }

    /**
     * 删除表单布局
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除表单布局", description = "根据ID删除表单布局")
    public R<Boolean> delete(
            @Parameter(description = "布局ID") @PathVariable Long id) {
        boolean result = formLayoutService.removeById(id);
        return R.data(result);
    }
}
