package org.springblade.mall.controller;

import org.springblade.mall.service.ProductService;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 前端商品统计控制器
 * 处理前端的商品统计相关请求
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/products")
@Tag(name = "前端商品统计", description = "前端商品统计")
public class FrontProductStatsController extends BladeController {

    private ProductService productService;

    /**
     * 获取商品统计数据
     * @return 统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取商品统计数据", description = "获取商品统计数据")
    public R<Map<String, Object>> getProductStats() {
        try {
            Map<String, Object> stats = productService.getProductStats();
            return R.data(stats);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}


