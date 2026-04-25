package org.springblade.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long productId;

    private Integer quantity;

    private Long skuId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private String skuAttributes;
}
