package org.springblade.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.CategoryParamTemplateDTO;
import org.springblade.mall.service.CategoryParamTemplateService;
import org.springblade.mall.vo.CategoryParamTemplateVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端分类参数模板控制器
 * 处理前端的分类参数模板相关请求
 */
@RestController
@AllArgsConstructor
@RequestMapping("/category-param-templates")
@Tag(name = "前端分类参数模板管理", description = "前端分类参数模板管理")
public class FrontCategoryParamTemplateController extends BladeController {

    private CategoryParamTemplateService categoryParamTemplateService;

    /**
     * 创建参数模板
     * @param dto 参数模板信息
     * @return 创建的参数模板
     */
    @PostMapping
    @Operation(summary = "创建参数模板", description = "传入CategoryParamTemplateDTO")
    public R<CategoryParamTemplateVO> createParamTemplate(
            @Parameter(description = "参数模板信息") @Valid @RequestBody CategoryParamTemplateDTO dto) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.createParamTemplate(dto);
            return R.data(paramTemplate);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新参数模板
     * @param id 参数模板ID
     * @param dto 参数模板信息
     * @return 更新的参数模板
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新参数模板", description = "传入id和CategoryParamTemplateDTO")
    public R<CategoryParamTemplateVO> updateParamTemplate(
            @Parameter(description = "参数模板ID") @PathVariable Long id,
            @Parameter(description = "参数模板信息") @Valid @RequestBody CategoryParamTemplateDTO dto) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.updateParamTemplate(id, dto);
            return R.data(paramTemplate);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除参数模板
     * @param id 参数模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除参数模板", description = "传入参数模板ID")
    public R<Void> deleteParamTemplate(
            @Parameter(description = "参数模板ID") @PathVariable Long id) {
        try {
            categoryParamTemplateService.deleteParamTemplate(id);
            return R.success("参数模板删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取参数模板详情
     * @param id 参数模板ID
     * @return 参数模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取参数模板详情", description = "传入参数模板ID")
    public R<CategoryParamTemplateVO> getParamTemplateById(
            @Parameter(description = "参数模板ID") @PathVariable Long id) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.getParamTemplateById(id);
            return R.data(paramTemplate);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类的所有参数模板
     * @param categoryId 分类ID
     * @return 参数模板列表
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "获取分类的所有参数模板", description = "传入categoryId")
    public R<List<CategoryParamTemplateVO>> getParamTemplatesByCategoryId(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        try {
            List<CategoryParamTemplateVO> paramTemplates = categoryParamTemplateService.getParamTemplatesByCategoryId(categoryId);
            return R.data(paramTemplates);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询参数模板
     * @param categoryId 分类ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询参数模板", description = "传入分页参数")
    public R<IPage<CategoryParamTemplateVO>> getParamTemplatePage(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            IPage<CategoryParamTemplateVO> result = categoryParamTemplateService.getParamTemplatePage(categoryId, page, pageSize);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



