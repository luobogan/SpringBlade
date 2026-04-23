package org.springblade.mall.controller;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.mall.dto.PromotionDTO;
import org.springblade.mall.dto.PromotionProductDTO;
import org.springblade.mall.service.PromotionService;
import org.springblade.mall.vo.PromotionVO;
import org.springblade.mall.vo.PromotionProductVO;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 促销规则管理控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/admin/promotions")
@Tag(name = "促销规则管理", description = "促销规则管理")
public class PromotionController extends BladeController {

    private PromotionService promotionService;

    /**
     * 创建促销规则
     * @param promotionDTO 促销信息
     * @return 创建的促销
     */
    @PostMapping
    @Operation(summary = "创建促销规则", description = "创建促销规则")
    public R<PromotionVO> createPromotion(@Parameter(description = "促销信息") @Valid @RequestBody PromotionDTO promotionDTO) {
        try {
            PromotionVO promotion = promotionService.createPromotion(promotionDTO);
            R<PromotionVO> result = R.data(promotion);
            result.setMsg("促销规则创建成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新促销规则
     * @param id 促销ID
     * @param promotionDTO 促销信息
     * @return 更新后的促销
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新促销规则", description = "更新促销规则")
    public R<PromotionVO> updatePromotion(
            @Parameter(description = "促销ID") @PathVariable Long id,
            @Parameter(description = "促销信息") @Valid @RequestBody PromotionDTO promotionDTO) {
        try {
            PromotionVO promotion = promotionService.updatePromotion(id, promotionDTO);
            R<PromotionVO> result = R.data(promotion);
            result.setMsg("促销规则更新成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除促销规则
     * @param id 促销ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除促销规则", description = "删除促销规则")
    public R<?> deletePromotion(@Parameter(description = "促销ID") @PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return R.success("促销规则删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取促销规则详情
     * @param id 促销ID
     * @return 促销详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取促销规则详情", description = "获取促销规则详情")
    public R<PromotionVO> getPromotionById(@Parameter(description = "促销ID") @PathVariable Long id) {
        try {
            PromotionVO promotion = promotionService.getPromotionById(id);
            if (promotion == null) {
                return R.fail("促销规则不存在");
            }
            return R.data(promotion);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取促销规则列表
     * @param type 促销类型（可选）
     * @param status 状态（可选）
     * @return 促销列表
     */
    @GetMapping
    @Operation(summary = "获取促销规则列表", description = "获取促销规则列表")
    public R<List<PromotionVO>> getPromotionList(
            @Parameter(description = "促销类型") @RequestParam(required = false) Integer type,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        try {
            List<PromotionVO> promotions = promotionService.getPromotionList(type, status);
            return R.data(promotions);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新促销状态
     * @param id 促销ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新促销状态", description = "更新促销状态")
    public R<?> updatePromotionStatus(
            @Parameter(description = "促销ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        try {
            promotionService.updatePromotionStatus(id, status);
            return R.success("状态更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 添加促销商品
     * @param promotionProductDTO 促销商品信息
     * @return 创建的促销商品关联
     */
    @PostMapping("/products")
    @Operation(summary = "添加促销商品", description = "添加促销商品")
    public R<PromotionProductVO> addPromotionProduct(
            @Parameter(description = "促销商品信息") @Valid @RequestBody PromotionProductDTO promotionProductDTO) {
        try {
            PromotionProductVO product = promotionService.addPromotionProduct(promotionProductDTO);
            R<PromotionProductVO> result = R.data(product);
            result.setMsg("促销商品添加成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量添加促销商品
     * @param promotionId 促销ID
     * @param products 商品列表
     * @return 创建的促销商品关联列表
     */
    @PostMapping("/{promotionId}/products/batch")
    @Operation(summary = "批量添加促销商品", description = "批量添加促销商品")
    public R<List<PromotionProductVO>> batchAddPromotionProducts(
            @Parameter(description = "促销ID") @PathVariable Long promotionId,
            @Parameter(description = "商品列表") @RequestBody List<PromotionProductDTO> products) {
        try {
            List<PromotionProductVO> productsList = promotionService.batchAddPromotionProducts(promotionId, products);
            R<List<PromotionProductVO>> result = R.data(productsList);
            result.setMsg("批量添加成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除促销商品
     * @param id 关联ID
     * @return 删除结果
     */
    @DeleteMapping("/products/{id}")
    @Operation(summary = "删除促销商品", description = "删除促销商品")
    public R<?> deletePromotionProduct(@Parameter(description = "关联ID") @PathVariable Long id) {
        try {
            promotionService.deletePromotionProduct(id);
            return R.success("促销商品删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取促销商品列表
     * @param promotionId 促销ID
     * @return 促销商品列表
     */
    @GetMapping("/{promotionId}/products")
    @Operation(summary = "获取促销商品列表", description = "获取促销商品列表")
    public R<List<PromotionProductVO>> getPromotionProducts(
            @Parameter(description = "促销ID") @PathVariable Long promotionId) {
        try {
            List<PromotionProductVO> products = promotionService.getPromotionProducts(promotionId);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



