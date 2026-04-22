package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;

/**
 * 商品尺寸实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_size")
public class ProductSize extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 尺寸名称
     */
    private String sizeName;

    /**
     * 该尺寸库存
     */
    private Integer stock;
}





