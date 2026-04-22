package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.time.LocalDateTime;

/**
 * 商品相册图片实体类
 * 用于存储商品相册图片的详细信息
 *
 * @author YoupinMall
 * @since 2026-02-25
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_album_image")
public class ProductAlbumImage extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID（关联到具体的商品规格）
     */
    private Long skuId;

    /**
     * 颜色属性标识
     */
    private String color;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 排序序号
     */
    private Integer sort;

    /**
     * 是否为主图
     */
    private Integer isMain;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 图片宽度
     */
    private Integer width;

    /**
     * 图片高度
     */
    private Integer height;

    /**
     * 图片大小（字节）
     */
    private Long size;

    /**
     * 图片类型
     */
    private String type;
}





