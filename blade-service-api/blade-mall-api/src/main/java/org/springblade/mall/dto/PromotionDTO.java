package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 促销规则DTO
 */
@Data
public class PromotionDTO {

    /**
     * 促销ID（编辑时使用）
     */
    private Long id;

    /**
     * 促销名称
     */
    @NotBlank(message = "促销名称不能为空")
    private String name;

    /**
     * 促销描述
     */
    private String description;

    /**
     * 促销类型：1满减，2折扣，3秒杀，4团购
     */
    @NotNull(message = "促销类型不能为空")
    private Integer type;

    /**
     * 促销规则JSON
     */
    @NotBlank(message = "促销规则不能为空")
    private String rules;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 排序
     */
    private Integer sortOrder;
}




