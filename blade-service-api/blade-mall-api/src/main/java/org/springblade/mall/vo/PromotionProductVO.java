package org.springblade.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 促销商品关联VO
 */
@Data
public class PromotionProductVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 促销ID
     */
    private Long promotionId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品信息
     */
    private ProductVO product;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * SKU信息
     */
    private ProductSkuVO sku;

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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}




