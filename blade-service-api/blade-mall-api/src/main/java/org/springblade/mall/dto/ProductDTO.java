package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品DTO（数据传输对象）
 *
 * V2 更新说明：
 * 1. 重命名字段：rating -> userRating, reviewCount -> userReviewCount
 * 2. 新增字段：detailDescription, tags, sortOrder, unit, weight, productCode, minPurchase, maxPurchase, viewCount, favoriteCount, onShelfTime, offShelfTime
 * 3. 保持列表字段：images, colors, sizes, specifications
 *
 * @author YoupinMall
 * @since 2026-01-28
 */
@Data
public class ProductDTO {

    // ==================== 基本信息字段 ====================

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
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
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 品牌ID
     */
    @NotNull(message = "品牌ID不能为空")
    private Long brandId;

    // ==================== 价格库存字段 ====================

    /**
     * 商品价格
     */
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
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
     * 库存（可选，使用 SKU 库存时不需要此字段）
     */
    private Integer stock;

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

    // ==================== 商品编码字段 ====================

    /**
     * 商品编号/SKU
     */
    private String productCode;

    /**
     * 商品货号
     */
    private String productSn;

    /**
     * 排序
     */
    private Integer sort;

    // ==================== 图片标识字段 ====================

    /**
     * 主图
     */
    private String mainImage;

    // ==================== 状态标识字段 ====================

    /**
     * 是否新品：0否，1是
     */
    private Integer isNew;

    /**
     * 是否热销：0否，1是
     */
    private Integer isHot;

    /**
     * 是否推荐：0否，1是
     */
    private Integer isRecommend;

    /**
     * 是否预告商品：0否，1是
     */
    private Integer isPreview;

    /**
     * 是否会员专属商品：0 否，1 是
     */
    private Integer isMembership;

    /**
     * 状态：1 正常，0 禁用
     */
    private Integer status;

    // ==================== 时间字段 ====================

    /**
     * 上架时间
     */
    private LocalDateTime onShelfTime;

    /**
     * 下架时间
     */
    private LocalDateTime offShelfTime;

    // ==================== 促销相关字段 ====================

    /**
     * 当前生效的促销ID
     */
    private Long promotionId;

    /**
     * 促销类型
     */
    private Integer promotionType;

    // ==================== 积分相关字段 ====================

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

    // ==================== 服务保证字段 ====================

    /**
     * 服务保证IDs（如：[1, 2, 3]）
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

    // ==================== 列表字段（关联表数据） ====================

    /**
     * 商品图片列表
     */
    private List<String> images;

    /**
     * 商品相册图片列表（包含详细信息）
     */
    private List<ProductAlbumImageDTO> albumImages;

    /**
     * 商品颜色列表
     */
    private List<String> colors;

    /**
     * 商品尺寸列表
     */
    private List<String> sizes;

    /**
     * 商品规格列表
     */
    private List<ProductSpecificationDTO> specifications;

    // ==================== 商品属性和参数字段 ====================

    /**
     * 商品属性值列表
     */
    private List<ProductAttributeValueDTO> attributeValues;

    /**
     * 商品参数值列表
     */
    private List<ProductParamDTO> paramValues;

    // ==================== 关联商品字段 ====================

    /**
     * 相关商品IDs
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

    // ==================== SKU字段 ====================

    /**
     * SKU列表
     */
    private List<ProductSkuDTO> skus;

    /**
     * 规格属性列表
     */
    private List<ProductSpecAttributeDTO> specAttributes;
}



