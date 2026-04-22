package org.springblade.mall.service;

import org.springblade.mall.dto.BrandDTO;

import org.springblade.mall.vo.BrandVO;
import java.util.List;

/**
 * 品牌服务接口
 */
public interface BrandService {
    
    /**
     * 创建品牌
     * @param brandDTO 品牌信息
     * @return 创建的品牌
     */
    BrandVO createBrand(BrandDTO brandDTO);
    
    /**
     * 更新品牌
     * @param id 品牌ID
     * @param brandDTO 品牌信息
     * @return 更新后的品牌
     */
    BrandVO updateBrand(Long id, BrandDTO brandDTO);
    
    /**
     * 删除品牌
     * @param id 品牌ID
     */
    void deleteBrand(Long id);
    
    /**
     * 获取品牌详情
     * @param id 品牌ID
     * @return 品牌详情
     */
    BrandVO getBrandById(Long id);
    
    /**
     * 获取所有品牌
     * @return 品牌列表
     */
    List<BrandVO> getAllBrands();
    
    /**
     * 获取活跃的品牌
     * @return 活跃品牌列表
     */
    List<BrandVO> getActiveBrands();
}


