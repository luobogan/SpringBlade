package org.springblade.mall.dto;

import lombok.Data;

/**
 * 商品属性值DTO
 */
@Data
public class ProductAttributeValueDTO {

    /**
     * 属性ID
     */
    private Long attributeId;

    /**
     * 属性值
     */
    private String value;
}




