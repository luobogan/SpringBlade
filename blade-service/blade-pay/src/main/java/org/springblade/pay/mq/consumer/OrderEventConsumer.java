package org.springblade.pay.mq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.pay.constant.PayConstant;
import org.springblade.pay.service.IPayService;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final IPayService payService;

    public OrderEventConsumer(IPayService payService) {
        this.payService = payService;
    }

    public void handleOrderCancel(String orderNo, String paymentNo) {
        log.info("收到订单取消事件: orderNo={}, paymentNo={}", orderNo, paymentNo);
    }

    public void handleOrderRefund(String orderNo, String paymentNo, String reason) {
        log.info("收到订单退款事件: orderNo={}, paymentNo={}, reason={}", orderNo, paymentNo, reason);
    }
}
