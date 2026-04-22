package org.springblade.mall.dto;

import lombok.Data;

/**
 * 商品关联DTO
 */
@Data
public class ProductRelationDTO {

    /**
     * 关联ID（编辑时使用）
     */
    private Long id;

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




