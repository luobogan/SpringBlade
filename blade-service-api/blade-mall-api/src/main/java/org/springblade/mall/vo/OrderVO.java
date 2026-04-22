package org.springblade.mall.vo;

import lombok.Data;
import java.util.List;

/**
 * 订单VO
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private String userName;

    private Long addressId;

    private AddressVO address;

    private Long couponId;

    private String couponCode;

    private String couponName;

    private Integer couponType;

    private Double couponValue;

    private Double couponMinSpend;

    private Double totalAmount;

    private Double actualAmount;

    private Double discountAmount;

    private Integer status;

    private String statusText;

    private String paymentMethod;

    private String paymentNo;

    private String shippingMethod;

    private String trackingNo;

    private String remark;

    private List<OrderItemVO> items;

    private String createdAt;

    private String updatedAt;

    private String paidAt;

    private String shippedAt;

    private String completedAt;

    private String cancelledAt;
}




