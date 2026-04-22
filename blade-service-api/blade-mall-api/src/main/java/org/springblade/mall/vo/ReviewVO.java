package org.springblade.mall.vo;

import lombok.Data;
import java.util.List;

/**
 * 评价VO
 */
@Data
public class ReviewVO {
    
    private Long id;
    
    private Long productId;
    
    private String productName;
    
    private Long orderId;
    
    private String orderNo;
    
    private Long userId;
    
    private String userName;
    
    private Integer rating;
    
    private String ratingText;
    
    private String content;
    
    private List<String> images;
    
    private Boolean isActive;
    
    private String createdAt;
    
    private String updatedAt;
}



