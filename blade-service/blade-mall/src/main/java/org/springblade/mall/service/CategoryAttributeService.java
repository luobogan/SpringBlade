package org.springblade.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.mall.dto.CategoryAttributeDTO;
import org.springblade.mall.dto.CategoryAttributeValueDTO;
import org.springblade.mall.vo.CategoryAttributeVO;
import org.springblade.mall.vo.CategoryAttributeValueVO;
import java.util.List;

/**
 * 分类属性服务接口
 */
public interface CategoryAttributeService {

    /**
     * 创建分类属性
     */
    CategoryAttributeVO createAttribute(CategoryAttributeDTO dto);

    /**
     * 更新分类属性
     */
    CategoryAttributeVO updateAttribute(Long id, CategoryAttributeDTO dto);

    /**
     * 删除分类属性
     */
    void deleteAttribute(Long id);

    /**
     * 获取分类属性详情（包含属性值）
     */
    CategoryAttributeVO getAttributeById(Long id);

    /**
     * 获取分类的所有属性（包含属性值）
     */
    List<CategoryAttributeVO> getAttributesByCategoryId(Long categoryId);

    /**
     * 分页查询分类属性
     */
    IPage<CategoryAttributeVO> getAttributePage(Long categoryId, int page, int pageSize);

    /**
     * 添加属性值
     */
    CategoryAttributeValueVO addAttributeValue(CategoryAttributeValueDTO dto);

    /**
     * 更新属性值
     */
    CategoryAttributeValueVO updateAttributeValue(Long id, CategoryAttributeValueDTO dto);

    /**
     * 删除属性值
     */
    void deleteAttributeValue(Long id);

    /**
     * 获取属性的所有值
     */
    List<CategoryAttributeValueVO> getAttributeValues(Long attributeId);

    /**
     * 批量添加属性值
     */
    List<CategoryAttributeValueVO> batchAddAttributeValues(Long attributeId, List<CategoryAttributeValueDTO> values);

    /**
     * 复制分类属性到其他分类
     */
    void copyAttributesToCategory(Long sourceCategoryId, Long targetCategoryId);
}



