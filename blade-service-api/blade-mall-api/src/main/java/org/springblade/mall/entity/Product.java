package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import org.springblade.mall.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品实体类
 *
 * V2 更新说明：
 * 1. 重命名字段：rating -> userRating, reviewCount -> userReviewCount
 * 2. 新增字段：detailDescription, tags, sortOrder, unit, weight, productCode, minPurchase, maxPurchase, viewCount, favoriteCount, onShelfTime, offShelfTime
 * 3. 新增关联字段：categoryName, brandName, brandLogo, imageCount, colorCount, sizeCount, hotScore, recommendScore
 *
 * @author YoupinMall
 * @since 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mall_product", autoResultMap = true)
public class Product extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    // ==================== 基本信息字段 ====================

    /**
     * 产品名称
     */
    private String name;

    /**
     * 产品副标题
     */
    private String subtitle;

    /**
     * 产品描述（简短，用于列表页）
     */
    private String description;

    /**
     * 产品详细描述（HTML富文本，用于详情页）
     */
    @TableField("detail_description")
    private String detailDescription;

    /**
     * 产品标签（JSON格式，如：["热卖", "新品", "包邮"]）
     */
    @TableField(value = "tags", typeHandler = JsonTypeHandler.class)
    private List<String> tags;

    // ==================== 价格库存字段 ====================

    /**
     * 产品价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /**
     * 成本价
     */
    @TableField("cost_price")
    private BigDecimal costPrice;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 库存预警值
     */
    @TableField("stock_warning")
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
    @TableField("min_purchase")
    private Integer minPurchase;

    /**
     * 最高限购量（NULL表示不限制）
     */
    @TableField("max_purchase")
    private Integer maxPurchase;

    // ==================== 销售统计字段 ====================

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 收藏量
     */
    @TableField("favorite_count")
    private Integer favoriteCount;

    // ==================== 评价评分字段 ====================

    /**
     * 用户评分（0.0-5.0）
     */
    @TableField("user_rating")
    private BigDecimal userRating;

    /**
     * 用户评价数
     */
    @TableField("user_review_count")
    private Integer userReviewCount;

    // ==================== 排序权重字段 ====================

    /**
     * 排序权重（值越大越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    // ==================== 商品编码字段 ====================

    /**
     * 商品编号/SKU
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 商品货号
     */
    @TableField("product_sn")
    private String productSn;

    /**
     * 排序
     */
    private Integer sort;

    // ==================== 分类品牌字段 ====================

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 品牌ID
     */
    @TableField("brand_id")
    private Long brandId;

    // ==================== 图片标识字段 ====================

    /**
     * 主图
     */
    @TableField("main_image")
    private String mainImage;

    // ==================== 状态标识字段 ====================

    /**
     * 是否新品：0否，1是
     */
    @TableField("is_new")
    private Integer isNew;

    /**
     * 是否热销：0否，1是
     */
    @TableField("is_hot")
    private Integer isHot;

    /**
     * 是否推荐：0否，1是
     */
    @TableField("is_recommend")
    private Integer isRecommend;

    /**
     * 是否预告商品：0否，1是
     */
    @TableField("is_preview")
    private Integer isPreview;

    /**
     * 会员专属商品：0 否，1 是（仅会员可见或会员专享价）
     */
    @TableField("is_membership")
    private Integer isMembership;

    // ==================== 时间字段 ====================

    /**
     * 上架时间
     */
    @TableField("on_shelf_time")
    private LocalDateTime onShelfTime;

    /**
     * 下架时间
     */
    @TableField("off_shelf_time")
    private LocalDateTime offShelfTime;

    // ==================== 促销相关字段 ====================

    /**
     * 当前生效的促销ID
     */
    @TableField("promotion_id")
    private Long promotionId;

    /**
     * 促销价格
     */
    @TableField("promotion_price")
    private BigDecimal promotionPrice;

    /**
     * 促销结束时间
     */
    @TableField("promotion_end_time")
    private LocalDateTime promotionEndTime;

    /**
     * 促销类型
     */
    @TableField("promotion_type")
    private Integer promotionType;

    // ==================== 积分相关字段 ====================

    /**
     * 赠送积分
     */
    @TableField("gift_point")
    private Integer giftPoint;

    /**
     * 赠送成长值
     */
    @TableField("gift_growth")
    private Integer giftGrowth;

    /**
     * 积分使用限制
     */
    @TableField("use_point_limit")
    private Integer usePointLimit;

    // ==================== 服务保证字段 ====================

    /**
     * 服务保证IDs（JSON格式）
     */
    @TableField(value = "service_ids", typeHandler = JsonTypeHandler.class)
    private List<String> serviceIds;

    // ==================== SEO相关字段 ====================

    /**
     * 详情页标题
     */
    @TableField("detail_title")
    private String detailTitle;

    /**
     * 详情页描述
     */
    @TableField("detail_desc")
    private String detailDesc;

    /**
     * 关键字
     */
    private String keywords;

    /**
     * 备注
     */
    private String note;



    // ==================== 以下是关联字段（非数据库字段，exist = false） ====================

    /**
     * 分类名称（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 品牌名称（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private String brandName;

    /**
     * 品牌Logo（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private String brandLogo;

    /**
     * 图片数量（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private Integer imageCount;

    /**
     * 颜色数量（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private Integer colorCount;

    /**
     * 尺寸数量（来自视图 v_product_detail 或关联查询）
     */
    @TableField(exist = false)
    private Integer sizeCount;

    /**
     * 热度分数（来自视图 v_hot_products）
     * 计算公式：viewCount * 0.4 + sales * 0.6
     */
    @TableField(exist = false)
    private BigDecimal hotScore;

    /**
     * 推荐分数（来自视图 v_recommended_products）
     * 计算公式：userRating * 0.3 + sales * 0.3 + isRecommend * 0.4
     */
    @TableField(exist = false)
    private BigDecimal recommendScore;

    // ==================== 便捷方法 ====================

    /**
     * 判断商品是否有库存
     */
    public boolean hasStock() {
        return stock != null && stock > 0;
    }

    /**
     * 判断商品是否为新品
     */
    public boolean isNewProduct() {
        return isNew != null && isNew == 1;
    }

    /**
     * 判断商品是否热销
     */
    public boolean isHotProduct() {
        return isHot != null && isHot == 1;
    }

    /**
     * 判断商品是否推荐
     */
    public boolean isRecommended() {
        return isRecommend != null && isRecommend == 1;
    }

    /**
     * 判断商品是否正常状态
     */
    public boolean isActive() {
        return true; // 简化处理，默认所有商品都是活跃状态
    }

    /**
     * 判断是否有折扣
     */
    public boolean hasDiscount() {
        return originalPrice != null &&
               price != null &&
               originalPrice.compareTo(price) > 0;
    }

    /**
     * 计算折扣率
     */
    public BigDecimal getDiscountRate() {
        if (!hasDiscount()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(price.divide(originalPrice, 2, RoundingMode.HALF_UP));
    }

    /**
     * 获取展示价格（优先使用原价，如果没有则使用现价）
     */
    public BigDecimal getDisplayPrice() {
        return price != null ? price : originalPrice;
    }

    /**
     * 获取最低起购量（默认为1）
     */
    public int getSafeMinPurchase() {
        return minPurchase != null && minPurchase > 0 ? minPurchase : 1;
    }

    /**
     * 获取最高限购量（如果有限制）
     */
    public Integer getSafeMaxPurchase() {
        if (maxPurchase != null && maxPurchase > 0) {
            return maxPurchase;
        }
        return null; // 表示不限制
    }

    /**
     * 判断购买数量是否在合法范围内
     */
    public boolean isValidPurchaseQuantity(int quantity) {
        int min = getSafeMinPurchase();
        Integer max = getSafeMaxPurchase();

        if (quantity < min) {
            return false;
        }

        if (max != null && quantity > max) {
            return false;
        }

        // 还要检查库存
        if (!hasStock() || quantity > stock) {
            return false;
        }

        return true;
    }
}





