package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 促销规则VO
 */
@Data
public class PromotionVO {

    /**
     * 促销ID
     */
    private Long id;

    /**
     * 促销名称
     */
    private String name;

    /**
     * 促销描述
     */
    private String description;

    /**
     * 促销类型：1满减，2折扣，3秒杀，4团购
     */
    private Integer type;

    /**
     * 促销类型文本
     */
    private String typeText;

    /**
     * 促销规则JSON
     */
    private String rules;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态：0未开始，1进行中，2已结束，3已停用
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}




