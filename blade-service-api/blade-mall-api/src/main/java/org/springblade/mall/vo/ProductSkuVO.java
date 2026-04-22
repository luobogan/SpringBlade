package org.springblade.mall.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU VO
 */
@Data
public class ProductSkuVO {

    /**
     * SKU ID
     */
    private Long id;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * SKU 编码
     */
    private String skuCode;

    /**
     * SKU 规格名称
     */
    private String skuName;

    /**
     * 规格值 1
     */
    private String spec1;

    /**
     * 规格值 2
     */
    private String spec2;

    /**
     * 规格值 3
     */
    private String spec3;

    /**
     * 规格值 4（如：网络类型 5G）
     */
    private String spec4;

    /**
     * SKU 价格
     */
    private BigDecimal price;

    /**
     * SKU 原价
     */
    private BigDecimal originalPrice;

    /**
     * SKU 库存
     */
    private Integer stock;

    /**
     * SKU 图片
     */
    private String image;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 当前生效的促销 ID
     */
    private Long promotionId;

    /**
     * 促销价格
     */
    private BigDecimal promotionPrice;
}




