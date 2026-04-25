package org.springblade.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WechatPayRequestDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    private String tradeType = "JSAPI";

    private String description;

    private String openid;
}
