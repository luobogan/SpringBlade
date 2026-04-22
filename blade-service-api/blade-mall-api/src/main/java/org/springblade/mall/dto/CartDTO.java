package org.springblade.mall.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

/**
 * 购物车 DTO
 */
@Data
public class CartDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品数量
     */
    @NotNull(message = "商品数量不能为空")
    @Positive(message = "商品数量必须大于0")
    private Integer quantity;

    /**
     * 选中的规格（JSON格式）
     */
    private String selectedSpecs;

    /**
     * 选中的颜色
     */
    private String selectedColor;

    /**
     * 选中的尺寸
     */
    private String selectedSize;

    /**
     * SKU ID
     */
    private Long skuId;
}




