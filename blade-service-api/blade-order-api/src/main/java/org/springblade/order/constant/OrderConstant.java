package org.springblade.order.constant;

public class OrderConstant {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_RETURN_AFTER_SALES = "RETURN_AFTER_SALES";

    public static final int PAYMENT_TIMEOUT_MINUTES = 30;

    public static final String TOPIC_ORDER_EVENTS = "order-events";

    public static final String EVENT_ORDER_CREATED = "OrderCreatedEvent";
    public static final String EVENT_ORDER_CANCELLED = "OrderCancelledEvent";
    public static final String EVENT_ORDER_COMPLETED = "OrderCompletedEvent";
}
