package org.springblade.mall.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员账户 VO
 */
@Data
@EqualsAndHashCode(callSuper = false)

public class MemberAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会员账户 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 会员等级 ID
     */
    private Long levelId;

    /**
     * 会员等级名称
     */
    private String levelName;

    /**
     * 可用积分
     */
    private Integer points;

    /**
     * 累计获得积分
     */
    private Integer totalPoints;

    /**
     * 已使用积分
     */
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
    private LocalDateTime membershipStart;

    /**
     * 会员到期时间
     */
    private LocalDateTime membershipEnd;

    /**
     * 会员状态：0 非会员，1 会员，2 过期
     */
    private Integer status;

    /**
     * 会员状态描述
     */
    private String statusDesc;

    /**
     * 累计消费金额
     */
    private BigDecimal totalConsumption;

    /**
     * 订单数量
     */
    private Integer orderCount;

    /**
     * 最后签到日期
     */
    private LocalDate lastCheckin;

    /**
     * 连续签到天数
     */
    private Integer continuousCheckinDays;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 权益列表
     */
    private String benefits;

}





