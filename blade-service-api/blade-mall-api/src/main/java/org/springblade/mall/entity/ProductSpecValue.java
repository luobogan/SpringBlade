package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品规格属性值实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_spec_value")
public class ProductSpecValue extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 属性ID
     */
    private Long attributeId;

    /**
     * 属性值（如：红色、XL）
     */
    private String value;

    /**
     * 属性值图片
     */
    private String image;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}





