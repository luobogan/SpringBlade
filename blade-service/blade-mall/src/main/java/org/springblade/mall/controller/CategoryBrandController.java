package org.springblade.mall.controller;

import org.springblade.mall.entity.Brand;
import org.springblade.mall.service.CategoryBrandService;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/category-brands")
@Tag(name = "分类品牌管理", description = "分类品牌管理")
public class CategoryBrandController extends BladeController {

    private CategoryBrandService categoryBrandService;

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "根据分类ID获取品牌列表", description = "根据分类ID获取品牌列表")
    public R<List<Brand>> getBrandsByCategoryId(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        try {
            List<Brand> brands = categoryBrandService.getBrandsByCategoryId(categoryId);
            return R.data(brands);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/category/{categoryId}")
    @Operation(summary = "保存分类品牌关联", description = "保存分类品牌关联")
    public R<?> saveCategoryBrands(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "品牌ID列表") @RequestBody List<Long> brandIds) {
        try {
            categoryBrandService.saveCategoryBrands(categoryId, brandIds);
            return R.success("品牌关联保存成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("/category/{categoryId}/brand/{brandId}")
    @Operation(summary = "从分类中移除品牌", description = "从分类中移除品牌")
    public R<?> removeBrandFromCategory(
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



