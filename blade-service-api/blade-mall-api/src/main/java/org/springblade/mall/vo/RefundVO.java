package org.springblade.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款响应VO
 */
@Data
public class RefundVO {

    /**
     * 退款记录ID
     */
    private Long id;

    /**
     * 退款单号
     */
    private String refundNo;

    /**
     * 关联支付单号
     */
    private String paymentNo;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 退款金额
     */
    private BigDecimal amount;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 退款状态
     */
    private String status;

    /**
     * 退款状态文本
     */
    private String statusText;

    /**
     * 退款时间
     */
    private LocalDateTime refundedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 退款消息
     */
    private String message;
}




