package org.springblade.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.mall.entity.Product;
import org.springblade.mall.entity.ProductColor;
import org.springblade.mall.entity.ProductImage;
import org.springblade.mall.entity.ProductSize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品Mapper
 */

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据产品ID查询产品颜色
     */
    @Select("SELECT * FROM mall_product_color WHERE product_id = #{productId}")
    List<ProductColor> selectProductColors(@Param("productId") Long productId);

    /**
     * 根据产品ID查询产品图片
     */
    @Select("SELECT * FROM mall_product_image WHERE product_id = #{productId} ORDER BY sort ASC")
    List<ProductImage> selectProductImages(@Param("productId") Long productId);

    /**
     * 根据产品ID查询产品尺寸
     */
    @Select("SELECT * FROM mall_product_size WHERE product_id = #{productId}")
    List<ProductSize> selectProductSizes(@Param("productId") Long productId);

    /**
     * 恢复已删除的商品
     */
    @org.apache.ibatis.annotations.Update("UPDATE mall_product SET is_deleted = 0, updated_at = #{updatedAt} WHERE id = #{id} AND is_deleted = 1 AND tenant_id = #{tenantId}")
    int restoreProduct(@Param("id") Long id, @Param("updatedAt") java.time.LocalDateTime updatedAt, @Param("tenantId") String tenantId);

    /**
     * 逻辑删除商品
     */
    @org.apache.ibatis.annotations.Update("UPDATE mall_product SET is_deleted = 1, updated_at = #{updatedAt} WHERE id = #{id} AND is_deleted = 0 AND tenant_id = #{tenantId}")
    int deleteProduct(@Param("id") Long id, @Param("updatedAt") java.time.LocalDateTime updatedAt, @Param("tenantId") String tenantId);

    /**
     * 查询已删除的商品
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM mall_product WHERE is_deleted = 1 AND tenant_id = #{tenantId}")
    List<Product> selectDeletedProducts(@Param("tenantId") String tenantId);
}




