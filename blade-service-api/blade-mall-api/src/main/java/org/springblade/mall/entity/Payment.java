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
 * 支付记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_payment")
public class Payment extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 支付单号
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
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式：BALANCE余额支付，WECHAT微信支付，ALIPAY支付宝
     */
    private String paymentMethod;

    /**
     * 支付状态：PENDING待支付，PROCESSING支付中，SUCCESS支付成功，FAILED支付失败，REFUNDED已退款
     */
    @TableField("status")
    private String paymentStatus;

    /**
     * 第三方交易号（微信/支付宝交易号）
     */
    @TableField("transaction_id")
    private String transactionId;

    /**
     * 支付时间
     */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime paymentCreateTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime paymentUpdateTime;
}





