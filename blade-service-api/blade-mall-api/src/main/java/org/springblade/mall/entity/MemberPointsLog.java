package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员积分日志表 实体类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_member_points_log")
public class MemberPointsLog extends MallTenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 积分变动（正数获得，负数消费）
     */
    @TableField("points_value")
    private Integer pointsValue;

    /**
     * 变动类型：1 获得，2 消费，3 过期，4 系统调整
     */
    private Integer type;

    /**
     * 子类型：1 购物，2 签到，3 活动，4 兑换，5 退款
     */
    @TableField("sub_type")
    private Integer subType;

    /**
     * 来源类型：ORDER, CHECKIN, ACTIVITY, EXCHANGE, SYSTEM
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源 ID（如订单 ID）
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 变动前积分
     */
    @TableField("before_points")
    private Integer beforePoints;

    /**
     * 变动后积分
     */
    @TableField("after_points")
    private Integer afterPoints;

    /**
     * 过期日期（仅获得类型）
     */
    @TableField("expire_date")
    private LocalDate expireDate;

    /**
     * 描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;


}





