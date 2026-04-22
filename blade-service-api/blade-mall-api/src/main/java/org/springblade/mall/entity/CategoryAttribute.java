package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类属性实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_category_attribute")
public class CategoryAttribute extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

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
     * 属性值列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<CategoryAttributeValue> values;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}





