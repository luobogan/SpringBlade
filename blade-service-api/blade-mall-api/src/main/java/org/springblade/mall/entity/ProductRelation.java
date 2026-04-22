package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_relation")
public class ProductRelation extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主商品ID
     */
    private Long productId;

    /**
     * 关联商品ID
     */
    private Long relatedProductId;

    /**
     * 关联类型：1相关商品，2搭配商品，3推荐商品
     */
    private Integer type;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}





