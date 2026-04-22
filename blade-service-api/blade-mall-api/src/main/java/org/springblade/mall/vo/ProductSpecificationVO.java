package org.springblade.mall.vo;

import lombok.Data;

/**
 * 商品规格VO
 */
@Data
public class ProductSpecificationVO {
    
    private Long id;
    
    private Long productId;
    
    private String name;
    
    private String value;
}



