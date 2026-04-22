package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品参数实体类
 * 
 * @author YoupinMall
 * @since 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_product_param")
public class ProductParam extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 参数名称
     */
    private String paramName;

    /**
     * 参数值
     */
    private String paramValue;

    /**
     * 排序
     */
    private Integer sortOrder;
}





