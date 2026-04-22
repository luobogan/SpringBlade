package org.springblade.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品规格属性值VO
 */
@Data
public class ProductSpecValueVO {

    /**
     * 属性值ID
     */
    private Long id;

    /**
     * 属性ID
     */
    private Long attributeId;

    /**
     * 属性值
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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}




