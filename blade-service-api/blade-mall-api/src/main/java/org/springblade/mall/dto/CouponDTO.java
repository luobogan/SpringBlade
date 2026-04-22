package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 优惠券DTO
 */
@Data
public class CouponDTO {

    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @NotBlank(message = "优惠券码不能为空")
    private String code;

    @NotNull(message = "优惠券类型不能为空")
    private Integer type; // 1: 固定金额, 2: 百分比

    @NotNull(message = "优惠金额不能为空")
    @Positive(message = "优惠金额必须大于0")
    private Double value;

    private Double maxDiscount;

    @NotNull(message = "最低消费金额不能为空")
    private Double minSpend;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Integer totalQuantity;

    private Integer usedQuantity;

    private Integer perUserLimit;

    private Integer status;

    private String description;
}



