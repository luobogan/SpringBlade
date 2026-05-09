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
 * 商品分类实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_category")
public class Category extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类级别
     */
    private Integer level;

    /**
     * 排序顺序
     */
    @TableField("sort")
    private Integer sortOrder;

    /**
     * 分类图标ID（关联ImageFile表主键）
     */
    @TableField("icon_id")
    private Long iconId;

    /**
     * 分类图片ID（关联ImageFile表主键）
     */
    @TableField("image_id")
    private Long imageId;

    /**
     * 分类Banner图片ID（关联ImageFile表主键）
     */
    @TableField("banner_id")
    private Long bannerId;

    /**
     * 租户ID
     */
    private String tenantId;
}
