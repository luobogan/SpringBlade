package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.ReviewDTO;
import org.springblade.mall.service.ReviewService;
import org.springblade.mall.vo.ReviewVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 前端评价控制器
 * 处理前端的评价相关请求
 */
@RestController
@RequestMapping("/reviews")
@AllArgsConstructor
public class FrontReviewController extends BladeController {

    private ReviewService reviewService;

    /**
     * 获取商品评价列表（公开接口）
     * @param productId 商品ID
     * @return 评价列表
     */
    @GetMapping("/product/{productId}")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "获取商品评价列表", description = "传入productId")
    public R<List<ReviewVO>> getReviewsByProductId(@PathVariable Long productId) {
        try {
            List<ReviewVO> reviews = reviewService.getReviewsByProductId(productId);
            return R.data(reviews);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品评价统计（公开接口）
     * @param productId 商品ID
     * @return 评价统计
     */
    @GetMapping("/stats/{productId}")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "获取商品评价统计", description = "传入productId")
    public R<ReviewService.ReviewStatsVO> getReviewStats(@PathVariable Long productId) {
        try {
            ReviewService.ReviewStatsVO stats = reviewService.getReviewStats(productId);
            return R.data(stats);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 创建评价（需要登录）
     * @param productId 商品ID
     * @param reviewDTO 评价信息
     * @return 创建结果
     */
    @PostMapping("/product/{productId}")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "创建评价", description = "传入productId和ReviewDTO")
    public R<ReviewVO> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewDTO reviewDTO,
            BladeUser user) {
        try {
            if (user == null) {
                return R.fail("用户未登录");
            }

            reviewDTO.setProductId(productId);

            ReviewVO reviewVO = reviewService.createReview(reviewDTO);
            return R.success("评价创建成功").data(reviewVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取当前用户评价列表（需要登录）
     * @return 评价列表
     */
    @GetMapping("/user")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "获取当前用户评价列表", description = "获取当前用户评价列表")
    public R<List<ReviewVO>> getCurrentUserReviews(BladeUser user) {
        try {
            if (user == null) {
                return R.fail("用户未登录");
            }

            List<ReviewVO> reviews = reviewService.getReviewsByUserId(user.getUserId());
            return R.data(reviews);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



