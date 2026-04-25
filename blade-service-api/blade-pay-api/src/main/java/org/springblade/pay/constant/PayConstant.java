package org.springblade.pay.constant;

public class PayConstant {

    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_ALIPAY = "ALIPAY";
    public static final String CHANNEL_BALANCE = "BALANCE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_REFUNDED = "REFUNDED";

    public static final String TOPIC_PAY_EVENTS = "pay-events";
    public static final String TOPIC_ORDER_EVENTS = "order-events";

    public static final String EVENT_PAYMENT_SUCCESS = "PaymentSuccessEvent";
    public static final String EVENT_PAYMENT_FAILED = "PaymentFailedEvent";
    public static final String EVENT_REFUND_SUCCESS = "RefundSuccessEvent";
    public static final String EVENT_REFUND_FAILED = "RefundFailedEvent";
}
