package org.springblade.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付方式VO
 */
@Data
public class PaymentMethodVO {

    /**
     * 支付方式代码
     */
    private String code;

    /**
     * 支付方式名称
     */
    private String name;

    /**
     * 支付方式图标
     */
    private String icon;

    /**
     * 手续费率
     */
    private BigDecimal feeRate;

    /**
     * 是否可用
     */
    private Boolean enabled;

    /**
     * 是否推荐
     */
    private Boolean recommended;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 描述
     */
    private String description;
}




