package org.springblade.pay.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentVO {
    private Long id;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private String paymentTime;
    private String expireTime;
    private String remark;
    private String createdAt;
    private String updatedAt;
}
