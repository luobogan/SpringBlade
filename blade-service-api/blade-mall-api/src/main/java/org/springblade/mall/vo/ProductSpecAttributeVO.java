package org.springblade.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品规格属性VO
 */
@Data
public class ProductSpecAttributeVO {

    /**
     * 属性ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 属性值列表
     */
    private List<ProductSpecValueVO> values;
}




