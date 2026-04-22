package org.springblade.mall.service;

import org.springblade.mall.dto.ProductRelationDTO;
import org.springblade.mall.vo.ProductRelationVO;
import java.util.List;

/**
 * 商品关联服务接口
 */
public interface ProductRelationService {

    /**
     * 获取商品的关联商品列表
     * @param productId 商品ID
     * @param type 关联类型（可选）
     * @return 关联商品列表
     */
    List<ProductRelationVO> getProductRelations(Long productId, Integer type);

    /**
     * 添加商品关联
     * @param relationDTO 关联信息
     * @return 创建的关联
     */
    ProductRelationVO addProductRelation(ProductRelationDTO relationDTO);

    /**
     * 批量添加商品关联
     * @param productId 主商品ID
     * @param relations 关联列表
     * @return 创建的关联列表
     */
    List<ProductRelationVO> batchAddProductRelations(Long productId, List<ProductRelationDTO> relations);

    /**
     * 更新商品关联
     * @param id 关联ID
     * @param relationDTO 关联信息
     * @return 更新后的关联
     */
    ProductRelationVO updateProductRelation(Long id, ProductRelationDTO relationDTO);

    /**
     * 删除商品关联
     * @param id 关联ID
     */
    void deleteProductRelation(Long id);

    /**
     * 批量删除商品关联
     * @param ids 关联ID列表
     */
    void batchDeleteProductRelations(List<Long> ids);

    /**
     * 清空商品的关联商品
     * @param productId 商品ID
     * @param type 关联类型（可选，为空则清空所有类型）
     */
    void clearProductRelations(Long productId, Integer type);

    /**
     * 检查商品关联是否存在
     * @param productId 主商品ID
     * @param relatedProductId 关联商品ID
     * @param type 关联类型
     * @return 是否存在
     */
    boolean isRelationExists(Long productId, Long relatedProductId, Integer type);
}



