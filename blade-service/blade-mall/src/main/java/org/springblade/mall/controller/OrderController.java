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

import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.dto.OrderDTO;
import org.springblade.mall.dto.PaymentDTO;
import org.springblade.mall.service.OrderService;
import org.springblade.mall.vo.OrderVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/order")
@AllArgsConstructor
public class OrderController extends BladeController {

    private OrderService orderService;

    /**
     * 创建订单
     */
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

    /**
     * 获取订单详情
     */
    @GetMapping("/detail")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "查看详情", description = "传入id")
    public R<OrderVO> getOrderById(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.getOrderById(id);
        return R.data(orderVO);
    }

    /**
     * 根据订单号获取订单
     */
    @GetMapping("/detail-by-no")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "根据订单号查看详情", description = "传入orderNo")
    public R<OrderVO> getOrderByOrderNo(@Parameter(description = "订单号", required = true) @RequestParam String orderNo) {
        OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
        return R.data(orderVO);
    }

    /**
     * 获取当前用户订单列表
     */
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

    /**
     * 获取所有订单（管理员）
     */
    @GetMapping("/list")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "获取所有订单", description = "无需参数")
    public R<List<OrderVO>> getAllOrders() {
        List<OrderVO> orders = orderService.getAllOrders();
        return R.data(orders);
    }

    /**
     * 根据状态获取订单
     */
    @GetMapping("/list-by-status")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "根据状态获取订单", description = "传入status")
    public R<List<OrderVO>> getOrdersByStatus(@Parameter(description = "订单状态", required = true) @RequestParam String status) {
        List<OrderVO> orders = orderService.getOrdersByStatus(status);
        return R.data(orders);
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/update-status")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "更新订单状态", description = "传入id和status")
    public R<OrderVO> updateOrderStatus(@Parameter(description = "订单ID", required = true) @RequestParam Long id, 
                                        @Parameter(description = "订单状态", required = true) @RequestParam String status) {
        OrderVO orderVO = orderService.updateOrderStatus(id, status);
        return R.data(orderVO, "订单状态更新成功");
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    @ApiOperationSupport(order = 8)
    @Operation(summary = "支付订单", description = "传入id和paymentDTO")
    public R<OrderVO> payOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id, 
                               @Valid @RequestBody PaymentDTO paymentDTO) {
        OrderVO orderVO = orderService.payOrder(id, paymentDTO.getPaymentMethod(), paymentDTO.getPaymentNo());
        return R.data(orderVO, "订单支付成功");
    }

    /**
     * 发货
     */
    @PostMapping("/ship")
    @ApiOperationSupport(order = 9)
    @Operation(summary = "发货", description = "传入id、shippingMethod和trackingNo")
    public R<OrderVO> shipOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id, 
                                @Parameter(description = "配送方式", required = true) @RequestParam String shippingMethod, 
                                @Parameter(description = "物流单号", required = true) @RequestParam String trackingNo) {
        OrderVO orderVO = orderService.shipOrder(id, shippingMethod, trackingNo);
        return R.data(orderVO, "订单发货成功");
    }

    /**
     * 完成订单
     */
    @PostMapping("/complete")
    @ApiOperationSupport(order = 10)
    @Operation(summary = "完成订单", description = "传入id")
    public R<OrderVO> completeOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.completeOrder(id);
        return R.data(orderVO, "订单完成成功");
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    @ApiOperationSupport(order = 11)
    @Operation(summary = "取消订单", description = "传入id")
    public R<OrderVO> cancelOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.cancelOrder(id);
        return R.data(orderVO, "订单取消成功");
    }

    /**
     * 删除订单
     */
    @PostMapping("/remove")
    @ApiOperationSupport(order = 12)
    @Operation(summary = "删除订单", description = "传入id")
    public R<?> deleteOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        orderService.deleteOrder(id);
        return R.success("订单删除成功");
    }

    /**
     * 评价订单
     */
    @PostMapping("/review")
    @ApiOperationSupport(order = 13)
    @Operation(summary = "评价订单", description = "传入id")
    public R<OrderVO> reviewOrder(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.reviewOrder(id);
        return R.data(orderVO, "订单评价成功");
    }

    /**
     * 申请退换/售后
     */
    @PostMapping("/apply-return")
    @ApiOperationSupport(order = 14)
    @Operation(summary = "申请退换/售后", description = "传入id")
    public R<OrderVO> applyReturn(@Parameter(description = "订单ID", required = true) @RequestParam Long id) {
        OrderVO orderVO = orderService.applyReturn(id);
        return R.data(orderVO, "申请退换/售后成功");
    }

    /**
     * 获取当前用户的待付款订单
     */
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

    /**
     * 获取当前用户的待收货订单
     */
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

    /**
     * 获取当前用户的待评价订单
     */
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

    /**
     * 获取当前用户的退换/售后订单
     */
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

    /**
     * 获取当前用户的全部订单
     */
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

    /**
     * 获取当前用户的订单数量统计
     */
    @GetMapping("/user/counts")
    @ApiOperationSupport(order = 20)
    @Operation(summary = "获取当前用户的订单数量统计", description = "无需参数")
    public R<OrderService.OrderCountStats> getOrderCounts(BladeUser user) {
        if (user == null) {
            return R.fail("用户未登录");
        }
        OrderService.OrderCountStats stats = orderService.getOrderCountStats(user.getUserId());
        return R.data(stats);
    }
}


