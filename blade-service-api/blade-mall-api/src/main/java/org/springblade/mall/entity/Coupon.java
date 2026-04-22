package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_coupon")
public class Coupon extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券码
     */
    private String code;

    /**
     * 优惠券类型：1固定金额，2百分比
     */
    private Integer type;

    /**
     * 优惠金额或百分比
     */
    private BigDecimal value;

    /**
     * 最低消费金额
     */
    private BigDecimal minSpend;

    /**
     * 最大优惠金额（针对百分比类型）
     */
    private BigDecimal maxDiscount;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总发行量（0表示无限制）
     */
    private Integer totalQuantity;

    /**
     * 已使用数量
     */
    private Integer usedQuantity;

    /**
     * 每用户限领数量
     */
    private Integer perUserLimit;


}




