package org.springblade.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 促销商品关联DTO
 */
@Data
public class PromotionProductDTO {

    /**
     * ID（编辑时使用）
     */
    private Long id;

    /**
     * 促销ID
     */
    @NotNull(message = "促销ID不能为空")
    private Long promotionId;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
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




