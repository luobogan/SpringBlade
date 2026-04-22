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
 * <p>
 * 会员成长值日志表 实体类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_member_growth_log")
public class MemberGrowthLog extends MallTenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 成长值变动（正数增加，负数减少）
     */
    @TableField("growth_value")
    private Integer growthValue;

    /**
     * 变动类型：1 消费，2 签到，3 活动，4 系统调整
     */
    private Integer type;

    /**
     * 来源类型：ORDER, CHECKIN, ACTIVITY, SYSTEM
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源 ID（如订单 ID）
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 变动前成长值
     */
    @TableField("before_growth")
    private Integer beforeGrowth;

    /**
     * 变动后成长值
     */
    @TableField("after_growth")
    private Integer afterGrowth;

    /**
     * 描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;


}





