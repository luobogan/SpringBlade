package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 订单项实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_order_item")
public class OrderItem extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称（下单时快照）
     */
    private String productName;

    /**
     * 产品图片（下单时快照）
     */
    private String productImage;

    /**
     * 产品价格（下单时快照）
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 小计金额
     */
    private BigDecimal totalPrice;

    /**
     * SKU属性（如：颜色、尺寸等）
     */
    private String skuAttributes;

    /**
     * SKU ID
     */
    private Long skuId;
}





