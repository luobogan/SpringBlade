package org.springblade.pay.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentMethodVO {

    private String code;

    private String name;

    private String icon;

    private BigDecimal feeRate;

    private Boolean enabled;

    private Boolean recommended;

    private Integer sort;

    private String description;
}
