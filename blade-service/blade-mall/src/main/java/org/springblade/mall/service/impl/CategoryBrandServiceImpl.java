package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springblade.mall.entity.Brand;
import org.springblade.mall.entity.CategoryBrand;
import org.springblade.mall.mapper.BrandMapper;
import org.springblade.mall.mapper.CategoryBrandMapper;
import org.springblade.mall.service.CategoryBrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryBrandServiceImpl implements CategoryBrandService {

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Override
    public List<Brand> getBrandsByCategoryId(Long categoryId) {
        LambdaQueryWrapper<CategoryBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryBrand::getCategoryId, categoryId);
        List<CategoryBrand> categoryBrands = categoryBrandMapper.selectList(wrapper);

        if (categoryBrands.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> brandIds = categoryBrands.stream()
                .map(CategoryBrand::getBrandId)
                .collect(Collectors.toList());

        return brandMapper.selectBatchIds(brandIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategoryBrands(Long categoryId, List<Long> brandIds) {
        LambdaQueryWrapper<CategoryBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryBrand::getCategoryId, categoryId);
        categoryBrandMapper.delete(wrapper);

        if (brandIds != null && !brandIds.isEmpty()) {
            for (Long brandId : brandIds) {
                CategoryBrand categoryBrand = new CategoryBrand();
                categoryBrand.setCategoryId(categoryId);
                categoryBrand.setBrandId(brandId);
                categoryBrand.setSortOrder(0);
                categoryBrand.setCreatedAt(LocalDateTime.now());
                categoryBrandMapper.insert(categoryBrand);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBrandFromCategory(Long categoryId, Long brandId) {
        LambdaQueryWrapper<CategoryBrand> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryBrand::getCategoryId, categoryId);
        wrapper.eq(CategoryBrand::getBrandId, brandId);
        categoryBrandMapper.delete(wrapper);
    }
}



