package org.springblade.mall.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级 VO
 */
@Data
@EqualsAndHashCode(callSuper = false)

public class MemberLevelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会员等级 ID
     */
    private Long id;

    /**
     * 等级名称
     */
    private String name;

    /**
     * 等级值
     */
    private Integer levelValue;

    /**
     * 最低成长值
     */
    private Integer minGrowth;

    /**
     * 最高成长值
     */
    private Integer maxGrowth;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 等级图标
     */
    private String icon;

    /**
     * 权益描述（JSON）
     */
    private String benefits;

    /**
     * 购买价格
     */
    private BigDecimal price;

    /**
     * 有效期天数
     */
    private Integer durationDays;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 权益列表（解析后的）
     */
    private List<String> benefitList;

}





