package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类参数模板实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_category_param_template")
public class CategoryParamTemplate extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

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
}





