package org.springblade.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员信息 VO
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 会员等级 ID
     */
    private Long levelId;

    /**
     * 会员等级名称
     */
    private String levelName;

    /**
     * 会员等级值
     */
    private Integer levelValue;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 成长值
     */
    private Integer growth;

    /**
     * 经验值
     */
    private Integer experience;

    /**
     * 累计消费金额
     */
    private java.math.BigDecimal totalConsumption;

    /**
     * 订单数量
     */
    private Integer orderCount;

    /**
     * 状态：0 禁用，1 正常
     */
    private Integer status;

    /**
     * 会员开始时间
     */
    private LocalDateTime membershipStart;

    /**
     * 会员结束时间
     */
    private LocalDateTime membershipEnd;

    /**
     * 注册时间
     */
    private LocalDateTime registerTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}




