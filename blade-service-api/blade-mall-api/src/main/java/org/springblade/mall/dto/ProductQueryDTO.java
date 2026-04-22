package org.springblade.mall.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品查询DTO（数据传输对象）
 * 用于封装商品列表查询的参数
 *
 * @author YoupinMall
 * @since 2026-04-01
 */
@Data
public class ProductQueryDTO {

    /**
     * 当前页码，默认为1
     */
    private Integer current = 1;

    /**
     * 每页大小，默认为12
     */
    private Integer pageSize = 12;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer status;

    /**
     * 是否新品：0-否，1-是
     */
    private Integer isNew;

    /**
     * 是否热销：0-否，1-是
     */
    private Integer isHot;

    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommend;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 排序字段，默认为createdAt
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向，默认为desc（降序）
     * 可选值：asc（升序）、desc（降序）
     */
    private String sortOrder = "desc";

}




