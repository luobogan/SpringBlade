package org.springblade.mall.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * SKU矩阵生成DTO
 */
@Data
public class SkuMatrixGenerateDTO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 规格属性列表
     */
    private List<ProductSpecAttributeDTO> attributes;

    /**
     * 基础价格
     */
    private BigDecimal basePrice;

    /**
     * 基础库存
     */
    private Integer baseStock;

    /**
     * 是否清空现有SKU
     */
    private Boolean clearExisting;
}




