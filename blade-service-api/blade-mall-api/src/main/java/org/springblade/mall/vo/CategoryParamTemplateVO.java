package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类参数模板VO
 */
@Data
public class CategoryParamTemplateVO {
    
    /**
     * 模板ID
     */
    private Long id;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 参数名称
     */
    private String name;
    
    /**
     * 参数类型：1-单选，2-多选
     */
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
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}




