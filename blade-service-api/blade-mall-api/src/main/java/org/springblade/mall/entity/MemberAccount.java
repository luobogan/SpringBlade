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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员账户表 实体类
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_member_account")
public class MemberAccount extends MallTenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 当前会员等级 ID
     */
    @TableField("level_id")
    private Long levelId;

    /**
     * 可用积分
     */
    private Integer points;

    /**
     * 累计获得积分
     */
    @TableField("total_points")
    private Integer totalPoints;

    /**
     * 已使用积分
     */
    @TableField("used_points")
    private Integer usedPoints;

    /**
     * 成长值
     */
    private Integer growth;

    /**
     * 经验值
     */
    private Integer experience;

    /**
     * 会员开始时间
     */
    @TableField("membership_start")
    private LocalDateTime membershipStart;

    /**
     * 会员到期时间
     */
    @TableField("membership_end")
    private LocalDateTime membershipEnd;

    /**
     * 累计消费金额
     */
    @TableField("total_consumption")
    private BigDecimal totalConsumption;

    /**
     * 订单数量
     */
    @TableField("order_count")
    private Integer orderCount;

    /**
     * 最后签到日期
     */
    @TableField("last_checkin")
    private LocalDate lastCheckin;

    /**
     * 连续签到天数
     */
    @TableField("continuous_checkin_days")
    private Integer continuousCheckinDays;




}




