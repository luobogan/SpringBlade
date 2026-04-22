package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销商品关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_promotion_product")
public class PromotionProduct extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 促销ID
     */
    private Long promotionId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID（为空表示作用于商品所有SKU）
     */
    private Long skuId;

    /**
     * 促销价格
     */
    private BigDecimal promotionPrice;

    /**
     * 限购数量
     */
    private Integer limitQuantity;

    /**
     * 排序
     */
    private Integer sortOrder;
}





