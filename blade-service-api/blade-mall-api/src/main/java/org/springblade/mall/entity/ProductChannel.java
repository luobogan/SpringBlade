package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品渠道关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_channel")
public class ProductChannel extends MallTenantEntity {
    
    private Long productId;
    
    private Long channelId;
    
    private String channelProductId;
    
    private String channelProductUrl;
    
    private BigDecimal channelPrice;
    
    private Integer channelStock;
    
    @TableField("status")
    private String channelStatus;
}





