package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价图片实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_review_image")
public class ReviewImage extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 评价ID
     */
    private Long reviewId;

    /**
     * 图片URL
     */
    private String imageUrl;
}





