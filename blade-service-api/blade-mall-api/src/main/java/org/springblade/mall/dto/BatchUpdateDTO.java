package org.springblade.mall.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量更新商品DTO
 */
@Data
public class BatchUpdateDTO {
    private List<Long> productIds;
    private String status;
    private BigDecimal priceAdjust;
    private Integer stockAdjust;
    private Boolean isFeatured;
    private Boolean isNew;
    private Boolean isHot;
}




