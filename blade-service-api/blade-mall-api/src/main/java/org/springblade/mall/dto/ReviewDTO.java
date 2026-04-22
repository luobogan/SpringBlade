package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.List;

/**
 * 评价DTO
 */
@Data
public class ReviewDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    @NotNull(message = "评分不能为空")
    @Positive(message = "评分必须大于0")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    private String content;

    private Integer anonymous;

    private List<String> images;
}



