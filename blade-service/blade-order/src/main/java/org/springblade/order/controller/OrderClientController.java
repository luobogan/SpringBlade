package org.springblade.order.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.order.feign.IOrderClient;
import org.springblade.order.service.IOrderService;
import org.springblade.order.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@AllArgsConstructor
public class OrderClientController implements IOrderClient {

    private final IOrderService orderService;

    @Override
    @GetMapping(GET_BY_ORDER_NO)
    public R<OrderVO> getByOrderNo(@RequestParam("orderNo") String orderNo) {
        try {
            OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
            return R.data(orderVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @PostMapping(UPDATE_PAY_STATUS)
    public R<Boolean> updatePayStatus(@RequestParam("orderNo") String orderNo,
                                      @RequestParam("paymentNo") String paymentNo) {
        try {
            OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
            orderService.payOrder(orderVO.getId(), orderVO.getPaymentMethod(), paymentNo);
            return R.data(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    @PostMapping(CANCEL_ORDER)
    public R<Boolean> cancelOrder(@RequestParam("orderNo") String orderNo) {
        try {
            OrderVO orderVO = orderService.getOrderByOrderNo(orderNo);
            orderService.cancelOrder(orderVO.getId());
            return R.data(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
