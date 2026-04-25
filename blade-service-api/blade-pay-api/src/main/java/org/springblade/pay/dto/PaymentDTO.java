package org.springblade.pay.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDTO {
    private String paymentMethod;
    private String paymentNo;
    private BigDecimal amount;
    private String payPassword;
    private String orderNo;
}
