package org.springblade.mall.controller;

import org.springblade.mall.mapper.OrderMapper;
import org.springblade.mall.mapper.ProductMapper;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.utils.SecureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.constant.RoleConstant;
import org.springframework.web.bind.annotation.*;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.system.user.feign.IUserClient;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理员仪表盘控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/admin/dashboard")
public class DashboardController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private IUserClient userClient;

    /**
     * 获取统计数据
     * @return 统计数据
     */
    @GetMapping("/stats")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public ResponseEntity<R<Map<String, Object>>> getStats() {
        log.info("=== DashboardController.getStats() 被调用 ===");
        try {
            Map<String, Object> stats = new HashMap<>();

            // 商品总数
            Long productCount = productMapper.selectCount(null);
            stats.put("productCount", productCount);
            log.info("商品总数: {}", productCount);

            // 订单总数（使用自定义方法避免 MySQL 关键字问题）
            Long orderCount = orderMapper.countAllOrders();
            stats.put("orderCount", orderCount);
            log.info("订单总数: {}", orderCount);

            // 用户总数
            // 注意：由于 IUserClient 接口暂未提供获取用户总数的方法，
            // 这里使用订单表中的唯一用户ID数量作为用户总数的近似值
            Long userCount = 0L;
            try {
                userCount = orderMapper.countDistinctUsers();
            } catch (Exception e) {
                log.error("获取用户总数失败: {}", e.getMessage());
            }
            stats.put("userCount", userCount);
            log.info("用户总数: {}", userCount);

            // 销售总额（这里简单计算，实际应该从订单表统计）
            stats.put("totalSales", 0.0);

            // 最近订单（模拟数据）
            List<Map<String, Object>> recentOrders = List.of();
            stats.put("recentOrders", recentOrders);

            // 热销商品（模拟数据）
            List<Map<String, Object>> topProducts = List.of();
            stats.put("topProducts", topProducts);

            log.info("返回统计数据");
            return ResponseEntity.ok(R.data(stats));
        } catch (Exception e) {
            log.error("getStats 异常: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 获取销售趋势
     * @param days 天数
     * @return 销售趋势数据
     */
    @GetMapping("/sales-trend")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public ResponseEntity<R<List<Map<String, Object>>>> getSalesTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<Map<String, Object>> trend = List.of();
            return ResponseEntity.ok(R.data(trend));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 获取订单趋势
     * @param days 天数
     * @return 订单趋势数据
     */
    @GetMapping("/order-trend")
    @PreAuth(RoleConstant.HAS_ROLE_ADMIN)
    public ResponseEntity<R<List<Map<String, Object>>>> getOrderTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<Map<String, Object>> trend = List.of();
            return ResponseEntity.ok(R.data(trend));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }
}



