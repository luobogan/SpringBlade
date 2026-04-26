package org.springblade.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_order")
public class Order extends TenantEntity {

	@TableField(value = "order_no")
    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal actualAmount;

    private Long couponId;

    private BigDecimal couponAmount;

    private String paymentMethod;

    private String paymentNo;

    private LocalDateTime paymentTime;

    private String shippingMethod;

    private Long shippingAddressId;

    private String trackingNo;

    private LocalDateTime shippingTime;

    private LocalDateTime confirmTime;

    private String orderStatus;

    private String remark;

    /**
     * BaseEntity中的status字段，订单表无此字段，标记为不存在
     */
    private Integer status;
}
