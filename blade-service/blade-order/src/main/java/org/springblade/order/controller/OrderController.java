package org.springblade.order.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.order.dto.OrderDTO;
import org.springblade.order.service.IOrderService;
import org.springblade.order.vo.OrderCountStats;
import org.springblade.order.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@AllArgsConstructor
public class OrderController extends BladeController {

    private final IOrderService orderService;

    @PostMapping("/submit")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "创建订单", description = "传入orderDTO")
    public R<OrderVO> createOrder(@Valid @RequestBody OrderDTO orderDTO, BladeUser user) {
        if (user != null) {
            orderDTO.setUserId(user.getUserId());
        }
        OrderVO orderVO = orderService.createOrder(orderDTO);
        return R.data(orderVO, "订单创建成功");
    }

    @GetMapping("/detail")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "查看详情", description = "传入id")
    public R<OrderVO> getOrderById(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.getOrderById(id);
        return R.data(orderVO);
    }

    @GetMapping("/detail-by-no")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "根据订单号查看详情", description = "传入orderNo")
    public R<OrderVO> getOrderByOrderNo(@Parameter(description = "订单号", required = true) @RequestParam String orderNo) {
        OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
        return R.data(orderVO);
    }

    @GetMapping("/user/list")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "获取当前用户订单列表", description = "无需参数")
    public R<List<OrderVO>> getCurrentUserOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserId(user.getUserId());
        return R.data(orders);
    }

    @GetMapping("/list")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "获取所有订单", description = "无需参数")
    public R<List<OrderVO>> getAllOrders() {
        List<OrderVO> orders = orderService.getAllOrders();
        return R.data(orders);
    }

    @GetMapping("/list-by-status")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "根据状态获取订单", description = "传入status")
    public R<List<OrderVO>> getOrdersByStatus(@Parameter(description = "订单状态", required = true) @RequestParam String status) {
        List<OrderVO> orders = orderService.getOrdersByStatus(status);
        return R.data(orders);
    }

    @PostMapping("/update-status")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "更新订单状态", description = "传入id和status")
    public R<OrderVO> updateOrderStatus(@Parameter(description = "订单ID", required = true) @RequestParam Long id,
                                        @Parameter(description = "订单状态", required = true) @RequestParam String status) {
        OrderVO orderVO = orderService.updateOrderStatus(id, status);
        return R.data(orderVO, "订单状态更新成功");
    }

    @PostMapping("/pay")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "支付订单", description = "传入id、paymentMethod和paymentNo")
    public R<OrderVO> payOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id,
                               @Parameter(description = "支付方式", required = true) @RequestParam String paymentMethod,
                               @Parameter(description = "支付单号", required = true) @RequestParam String paymentNo) {
        OrderVO orderVO = orderService.payOrder(id, paymentMethod, paymentNo);
        return R.data(orderVO, "订单支付成功");
    }

    @PostMapping("/ship")
    @ApiOperationSupport(order = 9)
    @Operation(summary = "发货", description = "传入id、shippingMethod和trackingNo")
    public R<OrderVO> shipOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id,
                                @Parameter(description = "配送方式", required = true) @RequestParam String shippingMethod,
                                @Parameter(description = "物流单号", required = true) @RequestParam String trackingNo) {
        OrderVO orderVO = orderService.shipOrder(id, shippingMethod, trackingNo);
        return R.data(orderVO, "订单发货成功");
    }

    @PostMapping("/complete")
    @ApiOperationSupport(order = 10)
    @Operation(summary = "完成订单", description = "传入id")
    public R<OrderVO> completeOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.completeOrder(id);
        return R.data(orderVO, "订单完成成功");
    }

    @PostMapping("/cancel")
    @ApiOperationSupport(order = 11)
    @Operation(summary = "取消订单", description = "传入id")
    public R<OrderVO> cancelOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.cancelOrder(id);
        return R.data(orderVO, "订单取消成功");
    }

    @PostMapping("/remove")
    @ApiOperationSupport(order = 12)
    @Operation(summary = "删除订单", description = "传入id")
    public R<?> deleteOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        orderService.deleteOrder(id);
        return R.success("订单删除成功");
    }

    @PostMapping("/review")
    @ApiOperationSupport(order = 13)
    @Operation(summary = "评价订单", description = "传入id")
    public R<OrderVO> reviewOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.reviewOrder(id);
        return R.data(orderVO, "订单评价成功");
    }

    @PostMapping("/apply-return")
    @ApiOperationSupport(order = 14)
    @Operation(summary = "申请退换/售后", description = "传入id")
    public R<OrderVO> applyReturn(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.applyReturn(id);
        return R.data(orderVO, "申请退换/售后成功");
    }

    @GetMapping("/user/pending-payment")
    @ApiOperationSupport(order = 15)
    @Operation(summary = "获取当前用户的待付款订单", description = "无需参数")
    public R<List<OrderVO>> getPendingPaymentOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserIdAndStatus(user.getUserId(), "PENDING");
        return R.data(orders);
    }

    @GetMapping("/user/pending-receipt")
    @ApiOperationSupport(order = 16)
    @Operation(summary = "获取当前用户的待收货订单", description = "无需参数")
    public R<List<OrderVO>> getPendingReceiptOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserIdAndStatuses(user.getUserId(), List.of("PAID", "SHIPPED"));
        return R.data(orders);
    }

    @GetMapping("/user/pending-review")
    @ApiOperationSupport(order = 17)
    @Operation(summary = "获取当前用户的待评价订单", description = "无需参数")
    public R<List<OrderVO>> getPendingReviewOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserIdAndStatus(user.getUserId(), "PENDING_REVIEW");
        return R.data(orders);
    }

    @GetMapping("/user/return-after-sales")
    @ApiOperationSupport(order = 18)
    @Operation(summary = "获取当前用户的退换/售后订单", description = "无需参数")
    public R<List<OrderVO>> getReturnAfterSalesOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserIdAndStatus(user.getUserId(), "RETURN_AFTER_SALES");
        return R.data(orders);
    }

    @GetMapping("/user/all")
    @ApiOperationSupport(order = 19)
    @Operation(summary = "获取当前用户的全部订单", description = "无需参数")
    public R<List<OrderVO>> getAllUserOrders(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        List<OrderVO> orders = orderService.getOrdersByUserId(user.getUserId());
        return R.data(orders);
    }

    @GetMapping("/user/counts")
    @ApiOperationSupport(order = 20)
    @Operation(summary = "获取当前用户的订单数量统计", description = "无需参数")
    public R<OrderCountStats> getOrderCounts(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        OrderCountStats stats = orderService.getOrderCountStats(user.getUserId());
        return R.data(stats);
    }
}
