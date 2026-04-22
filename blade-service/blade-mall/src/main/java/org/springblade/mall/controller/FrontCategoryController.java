package org.springblade.mall.controller;

import org.springblade.mall.service.CategoryService;
import org.springblade.mall.vo.CategoryVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 前端分类控制器
 * 处理前端的分类相关请求，路径为 /categories
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/front/categories")
@Tag(name = "前端分类管理", description = "前端分类管理")
public class FrontCategoryController extends BladeController {

    private CategoryService categoryService;

    /**
     * 获取所有分类
     * @param top 是否只获取顶级分类
     * @param valid 是否只获取有效的分类
     * @return 分类列表
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "获取所有分类")
    public R<List<CategoryVO>> getAllCategories(
            @Parameter(description = "是否只获取顶级分类") @RequestParam(required = false) Integer top,
            @Parameter(description = "是否只获取有效的分类") @RequestParam(required = false) Integer valid) {
        try {
            List<CategoryVO> categories = categoryService.getAllCategories();

            // 过滤顶级分类
            if (top != null && top == 1) {
                categories = categories.stream()
                        .filter(category -> category.getParentId() == 0 || category.getParentId() == null)
                        .collect(Collectors.toList());
            }

            // 过滤有效的分类
            if (valid != null && valid == 1) {
                categories = categories.stream()
                        .filter(category -> category.getStatus() == 1)
                        .collect(Collectors.toList());
            }

            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类树
     * @return 分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取分类树")
    public R<List<CategoryVO>> getCategoryTree() {
        try {
            List<CategoryVO> categories = categoryService.getAllCategories();
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @GetMapping("/sub/{parentId}")
    @Operation(summary = "获取子分类", description = "获取子分类")
    public R<List<CategoryVO>> getSubCategories(@Parameter(description = "父分类ID") @PathVariable Long parentId) {
        try {
            List<CategoryVO> categories = categoryService.getSubCategories(parentId);
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取活跃分类
     * @return 活跃分类列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃分类", description = "获取活跃分类")
    public R<List<CategoryVO>> getActiveCategories() {
        try {
            List<CategoryVO> categories = categoryService.getActiveCategories();
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



