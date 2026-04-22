package org.springblade.mall.dto;

import lombok.Data;

/**
 * 商品规格属性值DTO
 */
@Data
public class ProductSpecValueDTO {

    /**
     * 属性值ID（编辑时使用）
     */
    private Long id;

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
}




