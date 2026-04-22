package org.springblade.mall.service;

import org.springblade.mall.dto.*;
import org.springblade.mall.vo.ProductSkuVO;
import org.springblade.mall.vo.ProductSpecAttributeVO;
import org.springblade.mall.vo.ProductVO;
import org.springblade.mall.vo.SkuStockLogVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品服务接口 - 增强版
 */
public interface ProductService {

    /**
     * 创建商品
     * @param productDTO 商品信息
     * @return 创建的商品
     */
    ProductVO createProduct(ProductDTO productDTO);

    /**
     * 更新商品（不带确认参数，向后兼容）
     * @param id 商品ID
     * @param productDTO 商品信息
     * @return 更新后的商品
     */
    ProductVO updateProduct(Long id, ProductDTO productDTO);

    /**
     * 更新商品（带确认参数）
     * @param id 商品ID
     * @param productDTO 商品信息
     * @param confirmEdit 是否确认编辑已上架商品
     * @return 更新后的商品
     */
    ProductVO updateProduct(Long id, ProductDTO productDTO, Boolean confirmEdit);

    /**
     * 删除商品（逻辑删除）
     * @param id 商品ID
     */
    void deleteProduct(Long id);

    /**
     * 恢复商品（从回收站恢复）
     * @param id 商品ID
     */
    void restoreProduct(Long id);

    /**
     * 获取回收站商品列表
     * @return 已删除商品列表
     */
    List<ProductVO> getDeletedProducts();

    /**
     * 批量删除商品
     * @param ids 商品ID列表
     */
    void batchDeleteProducts(List<Long> ids);

    /**
     * 获取商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    ProductVO getProductById(Long id);

    /**
     * 获取所有商品
     * @return 商品列表
     */
    List<ProductVO> getAllProducts();

    /**
     * 高级查询商品列表（支持筛选、排序、分页）
     * @param queryDTO 查询参数DTO
     * @return 商品列表及分页信息
     */
    Map<String, Object> getProductsWithFilters(ProductQueryDTO queryDTO);

    /**
     * 获取活跃商品
     * @return 活跃商品列表
     */
    List<ProductVO> getActiveProducts();

    /**
     * 获取特色商品
     * @return 特色商品列表
     */
    List<ProductVO> getFeaturedProducts();

    /**
     * 获取新品
     * @return 新品列表
     */
    List<ProductVO> getNewProducts();

    /**
     * 获取热销商品
     * @return 热销商品列表
     */
    List<ProductVO> getHotProducts();

    /**
     * 根据分类获取商品
     * @param categoryId 分类ID
     * @return 商品列表
     */
    List<ProductVO> getProductsByCategory(Long categoryId);

    /**
     * 根据品牌获取商品
     * @param brandId 品牌ID
     * @return 商品列表
     */
    List<ProductVO> getProductsByBrand(Long brandId);

    /**
     * 搜索商品
     * @param keyword 搜索关键词
     * @return 商品列表
     */
    List<ProductVO> searchProducts(String keyword);

    /**
     * 更新商品状态
     * @param id 商品ID
     * @param status 状态值
     */
    void updateProductStatus(Long id, Integer status);

    /**
     * 批量更新商品状态
     * @param ids 商品ID列表
     * @param status 状态值
     */
    void batchUpdateProductStatus(List<Long> ids, Integer status);

    /**
     * 商品上架
     * @param id 商品ID
     */
    void publishProduct(Long id);

    /**
     * 商品下架
     * @param id 商品ID
     */
    void unpublishProduct(Long id);

    /**
     * 设置推荐商品
     * @param id 商品ID
     * @param isRecommend 是否推荐
     */
    void setRecommend(Long id, Integer isRecommend);

    /**
     * 设置新品
     * @param id 商品ID
     * @param isNew 是否新品
     */
    void setNew(Long id, Integer isNew);

    /**
     * 设置热销
     * @param id 商品ID
     * @param isHot 是否热销
     */
    void setHot(Long id, Integer isHot);

    /**
     * 调整库存
     * @param id 商品ID
     * @param quantity 调整数量（正数增加，负数减少）
     * @param remark 备注
     */
    void adjustStock(Long id, Integer quantity, String remark);

    /**
     * 获取相关推荐商品
     * @param id 商品ID
     * @return 推荐商品列表
     */
    List<ProductVO> getRelatedProducts(Long id);

    /**
     * 获取商品统计数据
     * @return 统计数据
     */
    Map<String, Object> getProductStats();

    /**
     * 获取商品SKU列表
     * @param productId 商品ID
     * @return SKU列表
     */
    List<ProductSkuVO> getProductSkus(Long productId);

    /**
     * 创建商品SKU
     * @param productId 商品ID
     * @param skuDTO SKU信息
     * @return 创建的SKU
     */
    ProductSkuVO createProductSku(Long productId, ProductSkuDTO skuDTO);

    /**
     * 更新商品SKU
     * @param id SKU ID
     * @param skuDTO SKU信息
     * @return 更新后的SKU
     */
    ProductSkuVO updateProductSku(Long id, ProductSkuDTO skuDTO);

    /**
     * 删除商品SKU
     * @param id SKU ID
     */
    void deleteProductSku(Long id);

    /**
     * 批量创建商品SKU
     * @param productId 商品ID
     * @param skuDTOs SKU列表
     * @return 创建的SKU列表
     */
    List<ProductSkuVO> batchCreateProductSkus(Long productId, List<ProductSkuDTO> skuDTOs);

    /**
     * 生成SKU矩阵
     * @param generateDTO SKU矩阵生成参数
     * @return 生成的SKU列表
     */
    List<ProductSkuVO> generateSkuMatrix(SkuMatrixGenerateDTO generateDTO);

    /**
     * 获取商品规格属性列表
     * @param productId 商品ID
     * @return 规格属性列表
     */
    List<ProductSpecAttributeVO> getProductSpecAttributes(Long productId);

    /**
     * 保存商品规格属性
     * @param productId 商品ID
     * @param attributes 规格属性列表
     * @return 保存后的规格属性列表
     */
    List<ProductSpecAttributeVO> saveProductSpecAttributes(Long productId, List<ProductSpecAttributeDTO> attributes);

    /**
     * 删除商品规格属性
     * @param attributeId 属性ID
     */
    void deleteProductSpecAttribute(Long attributeId);

    /**
     * 获取SKU库存变动日志
     * @param skuId SKU ID
     * @return 库存变动日志列表
     */
    List<SkuStockLogVO> getSkuStockLogs(Long skuId);

    /**
     * 调整SKU库存并记录日志
     * @param skuId SKU ID
     * @param quantity 调整数量
     * @param type 变动类型：1入库，2出库，3调整
     * @param remark 备注
     */
    void adjustSkuStock(Long skuId, Integer quantity, Integer type, String remark);

    /**
     * 更新商品总库存
     * @param productId 商品ID
     */
    void updateProductStock(Long productId);

    /**
     * 获取商品参数列表
     * @param productId 商品ID
     * @return 商品参数列表
     */
    List<Map<String, String>> getProductParams(Long productId);
}



