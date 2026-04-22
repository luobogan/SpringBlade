package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.mall.dto.BrandDTO;
import org.springblade.mall.entity.Brand;
import org.springblade.mall.mapper.BrandMapper;
import org.springblade.mall.service.BrandService;
import org.springblade.mall.vo.BrandVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 品牌服务实现类
 */
@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandVO createBrand(BrandDTO brandDTO) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 检查品牌名称是否已存在
        QueryWrapper<Brand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", brandDTO.getName());
        Brand existingBrand = brandMapper.selectOne(queryWrapper);
        if (existingBrand != null) {
            throw new RuntimeException("品牌名称已存在");
        }

        // 创建品牌实体
        Brand brand = new Brand();
        BeanUtils.copyProperties(brandDTO, brand);
        brand.setStatus(brandDTO.getStatus() != null ? brandDTO.getStatus() : 1);
        brand.setTenantId(tenantId);

        // 保存品牌
        brandMapper.insert(brand);

        // 转换为VO返回
        return convertToVO(brand);
    }

    @Override
    @Transactional
    public BrandVO updateBrand(Long id, BrandDTO brandDTO) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        // 获取品牌
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new RuntimeException("品牌不存在");
        }

        // 检查品牌名称是否已存在（排除当前品牌）
        QueryWrapper<Brand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", brandDTO.getName());
        queryWrapper.ne("id", id);
        Brand existingBrand = brandMapper.selectOne(queryWrapper);
        if (existingBrand != null) {
            throw new RuntimeException("品牌名称已存在");
        }

        // 更新品牌信息
        BeanUtils.copyProperties(brandDTO, brand);
        if (brandDTO.getStatus() != null) {
            brand.setStatus(brandDTO.getStatus());
        }

        // 保存更新
        brandMapper.updateById(brand);

        // 转换为VO返回
        return convertToVO(brand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        // 检查品牌是否存在
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new RuntimeException("品牌不存在");
        }

        // 删除品牌
        brandMapper.deleteById(id);
    }

    @Override
    public BrandVO getBrandById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new RuntimeException("品牌不存在");
        }
        return convertToVO(brand);
    }

    @Override
    public List<BrandVO> getAllBrands() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，直接返回空列表
        if (StringUtil.isBlank(tenantId)) {
            return new ArrayList<>();
        }

        QueryWrapper<Brand> queryWrapper = new QueryWrapper<>();
        List<Brand> brands = brandMapper.selectList(queryWrapper);
        return brands.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BrandVO> getActiveBrands() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，直接返回空列表
        if (StringUtil.isBlank(tenantId)) {
            return new ArrayList<>();
        }

        QueryWrapper<Brand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        List<Brand> brands = brandMapper.selectList(queryWrapper);
        return brands.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将品牌实体转换为VO
     * @param brand 品牌实体
     * @return 品牌VO
     */
    private BrandVO convertToVO(Brand brand) {
        BrandVO brandVO = new BrandVO();
        BeanUtils.copyProperties(brand, brandVO);
        return brandVO;
    }
}



