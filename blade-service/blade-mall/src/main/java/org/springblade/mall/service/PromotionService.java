package org.springblade.mall.service;

import org.springblade.mall.dto.PromotionDTO;
import org.springblade.mall.dto.PromotionProductDTO;
import org.springblade.mall.vo.PromotionVO;
import org.springblade.mall.vo.PromotionProductVO;
import java.util.List;

/**
 * 促销规则服务接口
 */
public interface PromotionService {

    /**
     * 创建促销规则
     * @param promotionDTO 促销信息
     * @return 创建的促销
     */
    PromotionVO createPromotion(PromotionDTO promotionDTO);

    /**
     * 更新促销规则
     * @param id 促销ID
     * @param promotionDTO 促销信息
     * @return 更新后的促销
     */
    PromotionVO updatePromotion(Long id, PromotionDTO promotionDTO);

    /**
     * 删除促销规则
     * @param id 促销ID
     */
    void deletePromotion(Long id);

    /**
     * 获取促销规则详情
     * @param id 促销ID
     * @return 促销详情
     */
    PromotionVO getPromotionById(Long id);

    /**
     * 获取促销规则列表
     * @param type 促销类型（可选）
     * @param status 状态（可选）
     * @return 促销列表
     */
    List<PromotionVO> getPromotionList(Integer type, Integer status);

    /**
     * 添加促销商品
     * @param promotionProductDTO 促销商品信息
     * @return 创建的促销商品关联
     */
    PromotionProductVO addPromotionProduct(PromotionProductDTO promotionProductDTO);

    /**
     * 批量添加促销商品
     * @param promotionId 促销ID
     * @param products 商品列表
     * @return 创建的促销商品关联列表
     */
    List<PromotionProductVO> batchAddPromotionProducts(Long promotionId, List<PromotionProductDTO> products);

    /**
     * 删除促销商品
     * @param id 关联ID
     */
    void deletePromotionProduct(Long id);

    /**
     * 获取促销商品列表
     * @param promotionId 促销ID
     * @return 促销商品列表
     */
    List<PromotionProductVO> getPromotionProducts(Long promotionId);

    /**
     * 更新促销状态
     * @param id 促销ID
     * @param status 状态
     */
    void updatePromotionStatus(Long id, Integer status);

    /**
     * 应用促销到商品
     * @param promotionId 促销ID
     */
    void applyPromotionToProducts(Long promotionId);

    /**
     * 取消商品促销
     * @param promotionId 促销ID
     */
    void cancelPromotionFromProducts(Long promotionId);
}



