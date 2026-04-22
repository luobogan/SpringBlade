package org.springblade.mall.controller;

import org.springblade.mall.dto.ProductRelationDTO;
import org.springblade.mall.service.ProductRelationService;
import org.springblade.mall.vo.ProductRelationVO;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品关联管理控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/admin/product-relations")
@Tag(name = "商品关联管理", description = "商品关联管理")
public class ProductRelationController extends BladeController {

    private ProductRelationService productRelationService;

    /**
     * 获取商品的关联商品列表
     * @param productId 商品ID
     * @param type 关联类型（可选）
     * @return 关联商品列表
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "获取商品关联列表", description = "获取商品的关联商品列表")
    public R<List<ProductRelationVO>> getProductRelations(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "关联类型") @RequestParam(required = false) Integer type) {
        try {
            List<ProductRelationVO> relations = productRelationService.getProductRelations(productId, type);
            return R.data(relations);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 添加商品关联
     * @param relationDTO 关联信息
     * @return 创建的关联
     */
    @PostMapping
    @Operation(summary = "添加商品关联", description = "添加商品关联")
    public R<ProductRelationVO> addProductRelation(
            @Parameter(description = "关联信息") @RequestBody ProductRelationDTO relationDTO) {
        try {
            ProductRelationVO relation = productRelationService.addProductRelation(relationDTO);
            R<ProductRelationVO> result = R.data(relation);
            result.setMsg("关联添加成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量添加商品关联
     * @param productId 主商品ID
     * @param relations 关联列表
     * @return 创建的关联列表
     */
    @PostMapping("/product/{productId}/batch")
    @Operation(summary = "批量添加商品关联", description = "批量添加商品关联")
    public R<List<ProductRelationVO>> batchAddProductRelations(
            @Parameter(description = "主商品ID") @PathVariable Long productId,
            @Parameter(description = "关联列表") @RequestBody List<ProductRelationDTO> relations) {
        try {
            List<ProductRelationVO> resultList = productRelationService.batchAddProductRelations(productId, relations);
            R<List<ProductRelationVO>> result = R.data(resultList);
            result.setMsg("批量关联添加成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新商品关联
     * @param id 关联ID
     * @param relationDTO 关联信息
     * @return 更新后的关联
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新商品关联", description = "更新商品关联")
    public R<ProductRelationVO> updateProductRelation(
            @Parameter(description = "关联ID") @PathVariable Long id,
            @Parameter(description = "关联信息") @RequestBody ProductRelationDTO relationDTO) {
        try {
            ProductRelationVO relation = productRelationService.updateProductRelation(id, relationDTO);
            R<ProductRelationVO> result = R.data(relation);
            result.setMsg("关联更新成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除商品关联
     * @param id 关联ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品关联", description = "删除商品关联")
    public R<?> deleteProductRelation(@Parameter(description = "关联ID") @PathVariable Long id) {
        try {
            productRelationService.deleteProductRelation(id);
            return R.success("关联删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量删除商品关联
     * @param ids 关联ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除商品关联", description = "批量删除商品关联")
    public R<?> batchDeleteProductRelations(@Parameter(description = "关联ID列表") @RequestBody List<Long> ids) {
        try {
            productRelationService.batchDeleteProductRelations(ids);
            return R.success("批量删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 清空商品的关联商品
     * @param productId 商品ID
     * @param type 关联类型（可选）
     * @return 清空结果
     */
    @DeleteMapping("/product/{productId}")
    @Operation(summary = "清空商品关联", description = "清空商品的关联商品")
    public R<?> clearProductRelations(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "关联类型") @RequestParam(required = false) Integer type) {
        try {
            productRelationService.clearProductRelations(productId, type);
            return R.success("关联清空成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



