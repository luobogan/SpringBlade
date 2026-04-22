package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.CategoryAttributeDTO;
import org.springblade.mall.dto.CategoryAttributeValueDTO;
import org.springblade.mall.service.CategoryAttributeService;
import org.springblade.mall.vo.CategoryAttributeVO;
import org.springblade.mall.vo.CategoryAttributeValueVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端分类属性控制器
 * 处理前端的分类属性相关请求
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/front/category-attributes")
@Tag(name = "前端分类属性管理", description = "前端分类属性管理")
public class FrontCategoryAttributeController extends BladeController {

    private CategoryAttributeService categoryAttributeService;

    /**
     * 获取分类的所有属性
     * @param categoryId 分类ID
     * @return 分类属性列表
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "获取分类的所有属性", description = "传入categoryId")
    public R<List<CategoryAttributeVO>> getAttributesByCategoryId(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        try {
            List<CategoryAttributeVO> attributes = categoryAttributeService.getAttributesByCategoryId(categoryId);
            return R.data(attributes);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 创建分类属性
     * @param dto 分类属性信息
     * @return 创建的属性
     */
    @PostMapping
    @Operation(summary = "创建分类属性", description = "传入CategoryAttributeDTO")
    public R<CategoryAttributeVO> createAttribute(
            @Parameter(description = "分类属性信息") @Valid @RequestBody CategoryAttributeDTO dto) {
        try {
            CategoryAttributeVO attribute = categoryAttributeService.createAttribute(dto);
            return R.data(attribute);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量添加属性值
     * @param attributeId 属性ID
     * @param values 属性值列表
     * @return 添加的属性值列表
     */
    @PostMapping("/{attributeId}/values/batch")
    @Operation(summary = "批量添加属性值", description = "传入attributeId和CategoryAttributeValueDTO列表")
    public R<List<CategoryAttributeValueVO>> batchAddAttributeValues(
            @Parameter(description = "属性ID") @PathVariable Long attributeId,
            @Parameter(description = "属性值列表") @RequestBody List<CategoryAttributeValueDTO> values) {
        try {
            List<CategoryAttributeValueVO> result = categoryAttributeService.batchAddAttributeValues(attributeId, values);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除分类属性
     * @param id 属性ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类属性", description = "传入属性ID")
    public R<Void> deleteAttribute(
            @Parameter(description = "属性ID") @PathVariable Long id) {
        try {
            categoryAttributeService.deleteAttribute(id);
            return R.success("分类属性删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



