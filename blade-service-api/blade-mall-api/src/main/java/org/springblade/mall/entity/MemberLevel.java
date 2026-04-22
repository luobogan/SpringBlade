package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员等级表 实体类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_member_level")
public class MemberLevel extends MallTenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 等级名称（如：普通会员、黄金会员）
     */
    private String name;

    /**
     * 等级值（1-9，值越大等级越高）
     */
    @TableField("level_value")
    private Integer levelValue;

    /**
     * 最低成长值要求
     */
    @TableField("min_growth")
    private Integer minGrowth;

    /**
     * 最高成长值（NULL 表示无上限）
     */
    @TableField("max_growth")
    private Integer maxGrowth;

    /**
     * 最低经验值要求
     */
    @TableField("min_experience")
    private Integer minExperience;

    /**
     * 折扣率（0.01-1.00）
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 等级图标
     */
    private String icon;

    /**
     * 权益描述（JSON 格式）
     */
    private String benefits;

    /**
     * 购买价格（0 表示免费）
     */
    private BigDecimal price;

    /**
     * 有效期天数（0 表示永久）
     */
    @TableField("duration_days")
    private Integer durationDays;

    /**
     * 排序权重
     */
    @TableField("sort_order")
    private Integer sortOrder;


}




