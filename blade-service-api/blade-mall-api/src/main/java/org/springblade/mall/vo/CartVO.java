package org.springblade.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车 VO
 */
@Data
public class CartVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 商品价格
     */
    private BigDecimal productPrice;

    /**
     * 商品当前价格
     */
    private BigDecimal productCurrentPrice;

    /**
     * 商品会员价格
     */
    private BigDecimal productMemberPrice;

    /**
     * 商品当前会员价格
     */
    private BigDecimal productCurrentMemberPrice;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 是否选中
     */
    private Integer selected;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

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




