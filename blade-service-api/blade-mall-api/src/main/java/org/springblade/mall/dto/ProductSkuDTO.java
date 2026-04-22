package org.springblade.mall.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品SKU DTO
 */
@Data
public class ProductSkuDTO {

    /**
     * SKU ID（编辑时使用）
     */
    private Long id;

    /**
     * SKU编码
     */
    private String skuCode;

    /**
     * SKU规格名称
     */
    private String skuName;

    /**
     * 规格值1
     */
    private String spec1;

    /**
     * 规格值2
     */
    private String spec2;

    /**
     * 规格值3
     */
    private String spec3;

    /**
     * 规格值4
     */
    private String spec4;

    /**
     * 前端传递的规格对象
     */
    private Map<String, String> specs;

    /**
     * SKU价格
     */
    private BigDecimal price;

    /**
     * SKU原价
     */
    private BigDecimal originalPrice;

    /**
     * SKU促销价格
     */
    private BigDecimal promotionPrice;

    /**
     * SKU库存
     */
    private Integer stock;

    /**
     * SKU图片
     */
    private String image;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 获取规格值数组
     */
    public String[] getSpecValues() {
        if (specs != null && !specs.isEmpty()) {
            return specs.values().toArray(new String[0]);
        }
        return new String[0];
    }
}




