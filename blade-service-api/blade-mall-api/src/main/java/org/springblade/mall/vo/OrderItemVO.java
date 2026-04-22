package org.springblade.mall.vo;

import lombok.Data;

/**
 * 订单项VO
 */
@Data
public class OrderItemVO {

    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private String productImage;

    private Double price;

    private Double totalPrice;

    private Integer quantity;

    private String skuAttributes;

    private Long skuId;
}




