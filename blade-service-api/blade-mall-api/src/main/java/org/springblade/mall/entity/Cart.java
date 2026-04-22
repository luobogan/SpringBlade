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
 * 购物车实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_cart")
public class Cart extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称（冗余字段，方便查询）
     */
    private String productName;

    /**
     * 商品图片（冗余字段，方便查询）
     */
    private String productImage;

    /**
     * 商品价格（冗余字段，方便查询）
     */
    private BigDecimal productPrice;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 是否选中（0:未选中, 1:已选中）
     */
    private Integer selected;

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





