package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_refund")
public class Refund extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

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
     * 用户ID
     */
    private Long userId;

    /**
     * 退款金额
     */
    private BigDecimal amount;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 退款状态：PENDING待处理，PROCESSING处理中，SUCCESS退款成功，FAILED退款失败
     */
    @TableField("refund_status")
    private String refundStatus;

    /**
     * 退款时间
     */
    private LocalDateTime refundedAt;

    /**
     * 第三方退款单号
     */
    private String refundTransactionId;

    /**
     * 退款回调数据
     */
    private String callbackData;

    /**
     * 退款失败原因
     */
    private String failReason;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditedAt;

    /**
     * 审核备注
     */
    private String auditRemark;
}





