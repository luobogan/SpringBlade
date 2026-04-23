/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 评价控制器 AppConstant.APPLICATION_MALL_NAME +
 */
@RestController
@RequestMapping("/review")
@AllArgsConstructor
public class ReviewController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping("/submit")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "创建评价", description = "传入reviewDTO")
    public R<ReviewVO> createReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        try {
            ReviewVO reviewVO = reviewService.createReview(reviewDTO);
            return R.data(reviewVO, "评价创建成功");
        } catch (Exception e) {
            log.error("创建评价失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除评价
     */
    @PostMapping("/remove")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "删除评价", description = "传入id")
    public R<?> deleteReview(@Parameter(description = "评价ID", required = true) @RequestParam Long id) {
        try {
            reviewService.deleteReview(id);
            return R.success("评价删除成功");
        } catch (Exception e) {
            log.error("删除评价失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/detail")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "查看详情", description = "传入id")
    public R<ReviewVO> getReviewById(@Parameter(description = "评价ID", required = true) @RequestParam Long id) {
        try {
            ReviewVO reviewVO = reviewService.getReviewById(id);
            return R.data(reviewVO);
        } catch (Exception e) {
            log.error("获取评价详情失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品评价列表
     */
    @GetMapping("/product/list")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "获取商品评价列表", description = "传入productId")
    public R<List<ReviewVO>> getReviewsByProductId(@Parameter(description = "商品ID", required = true) @RequestParam Long productId) {
        try {
            List<ReviewVO> reviews = reviewService.getReviewsByProductId(productId);
            return R.data(reviews);
        } catch (Exception e) {
            log.error("获取商品评价列表失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取当前用户评价列表
     */
    @GetMapping("/user/list")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "获取当前用户评价列表", description = "无需参数")
    public R<List<ReviewVO>> getCurrentUserReviews(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }

        try {
            List<ReviewVO> reviews = reviewService.getReviewsByUserId(user.getUserId());
            return R.data(reviews);
        } catch (Exception e) {
            log.error("获取用户评价列表失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取所有评价（管理员）
     */
    @GetMapping("/list")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "获取所有评价", description = "无需参数")
    public R<List<ReviewVO>> getAllReviews() {
        try {
            List<ReviewVO> reviews = reviewService.getAllReviews();
            return R.data(reviews);
        } catch (Exception e) {
            log.error("获取所有评价失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据评分获取评价
     */
    @GetMapping("/rating/list")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "根据评分获取评价", description = "传入productId和rating")
    public R<List<ReviewVO>> getReviewsByRating(@Parameter(description = "商品ID", required = true) @RequestParam Long productId,
                                               @Parameter(description = "评分", required = true) @RequestParam Integer rating) {
        try {
            List<ReviewVO> reviews = reviewService.getReviewsByRating(productId, rating);
            return R.data(reviews);
        } catch (Exception e) {
            log.error("根据评分获取评价失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品评价统计
     */
    @GetMapping("/stats")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "获取商品评价统计", description = "传入productId")
    public R<ReviewService.ReviewStatsVO> getReviewStats(@Parameter(description = "商品ID", required = true) @RequestParam Long productId) {
        try {
            ReviewService.ReviewStatsVO stats = reviewService.getReviewStats(productId);
            return R.data(stats);
        } catch (Exception e) {
            log.error("获取商品评价统计失败", e);
            return R.fail(e.getMessage());
        }
    }
}


