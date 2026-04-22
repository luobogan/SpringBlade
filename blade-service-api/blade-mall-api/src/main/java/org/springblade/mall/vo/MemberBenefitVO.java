package org.springblade.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 会员权益 VO
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
public class MemberBenefitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权益 ID
     */
    private Long id;

    /**
     * 等级 ID
     */
    private Long levelId;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 权益名称
     */
    private String name;

    /**
     * 权益类型：1 折扣券，2 优惠券，3 免邮券，4 生日礼，5 专属客服，6 优先发货，7 退换货特权，8 积分加速，9 其他
     */
    private Integer type;

    /**
     * 权益值
     */
    private String value;

    /**
     * 权益描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态：0 禁用，1 正常
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}




