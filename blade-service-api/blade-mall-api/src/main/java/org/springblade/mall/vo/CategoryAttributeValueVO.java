package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类属性值VO
 */
@Data
public class CategoryAttributeValueVO {

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
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}




