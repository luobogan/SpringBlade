package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员权益表 实体类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_member_benefit")
public class MemberBenefit extends MallTenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会员等级 ID
     */
    @TableField("level_id")
    private Long levelId;

    /**
     * 权益名称
     */
    private String name;

    /**
     * 权益类型：1 折扣，2 包邮，3 专属客服，4 生日礼，5 退换货
     */
    private Integer type;

    /**
     * 权益值（如折扣率、金额等）
     */
    private String value;

    /**
     * 权益描述
     */
    private String description;

    /**
     * 权益图标
     */
    private String icon;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;


}




