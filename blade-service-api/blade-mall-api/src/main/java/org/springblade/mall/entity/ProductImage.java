package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;

/**
 * 商品图片实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_image")
public class ProductImage extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 图片地址
     */
    private String imageUrl;

    /**
     * 排序权重
     */
    private Integer sort;
}





