package org.springblade.mall.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 成长值日志 VO
 * </p>
 *
 * @author youpinmall
 * @since 2026-03-05
 */
@Data
public class GrowthLogVO implements Serializable {

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
     * 成长值
     */
    private Integer growthValue;

    /**
     * 类型：1 增加，2 减少
     */
    private Integer type;

    /**
     * 类型文本
     */
    private String typeText;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源 ID
     */
    private Long sourceId;

    /**
     * 变动前成长值
     */
    private Integer beforeGrowth;

    /**
     * 变动后成长值
     */
    private Integer afterGrowth;

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




