package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类属性VO
 */
@Data
public class CategoryAttributeVO {

    /**
     * 属性ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性类型：1单选，2多选，3文本输入，4数字输入，5日期
     */
    private Integer type;

    /**
     * 属性类型文本
     */
    private String typeText;

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
    private List<CategoryAttributeValueVO> values;
}




