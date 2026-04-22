package org.springblade.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品关联VO
 */
@Data
public class ProductRelationVO {

    /**
     * 关联ID
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
     * 关联商品信息
     */
    private ProductVO relatedProduct;

    /**
     * 关联类型：1相关商品，2搭配商品，3推荐商品
     */
    private Integer type;

    /**
     * 关联类型文本
     */
    private String typeText;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}




