package org.springblade.mall.dto;

import lombok.Data;
import java.util.List;

/**
 * 商品规格属性DTO
 */
@Data
public class ProductSpecAttributeDTO {

    /**
     * 属性ID（编辑时使用）
     */
    private Long id;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 属性值列表
     */
    private List<ProductSpecValueDTO> values;
}




