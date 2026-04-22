package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.mall.dto.CategoryAttributeDTO;
import org.springblade.mall.dto.CategoryAttributeValueDTO;
import org.springblade.mall.entity.Category;
import org.springblade.mall.entity.CategoryAttribute;
import org.springblade.mall.entity.CategoryAttributeValue;
import org.springblade.mall.mapper.CategoryAttributeMapper;
import org.springblade.mall.mapper.CategoryAttributeValueMapper;
import org.springblade.mall.mapper.MallCategoryMapper;
import org.springblade.mall.service.CategoryAttributeService;
import org.springblade.mall.vo.CategoryAttributeVO;
import org.springblade.mall.vo.CategoryAttributeValueVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类属性服务实现类
 */
@Service
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    @Autowired
    private CategoryAttributeMapper categoryAttributeMapper;

    @Autowired
    private CategoryAttributeValueMapper categoryAttributeValueMapper;

    @Autowired
    private MallCategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryAttributeVO createAttribute(CategoryAttributeDTO dto) {
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        CategoryAttribute attribute = new CategoryAttribute();
        BeanUtils.copyProperties(dto, attribute);
        attribute.setCreatedAt(LocalDateTime.now());
        attribute.setUpdatedAt(LocalDateTime.now());

        categoryAttributeMapper.insert(attribute);
        return convertToVO(attribute);
    }

    @Override
    @Transactional
    public CategoryAttributeVO updateAttribute(Long id, CategoryAttributeDTO dto) {
        CategoryAttribute attribute = categoryAttributeMapper.selectById(id);
        if (attribute == null) {
            throw new RuntimeException("分类属性不存在");
        }

        BeanUtils.copyProperties(dto, attribute, "id", "createdAt");
        attribute.setUpdatedAt(LocalDateTime.now());

        categoryAttributeMapper.updateById(attribute);
        return convertToVO(attribute);
    }

    @Override
    @Transactional
    public void deleteAttribute(Long id) {
        QueryWrapper<CategoryAttributeValue> valueWrapper = new QueryWrapper<>();
        valueWrapper.eq("attribute_id", id);
        categoryAttributeValueMapper.delete(valueWrapper);

        categoryAttributeMapper.deleteById(id);
    }

    @Override
    public CategoryAttributeVO getAttributeById(Long id) {
        CategoryAttribute attribute = categoryAttributeMapper.selectById(id);
        if (attribute == null) {
            return null;
        }
        return convertToVO(attribute);
    }

    @Override
    public List<CategoryAttributeVO> getAttributesByCategoryId(Long categoryId) {
        QueryWrapper<CategoryAttribute> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        wrapper.orderByAsc("sort_order");

        List<CategoryAttribute> attributes = categoryAttributeMapper.selectList(wrapper);
        return attributes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<CategoryAttributeVO> getAttributePage(Long categoryId, int page, int pageSize) {
        Page<CategoryAttribute> pageParam = new Page<>(page, pageSize);
        
        QueryWrapper<CategoryAttribute> wrapper = new QueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByAsc("sort_order");

        IPage<CategoryAttribute> result = categoryAttributeMapper.selectPage(pageParam, wrapper);
        
        Page<CategoryAttributeVO> voPage = new Page<>(page, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    @Transactional
    public CategoryAttributeValueVO addAttributeValue(CategoryAttributeValueDTO dto) {
        CategoryAttribute attribute = categoryAttributeMapper.selectById(dto.getAttributeId());
        if (attribute == null) {
            throw new RuntimeException("分类属性不存在");
        }

        CategoryAttributeValue value = new CategoryAttributeValue();
        BeanUtils.copyProperties(dto, value);
        value.setCreatedAt(LocalDateTime.now());

        categoryAttributeValueMapper.insert(value);
        return convertToValueVO(value);
    }

    @Override
    @Transactional
    public CategoryAttributeValueVO updateAttributeValue(Long id, CategoryAttributeValueDTO dto) {
        CategoryAttributeValue value = categoryAttributeValueMapper.selectById(id);
        if (value == null) {
            throw new RuntimeException("属性值不存在");
        }

        BeanUtils.copyProperties(dto, value, "id", "createdAt");
        categoryAttributeValueMapper.updateById(value);
        
        return convertToValueVO(value);
    }

    @Override
    @Transactional
    public void deleteAttributeValue(Long id) {
        categoryAttributeValueMapper.deleteById(id);
    }

    @Override
    public List<CategoryAttributeValueVO> getAttributeValues(Long attributeId) {
        QueryWrapper<CategoryAttributeValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attribute_id", attributeId);
        wrapper.orderByAsc("sort_order");

        List<CategoryAttributeValue> values = categoryAttributeValueMapper.selectList(wrapper);
        return values.stream()
                .map(this::convertToValueVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<CategoryAttributeValueVO> batchAddAttributeValues(Long attributeId, List<CategoryAttributeValueDTO> values) {
        return values.stream()
                .map(dto -> {
                    dto.setAttributeId(attributeId);
                    return addAttributeValue(dto);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void copyAttributesToCategory(Long sourceCategoryId, Long targetCategoryId) {
        List<CategoryAttributeVO> sourceAttributes = getAttributesByCategoryId(sourceCategoryId);
        
        for (CategoryAttributeVO sourceAttr : sourceAttributes) {
            CategoryAttributeDTO attrDTO = new CategoryAttributeDTO();
            attrDTO.setCategoryId(targetCategoryId);
            attrDTO.setName(sourceAttr.getName());
            attrDTO.setType(sourceAttr.getType());
            attrDTO.setIsRequired(sourceAttr.getIsRequired());
            attrDTO.setIsSearchable(sourceAttr.getIsSearchable());
            attrDTO.setSortOrder(sourceAttr.getSortOrder());
            
            CategoryAttributeVO newAttr = createAttribute(attrDTO);
            
            if (sourceAttr.getValues() != null) {
                List<CategoryAttributeValueDTO> valueDTOs = sourceAttr.getValues().stream()
                        .map(valueVO -> {
                            CategoryAttributeValueDTO valueDTO = new CategoryAttributeValueDTO();
                            valueDTO.setAttributeId(newAttr.getId());
                            valueDTO.setValue(valueVO.getValue());
                            valueDTO.setSortOrder(valueVO.getSortOrder());
                            return valueDTO;
                        })
                        .collect(Collectors.toList());
                
                batchAddAttributeValues(newAttr.getId(), valueDTOs);
            }
        }
    }

    private CategoryAttributeVO convertToVO(CategoryAttribute attribute) {
        CategoryAttributeVO vo = new CategoryAttributeVO();
        BeanUtils.copyProperties(attribute, vo);
        
        switch (attribute.getType()) {
            case 1:
                vo.setTypeText("单选");
                break;
            case 2:
                vo.setTypeText("多选");
                break;
            case 3:
                vo.setTypeText("文本输入");
                break;
            case 4:
                vo.setTypeText("数字输入");
                break;
            case 5:
                vo.setTypeText("日期");
                break;
            default:
                vo.setTypeText("未知");
        }
        
        List<CategoryAttributeValueVO> values = getAttributeValues(attribute.getId());
        vo.setValues(values);
        
        return vo;
    }

    private CategoryAttributeValueVO convertToValueVO(CategoryAttributeValue value) {
        CategoryAttributeValueVO vo = new CategoryAttributeValueVO();
        BeanUtils.copyProperties(value, vo);
        return vo;
    }
}



