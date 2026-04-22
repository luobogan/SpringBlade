package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品规格属性实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_spec_attribute")
public class ProductSpecAttribute extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 属性名称（如：颜色、尺码）
     */
    private String name;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}





