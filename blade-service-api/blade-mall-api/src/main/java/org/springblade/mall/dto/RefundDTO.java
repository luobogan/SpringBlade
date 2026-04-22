package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
public class RefundDTO {

    /**
     * 支付单号
     */
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    /**
     * 退款类型：FULL全额退款，PARTIAL部分退款
     */
    private String refundType = "FULL";
}




