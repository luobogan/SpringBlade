package org.springblade.mall.vo;

import lombok.Data;

/**
 * 商品相册图片VO
 * 用于前端展示商品相册图片的详细信息
 *
 * @author YoupinMall
 * @since 2026-02-25
 */
@Data
public class ProductAlbumImageVO {

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片URL
     */
    private String url;

    /**
     * 排序序号
     */
    private Integer sort;

    /**
     * 是否为主图
     */
    private Boolean isMain;

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

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * SKU ID（关联到具体的商品规格）
     */
    private Long skuId;

    /**
     * 颜色属性标识
     */
    private String color;
}




