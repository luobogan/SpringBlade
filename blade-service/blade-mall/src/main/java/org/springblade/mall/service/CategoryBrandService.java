package org.springblade.mall.service;

import org.springblade.mall.entity.Brand;
import java.util.List;

public interface CategoryBrandService {

    List<Brand> getBrandsByCategoryId(Long categoryId);

    void saveCategoryBrands(Long categoryId, List<Long> brandIds);

    void removeBrandFromCategory(Long categoryId, Long brandId);
}



