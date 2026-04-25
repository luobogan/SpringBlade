package org.springblade.order.mq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.order.constant.OrderConstant;
import org.springblade.order.service.IOrderService;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final IOrderService orderService;

    public PaymentEventConsumer(IOrderService orderService) {
        this.orderService = orderService;
    }

    public void handlePaymentSuccess(String orderNo, String paymentNo) {
        log.info("收到支付成功事件: orderNo={}, paymentNo={}", orderNo, paymentNo);
        try {
            orderService.payOrder(null, "WECHAT", paymentNo);
            log.info("订单支付状态更新成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("更新订单支付状态失败: orderNo={}", orderNo, e);
            throw e;
        }
    }

    public void handlePaymentFailed(String orderNo, String reason) {
        log.info("收到支付失败事件: orderNo={}, reason={}", orderNo, reason);
    }

    public void handleRefundSuccess(String orderNo, String refundNo) {
        log.info("收到退款成功事件: orderNo={}, refundNo={}", orderNo, refundNo);
        try {
            orderService.applyReturn(null);
            log.info("订单退款状态更新成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("更新订单退款状态失败: orderNo={}", orderNo, e);
            throw e;
        }
    }
}
