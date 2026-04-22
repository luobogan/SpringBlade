package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类属性值实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_category_attribute_value")
public class CategoryAttributeValue extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 属性ID
     */
    private Long attributeId;

    /**
     * 属性值
     */
    private String value;

    /**
     * 排序
     */
    private Integer sortOrder;


}





