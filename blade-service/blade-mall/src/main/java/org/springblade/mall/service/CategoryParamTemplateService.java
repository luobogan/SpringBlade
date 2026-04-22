package org.springblade.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.mall.dto.CategoryParamTemplateDTO;
import org.springblade.mall.vo.CategoryParamTemplateVO;

import java.util.List;

/**
 * 分类参数模板服务接口
 */
public interface CategoryParamTemplateService {
    
    /**
     * 创建参数模板
     */
    CategoryParamTemplateVO createParamTemplate(CategoryParamTemplateDTO dto);
    
    /**
     * 更新参数模板
     */
    CategoryParamTemplateVO updateParamTemplate(Long id, CategoryParamTemplateDTO dto);
    
    /**
     * 删除参数模板
     */
    void deleteParamTemplate(Long id);
    
    /**
     * 获取参数模板详情
     */
    CategoryParamTemplateVO getParamTemplateById(Long id);
    
    /**
     * 根据分类ID获取参数模板列表
     */
    List<CategoryParamTemplateVO> getParamTemplatesByCategoryId(Long categoryId);
    
    /**
     * 分页查询参数模板
     */
    IPage<CategoryParamTemplateVO> getParamTemplatePage(Long categoryId, int page, int pageSize);
    
    /**
     * 复制参数模板到其他分类
     */
    void copyParamTemplatesToCategory(Long sourceCategoryId, Long targetCategoryId);
}



