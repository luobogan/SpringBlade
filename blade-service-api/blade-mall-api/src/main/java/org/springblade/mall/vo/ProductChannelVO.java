package org.springblade.mall.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品渠道关联VO
 */
@Data
public class ProductChannelVO {
    private Long productId;
    private Long channelId;
    private String channelName;
    private String channelCode;
    private String channelIcon;
    private String channelProductId;
    private String channelProductUrl;
    private BigDecimal channelPrice;
    private Integer channelStock;
    private String status;
}




