package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.CategoryBrandRequestDTO;
import org.springblade.mall.entity.Brand;
import org.springblade.mall.service.CategoryBrandService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端分类品牌控制器
 * 处理前端的分类品牌相关请求
 */
@RestController
@AllArgsConstructor
@RequestMapping("/front/category-brands")
@Tag(name = "前端分类品牌管理", description = "前端分类品牌管理")
public class FrontCategoryBrandController extends BladeController {

    private CategoryBrandService categoryBrandService;

    /**
     * 根据分类ID获取品牌列表
     * @param categoryId 分类ID
     * @return 品牌列表
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "根据分类ID获取品牌列表", description = "传入categoryId")
    public R<List<Brand>> getBrandsByCategoryId(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        try {
            List<Brand> brands = categoryBrandService.getBrandsByCategoryId(categoryId);
            return R.data(brands);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 保存分类品牌关联
     * @param categoryId 分类ID
     * @param request 分类品牌关联请求
     * @return 操作结果
     */
    @PostMapping("/category/{categoryId}")
    @Operation(summary = "保存分类品牌关联", description = "传入categoryId和品牌ID列表")
    public R<Void> saveCategoryBrands(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "品牌ID列表") @RequestBody CategoryBrandRequestDTO request) {
        try {
            categoryBrandService.saveCategoryBrands(categoryId, request.getBrandIds());
            return R.success("品牌关联保存成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 从分类中移除品牌
     * @param categoryId 分类ID
     * @param brandId 品牌ID
     * @return 操作结果
     */
    @DeleteMapping("/category/{categoryId}/brand/{brandId}")
    @Operation(summary = "从分类中移除品牌", description = "传入categoryId和brandId")
    public R<Void> removeBrandFromCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "品牌ID") @PathVariable Long brandId) {
        try {
            categoryBrandService.removeBrandFromCategory(categoryId, brandId);
            return R.success("品牌移除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



