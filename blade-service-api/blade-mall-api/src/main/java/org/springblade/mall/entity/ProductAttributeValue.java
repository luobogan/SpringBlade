package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.time.LocalDateTime;

/**
 * 商品属性值实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_attribute_value")
public class ProductAttributeValue extends MallTenantEntity {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 属性ID
     */
    private Long attributeId;

    /**
     * 属性值
     */
    private String value;
}





