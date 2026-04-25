package org.springblade.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundDTO {

    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "退款原因不能为空")
    private String reason;

    private String refundType = "FULL";
}
