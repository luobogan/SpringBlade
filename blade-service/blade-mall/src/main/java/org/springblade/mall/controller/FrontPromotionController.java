package org.springblade.mall.controller;

import org.springblade.mall.service.PromotionService;
import org.springblade.mall.vo.PromotionVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端促销管理控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/promotions")
@Tag(name = "前端促销管理", description = "前端促销管理")
public class FrontPromotionController extends BladeController {

    private PromotionService promotionService;

    /**
     * 获取促销规则列表（前端）
     * @param type 促销类型（可选）
     * @param status 状态（可选）
     * @return 促销列表
     */
    @GetMapping
    @Operation(summary = "获取促销规则列表（前端）", description = "获取促销规则列表")
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
     * 获取促销规则详情（前端）
     * @param id 促销ID
     * @return 促销详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取促销规则详情（前端）", description = "获取促销规则详情")
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
}



