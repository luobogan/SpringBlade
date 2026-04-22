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
 * 订单实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class Order extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal actualAmount;

    /**
     * 使用的优惠券ID
     */
    private Long couponId;

    /**
     * 优惠券金额
     */
    private BigDecimal couponAmount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付单号
     */
    private String paymentNo;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 配送方式
     */
    private String shippingMethod;

    /**
     * 配送地址ID
     */
    private Long shippingAddressId;

    /**
     * 物流单号
     */
    private String trackingNo;

    /**
     * 发货时间
     */
    private LocalDateTime shippingTime;

    /**
     * 确认收货时间
     */
    private LocalDateTime confirmTime;



    /**
     * 订单状态：PENDING待支付，PAID已支付，SHIPPED已发货，COMPLETED已完成，CANCELLED已取消，PENDING_REVIEW待评价，RETURN_AFTER_SALES退换/售后
     */
    private String orderStatus;

    /**
     * 订单备注
     */
    private String remark;


}




