package org.springblade.mall.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品VO（视图对象）
 *
 * V3 更新说明：
 * 1. 添加前端ProductForm.tsx所需的所有字段
 * 2. 确保字段名与前端一致
 * 3. 添加缺失的关联信息字段
 *
 * @author YoupinMall
 * @since 2026-02-20
 */
@Data
public class ProductVO {

    // ==================== 主键字段 ====================

    /**
     * 商品ID
     */
    private Long id;

    // ==================== 基本信息字段 ====================

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品副标题
     */
    private String subtitle;

    /**
     * 商品描述（简短，用于列表页）
     */
    private String description;

    /**
     * 商品详细描述（HTML富文本，用于详情页）
     */
    private String detailDescription;

    /**
     * 商品标签（如：["热卖", "新品", "包邮"]）
     */
    private List<String> tags;

    // ==================== 分类品牌字段 ====================

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌Logo
     */
    private String brandLogo;

    // ==================== 价格库存字段 ====================

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * SKU 总库存（所有 SKU 库存之和）
     */
    private Integer totalSkuStock;

    /**
     * 库存预警值
     */
    private Integer stockWarning;

    /**
     * 商品单位（默认：件）
     */
    private String unit;

    /**
     * 商品重量（单位：千克）
     */
    private BigDecimal weight;

    /**
     * 最低起购量（默认：1）
     */
    private Integer minPurchase;

    /**
     * 最高限购量（NULL表示不限制）
     */
    private Integer maxPurchase;

    // ==================== 销售统计字段 ====================

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 已售数量（兼容前端字段名）
     */
    private Integer sold;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 收藏量
     */
    private Integer favoriteCount;

    // ==================== 评价评分字段 ====================

    /**
     * 用户评分（0.0-5.0）
     */
    private BigDecimal userRating;

    /**
     * 用户评价数
     */
    private Integer userReviewCount;

    // ==================== 排序权重字段 ====================

    /**
     * 排序权重（值越大越靠前）
     */
    private Integer sortOrder;

    /**
     * 排序（兼容前端字段名）
     */
    private Integer sort;

    // ==================== 商品编码字段 ====================

    /**
     * 商品编号/SKU
     */
    private String productCode;

    /**
     * 商品货号（兼容前端字段名）
     */
    private String productSn;

    // ==================== 图片标识字段 ====================

    /**
     * 主图（兼容旧字段名）
     */
    private String image;

    /**
     * 主图（新字段名，与数据库字段一致）
     */
    private String mainImage;

    /**
     * 商品图片列表
     */
    private List<String> images;

    /**
     * 商品相册（兼容前端字段名）
     */
    private List<String> album;

    /**
     * 商品图片数量
     */
    private Integer imageCount;

    /**
     * 商品相册图片列表（包含详细信息）
     */
    private List<ProductAlbumImageVO> albumImages;

    // ==================== 状态标识字段 ====================

    /**
     * 状态字符串：active/inactive
     */
    private String status;

    /**
     * 是否推荐（兼容旧字段名）
     */
    private Boolean isFeatured;

    /**
     * 是否新品（Boolean类型）
     */
    private Boolean isNew;

    /**
     * 是否热销（Boolean类型）
     */
    private Boolean isHot;

    /**
     * 是否推荐（Boolean类型，新字段名）
     */
    private Boolean isRecommend;

    /**
     * 是否预告商品
     */
    private Boolean isPreview;

    /**
     * 是否会员专属商品
     */
    private Boolean isMembership;

    /**
     * 是否上架（兼容前端字段名）
     */
    private Boolean isOnSale;

    /**
     * 是否有折扣
     */
    private Boolean hasDiscount;

    /**
     * 折扣率（0-1之间的小数）
     */
    private BigDecimal discountRate;

    // ==================== 促销相关字段 ====================

    /**
     * 促销类型
     */
    private Integer promotionType;

    /**
     * 促销ID
     */
    private Long promotionId;

    /**
     * 赠送积分
     */
    private Integer giftPoint;

    /**
     * 赠送成长值
     */
    private Integer giftGrowth;

    /**
     * 积分使用限制
     */
    private Integer usePointLimit;

    /**
     * 服务保证IDs
     */
    private List<String> serviceIds;

    // ==================== SEO相关字段 ====================

    /**
     * 详情页标题
     */
    private String detailTitle;

    /**
     * 详情页描述
     */
    private String detailDesc;

    /**
     * 关键字
     */
    private String keywords;

    /**
     * 备注
     */
    private String note;

    // ==================== 时间字段 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 上架时间
     */
    private LocalDateTime onShelfTime;

    /**
     * 下架时间
     */
    private LocalDateTime offShelfTime;

    // ==================== 列表字段（关联表数据） ====================

    /**
     * 商品颜色列表
     */
    private List<String> colors;

    /**
     * 商品颜色数量
     */
    private Integer colorCount;

    /**
     * 商品尺寸列表
     */
    private List<String> sizes;

    /**
     * 商品尺寸数量
     */
    private Integer sizeCount;

    /**
     * 商品规格列表
     */
    private List<ProductSpecificationVO> specifications;

    /**
     * 商品SKU列表
     */
    private List<ProductSkuVO> skus;

    /**
     * 关联商品IDs
     */
    private List<Long> relatedProducts;

    /**
     * 搭配商品IDs
     */
    private List<Long> bundleProducts;

    /**
     * 推荐商品IDs
     */
    private List<Long> recommendProducts;

    /**
     * 商品参数列表
     */
    private List<Map<String, String>> params;

    /**
     * 分类属性列表
     */
    private List<ProductSpecAttributeVO> specAttributes;

    /**
     * 商品属性值列表
     */
    private List<Map<String, Object>> attributeValues;

    // ==================== 便捷方法 ====================

    /**
     * 判断是否有库存
     */
    public boolean hasStock() {
        return stock != null && stock > 0;
    }

    /**
     * 判断是否有原价（即是否有折扣）
     */
    public boolean hasOriginalPrice() {
        return originalPrice != null && originalPrice.compareTo(price) > 0;
    }
}




