package org.springblade.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 优惠券VO
 */
@Data
public class CouponVO {
    
    private Long id;
    
    private String name;
    
    private String code;
    
    private String type;
    
    private String typeText;
    
    private Double value;
    
    private Double amount;
    
    private Double maxDiscount;
    
    private Double minSpend;
    
    private Double minAmount;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer usageLimit;
    
    private Integer usageCount;
    
    private Boolean isActive;
    
    private Boolean isExpired;
    
    private Boolean isAvailable;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Integer status;
    
    private LocalDateTime useTime;
}



