package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.ProductAttributeValue;

/**
 * 商品属性值Mapper
 */
public interface ProductAttributeValueMapper extends BaseMapper<ProductAttributeValue> {

    /**
     * 物理删除商品属性值（绕过@TableLogic限制）
     * @param productId 商品ID
     */
    void deletePhysicalByProductId(Long productId);
}




