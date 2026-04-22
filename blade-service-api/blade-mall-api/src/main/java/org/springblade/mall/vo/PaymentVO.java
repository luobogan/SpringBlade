package org.springblade.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应VO
 */
@Data
public class PaymentVO {

    /**
     * 支付记录ID
     */
    private Long id;

    /**
     * 支付单号
     */
    private String paymentNo;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付方式名称
     */
    private String paymentMethodName;

    /**
     * 支付状态
     */
    private String status;

    /**
     * 支付状态文本
     */
    private String statusText;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付参数（用于跳转第三方支付）
     */
    private PaymentParamsVO paymentParams;
}




