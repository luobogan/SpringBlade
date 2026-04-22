package org.springblade.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 订单DTO
 */
@Data
public class OrderDTO {

    private Long userId;

    @NotNull(message = "地址ID不能为空")
    private Long shippingAddressId;

    private Long couponId;

    @NotNull(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    private String remark;
}




