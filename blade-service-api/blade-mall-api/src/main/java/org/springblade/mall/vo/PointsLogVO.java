package org.springblade.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 积分日志 VO
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
public class PointsLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 积分值
     */
    private Integer pointsValue;

    /**
     * 类型：1 增加，2 减少
     */
    private Integer type;

    /**
     * 类型文本
     */
    private String typeText;

    /**
     * 子类型
     */
    private Integer subType;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源 ID
     */
    private Long sourceId;

    /**
     * 变动前积分
     */
    private Integer beforePoints;

    /**
     * 变动后积分
     */
    private Integer afterPoints;

    /**
     * 描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}




