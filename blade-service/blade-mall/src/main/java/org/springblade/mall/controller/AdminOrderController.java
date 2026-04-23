package org.springblade.mall.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.service.OrderService;
import org.springblade.mall.vo.OrderVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 管理员订单控制器
 */
@RestController
@RequestMapping("/admin/orders")
@AllArgsConstructor
@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
public class AdminOrderController extends BladeController {

    private OrderService orderService;

    /**
     * 获取订单列表（分页）
     * @param keyword 搜索关键词
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping
    @ApiOperationSupport(order = 1)
    @Operation(summary = "获取订单列表", description = "传入筛选条件")
    public R<Map<String, Object>> getAllOrders(
            Query query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            List<OrderVO> orders;

            // 根据状态筛选订单
            if (status != null && !status.isEmpty()) {
                orders = orderService.getOrdersByStatus(status);
            } else {
                orders = orderService.getAllOrders();
            }

            // 根据关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                orders = orders.stream()
                        .filter(order -> order.getOrderNo().contains(keyword) ||
                                (order.getUserName() != null && order.getUserName().contains(keyword)))
                        .collect(java.util.stream.Collectors.toList());
            }

            // 模拟分页
            int start = (query.getCurrent() - 1) * query.getSize();
            int end = Math.min(start + query.getSize(), orders.size());
            List<OrderVO> pageOrders = orders.subList(start, end);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("list", pageOrders);
            response.put("total", orders.size());
            response.put("current", query.getCurrent());
            response.put("pageSize", query.getSize());

            return R.data(response);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "获取订单详情", description = "传入id")
    public R<OrderVO> getOrderById(@PathVariable Long id) {
        try {
            OrderVO orderVO = orderService.getOrderById(id);
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据订单号查询
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/order-no/{orderNo}")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "根据订单号查询", description = "传入orderNo")
    public R<OrderVO> getOrderByOrderNo(@PathVariable String orderNo) {
        try {
            OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 订单状态
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "更新订单状态", description = "传入id和status")
    public R<OrderVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> status) {
        try {
            OrderVO orderVO = orderService.updateOrderStatus(id, status.get("status"));
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 订单发货
     * @param id 订单ID
     * @param shippingInfo 发货信息
     * @return 发货结果
     */
    @PutMapping("/{id}/ship")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "订单发货", description = "传入id、shippingMethod和trackingNo")
    public R<OrderVO> shipOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> shippingInfo) {
        try {
            OrderVO orderVO = orderService.shipOrder(id,
                    shippingInfo.get("shippingMethod"),
                    shippingInfo.get("trackingNo"));
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 取消订单
     * @param id 订单ID
     * @return 取消结果
     */
    @PutMapping("/{id}/cancel")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "取消订单", description = "传入id")
    public R<OrderVO> cancelOrder(@PathVariable Long id) {
        try {
            OrderVO orderVO = orderService.cancelOrder(id);
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 完成订单
     * @param id 订单ID
     * @return 完成结果
     */
    @PutMapping("/{id}/complete")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "完成订单", description = "传入id")
    public R<OrderVO> completeOrder(@PathVariable Long id) {
        try {
            OrderVO orderVO = orderService.completeOrder(id);
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



