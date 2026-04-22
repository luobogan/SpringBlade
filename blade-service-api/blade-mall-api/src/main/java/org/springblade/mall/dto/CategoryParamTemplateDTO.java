package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类参数模板DTO
 */
@Data
public class CategoryParamTemplateDTO {
    
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    /**
     * 参数名称
     */
    @NotBlank(message = "参数名称不能为空")
    private String name;
    
    /**
     * 参数类型：1-单选，2-多选
     */
    @NotNull(message = "参数类型不能为空")
    private Integer type;
    
    /**
     * 参数值
     */
    private String value;
    
    /**
     * 是否必填
     */
    private Integer isRequired;
    
    /**
     * 是否可搜索
     */
    private Integer isSearchable;
    
    /**
     * 排序
     */
    private Integer sortOrder;
}




