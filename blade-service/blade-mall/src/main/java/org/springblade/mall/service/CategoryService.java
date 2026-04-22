package org.springblade.mall.service;

import org.springblade.mall.dto.CategoryDTO;

import org.springblade.mall.vo.CategoryVO;
import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {
    
    /**
     * 创建分类
     * @param categoryDTO 分类信息
     * @return 创建的分类
     */
    CategoryVO createCategory(CategoryDTO categoryDTO);
    
    /**
     * 更新分类
     * @param id 分类ID
     * @param categoryDTO 分类信息
     * @return 更新后的分类
     */
    CategoryVO updateCategory(Long id, CategoryDTO categoryDTO);
    
    /**
     * 删除分类
     * @param id 分类ID
     */
    void deleteCategory(Long id);
    
    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情
     */
    CategoryVO getCategoryById(Long id);
    
    /**
     * 获取所有分类
     * @return 分类列表
     */
    List<CategoryVO> getAllCategories();
    
    /**
     * 获取子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<CategoryVO> getSubCategories(Long parentId);
    
    /**
     * 获取活跃的分类
     * @return 活跃分类列表
     */
    List<CategoryVO> getActiveCategories();
    
    /**
     * 获取所有分类（包括禁用状态）
     * @return 分类树
     */
    List<CategoryVO> getAllCategoriesWithStatus();
    
    /**
     * 获取子分类（包括禁用状态）
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<CategoryVO> getSubCategoriesWithStatus(Long parentId);
    
    /**
     * 更新分类排序
     * @param sortData 排序数据
     */
    void updateCategorySort(List<SortRequest> sortData);
    
    /**
     * 排序请求DTO
     */
    static class SortRequest {
        private Long id;
        private Integer sort;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }
}
