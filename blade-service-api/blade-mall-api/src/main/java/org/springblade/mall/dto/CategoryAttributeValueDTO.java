package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类属性值DTO
 */
@Data
public class CategoryAttributeValueDTO {

    /**
     * 属性值ID（编辑时使用）
     */
    private Long id;

    /**
     * 属性ID
     */
    @NotNull(message = "属性ID不能为空")
    private Long attributeId;

    /**
     * 属性值
     */
    @NotBlank(message = "属性值不能为空")
    private String value;

    /**
     * 排序
     */
    private Integer sortOrder;
}




