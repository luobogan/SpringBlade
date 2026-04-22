package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;

/**
 * 商品颜色实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_color")
public class ProductColor extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 颜色名称
     */
    private String colorName;

    /**
     * 颜色值（如：#FF0000）
     */
    private String colorValue;

    /**
     * 该颜色库存
     */
    private Integer stock;
}





