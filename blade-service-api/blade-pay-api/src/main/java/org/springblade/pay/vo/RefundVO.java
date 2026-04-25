package org.springblade.pay.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundVO {
    private Long id;
    private String refundNo;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String reason;
    private String refundStatus;
    private String refundedAt;
    private String refundTransactionId;
    private String failReason;
    private String createdAt;
    private String updatedAt;
}
