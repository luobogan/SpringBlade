package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_sku")
public class ProductSku extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * SKU 编码
     */
    private String skuCode;

    /**
     * SKU 规格名称（如：红色+L 码）
     */
    private String skuName;

    /**
     * 规格值 1（如：红色）
     */
    private String spec1;

    /**
     * 规格值 2（如：L）
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
     * 当前生效的促销 ID
     */
    private Long promotionId;

    /**
     * 促销价格
     */
    private BigDecimal promotionPrice;


}





