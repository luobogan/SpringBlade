package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类属性DTO
 */
@Data
public class CategoryAttributeDTO {

    /**
     * 属性ID（编辑时使用）
     */
    private Long id;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 属性名称
     */
    @NotBlank(message = "属性名称不能为空")
    private String name;

    /**
     * 属性类型：1单选，2多选，3文本输入，4数字输入，5日期
     */
    @NotNull(message = "属性类型不能为空")
    private Integer type;

    /**
     * 是否必填：0否，1是
     */
    private Integer isRequired;

    /**
     * 是否可用于搜索筛选：0否，1是
     */
    private Integer isSearchable;

    /**
     * 排序
     */
    private Integer sortOrder;
}




