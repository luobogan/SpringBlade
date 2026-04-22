package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.CategoryAttributeDTO;
import org.springblade.mall.dto.CategoryAttributeValueDTO;
import org.springblade.mall.dto.BatchAddAttributeValuesDTO;
import org.springblade.mall.service.CategoryAttributeService;
import org.springblade.mall.vo.CategoryAttributeVO;
import org.springblade.mall.vo.CategoryAttributeValueVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类属性管理控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/category-attributes")
@AllArgsConstructor
public class CategoryAttributeController extends BladeController {

    private CategoryAttributeService categoryAttributeService;

    /**
     * 简单测试端点
     */
    @GetMapping("/test")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "测试端点", description = "测试端点")
    public R<String> test() {
        return R.data("测试成功");
    }

    /**
     * 创建分类属性
     */
    @PostMapping
    @ApiOperationSupport(order = 2)
    @Operation(summary = "创建分类属性", description = "传入CategoryAttributeDTO")
    public R<CategoryAttributeVO> createAttribute(@Valid @RequestBody CategoryAttributeDTO dto) {
        try {
            CategoryAttributeVO attribute = categoryAttributeService.createAttribute(dto);
            return R.data(attribute);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新分类属性
     */
    @PutMapping("/{id}")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "更新分类属性", description = "传入id和CategoryAttributeDTO")
    public R<CategoryAttributeVO> updateAttribute(
            @PathVariable Long id,
            @Valid @RequestBody CategoryAttributeDTO dto) {
        try {
            CategoryAttributeVO attribute = categoryAttributeService.updateAttribute(id, dto);
            return R.data(attribute);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除分类属性
     */
    @DeleteMapping("/{id}")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "删除分类属性", description = "传入id")
    public R<?> deleteAttribute(@PathVariable Long id) {
        try {
            categoryAttributeService.deleteAttribute(id);
            return R.success("分类属性删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类属性详情
     */
    @GetMapping("/{id}")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "获取分类属性详情", description = "传入id")
    public R<CategoryAttributeVO> getAttributeById(@PathVariable Long id) {
        try {
            CategoryAttributeVO attribute = categoryAttributeService.getAttributeById(id);
            if (attribute == null) {
                return R.fail("分类属性不存在");
            }
            return R.data(attribute);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类的所有属性
     */
    @GetMapping("/category/{categoryId}")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "获取分类的所有属性", description = "传入categoryId")
    public R<List<CategoryAttributeVO>> getAttributesByCategoryId(
            @PathVariable Long categoryId) {
        try {
            List<CategoryAttributeVO> attributes = categoryAttributeService.getAttributesByCategoryId(categoryId);
            return R.data(attributes);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询分类属性
     */
    @GetMapping
    @ApiOperationSupport(order = 7)
    @Operation(summary = "分页查询分类属性", description = "传入categoryId")
    public R<?> getAttributePage(
            Query query,
            @RequestParam(required = false) Long categoryId) {
        try {
            Object result = categoryAttributeService.getAttributePage(categoryId, query.getCurrent(), query.getSize());
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 添加属性值
     */
    @PostMapping("/values")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "添加属性值", description = "传入CategoryAttributeValueDTO")
    public R<CategoryAttributeValueVO> addAttributeValue(
            @Valid @RequestBody CategoryAttributeValueDTO dto) {
        try {
            CategoryAttributeValueVO value = categoryAttributeService.addAttributeValue(dto);
            return R.data(value);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新属性值
     */
    @PutMapping("/values/{id}")
    @ApiOperationSupport(order = 9)
    @Operation(summary = "更新属性值", description = "传入id和CategoryAttributeValueDTO")
    public R<CategoryAttributeValueVO> updateAttributeValue(
            @PathVariable Long id,
            @Valid @RequestBody CategoryAttributeValueDTO dto) {
        try {
            CategoryAttributeValueVO value = categoryAttributeService.updateAttributeValue(id, dto);
            return R.data(value);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除属性值
     */
    @DeleteMapping("/values/{id}")
    @ApiOperationSupport(order = 10)
    @Operation(summary = "删除属性值", description = "传入id")
    public R<?> deleteAttributeValue(@PathVariable Long id) {
        try {
            categoryAttributeService.deleteAttributeValue(id);
            return R.success("属性值删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取属性的所有值
     */
    @GetMapping("/{attributeId}/values")
    @ApiOperationSupport(order = 11)
    @Operation(summary = "获取属性的所有值", description = "传入attributeId")
    public R<List<CategoryAttributeValueVO>> getAttributeValues(
            @PathVariable Long attributeId) {
        try {
            List<CategoryAttributeValueVO> values = categoryAttributeService.getAttributeValues(attributeId);
            return R.data(values);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量添加属性值
     */
    @PostMapping("/{attributeId}/values/batch")
    @ApiOperationSupport(order = 12)
    @Operation(summary = "批量添加属性值", description = "传入attributeId和CategoryAttributeValueDTO列表")
    public R<List<CategoryAttributeValueVO>> batchAddAttributeValues(
            @PathVariable Long attributeId,
            @RequestBody BatchAddAttributeValuesDTO batchDTO) {
        try {
            List<CategoryAttributeValueVO> result = categoryAttributeService.batchAddAttributeValues(attributeId, batchDTO.getValues());
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 复制分类属性到其他分类
     */
    @PostMapping("/copy")
    @ApiOperationSupport(order = 13)
    @Operation(summary = "复制分类属性到其他分类", description = "传入sourceCategoryId和targetCategoryId")
    public R<?> copyAttributesToCategory(
            @RequestParam Long sourceCategoryId,
            @RequestParam Long targetCategoryId) {
        try {
            categoryAttributeService.copyAttributesToCategory(sourceCategoryId, targetCategoryId);
            return R.success("复制成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



