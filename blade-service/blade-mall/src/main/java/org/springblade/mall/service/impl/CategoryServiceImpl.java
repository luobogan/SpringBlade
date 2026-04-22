package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.mall.dto.CategoryDTO;
import org.springblade.mall.entity.Category;
import org.springblade.mall.mapper.MallCategoryMapper;
import org.springblade.mall.service.CategoryService;
import org.springblade.mall.utils.FileUploadUtil;
import org.springblade.mall.vo.CategoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private MallCategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryVO createCategory(CategoryDTO categoryDTO) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();

        // 验证 parentId - 如果为0或负数，设置为null（顶级分类）
        Long parentId = categoryDTO.getParentId();
        if (parentId != null && parentId <= 0) {
            parentId = null;
        }

        // 创建分类实体
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setParentId(parentId);
        category.setTenantId(tenantId);

        // 处理图片上传 - 将 base64 转换为文件路径
        if (categoryDTO.getBanner() != null && categoryDTO.getBanner().startsWith("data:image")) {
            String bannerPath = FileUploadUtil.uploadBase64Image(categoryDTO.getBanner(), "category");
            category.setBanner(bannerPath);
        }
        if (categoryDTO.getImage() != null && categoryDTO.getImage().startsWith("data:image")) {
            String imagePath = FileUploadUtil.uploadBase64Image(categoryDTO.getImage(), "category");
            category.setImage(imagePath);
        }

        // 处理字段映射差异
        category.setSortOrder(categoryDTO.getSort());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        // 保存分类
        categoryMapper.insert(category);

        // 转换为VO返回
        return convertToVO(category);
    }

    @Override
    @Transactional
    public CategoryVO updateCategory(Long id, CategoryDTO categoryDTO) {

        // 获取分类
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Category category = categoryMapper.selectOne(queryWrapper);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 处理图片上传 - 将 base64 转换为文件路径
        if (categoryDTO.getBanner() != null && categoryDTO.getBanner().startsWith("data:image")) {
            // 删除旧图片
            if (category.getBanner() != null) {
                FileUploadUtil.deleteFile(category.getBanner());
            }
            String bannerPath = FileUploadUtil.uploadBase64Image(categoryDTO.getBanner(), "category");
            category.setBanner(bannerPath);
        } else if (categoryDTO.getBanner() != null && !categoryDTO.getBanner().equals(category.getBanner())) {
            // 如果传了新的路径，但不是 base64，直接更新
            category.setBanner(categoryDTO.getBanner());
        }

        if (categoryDTO.getImage() != null && categoryDTO.getImage().startsWith("data:image")) {
            // 删除旧图片
            if (category.getImage() != null) {
                FileUploadUtil.deleteFile(category.getImage());
            }
            String imagePath = FileUploadUtil.uploadBase64Image(categoryDTO.getImage(), "category");
            category.setImage(imagePath);
        } else if (categoryDTO.getImage() != null && !categoryDTO.getImage().equals(category.getImage())) {
            category.setImage(categoryDTO.getImage());
        }

        // 更新其他字段
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        // 验证 parentId - 如果为0或负数，设置为null（顶级分类）
        Long parentId = categoryDTO.getParentId();
        if (parentId != null && parentId <= 0) {
            parentId = null;
        }
        category.setParentId(parentId);
        category.setIcon(categoryDTO.getIcon());
        category.setSortOrder(categoryDTO.getSort());
        category.setStatus(categoryDTO.getStatus());
        category.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        categoryMapper.updateById(category);

        // 如果分类被禁用，递归禁用所有子分类
        if (categoryDTO.getStatus() != null && categoryDTO.getStatus() == 0) {
            disableChildCategories(id);
        }

        // 转换为VO返回
        return convertToVO(category);
    }

    /**
     * 递归禁用所有子分类
     * @param parentId 父分类ID
     */
    private void disableChildCategories(Long parentId) {

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        List<Category> subCategories = categoryMapper.selectList(queryWrapper);

        for (Category subCategory : subCategories) {
            // 禁用子分类 - 注意：Category实体已移除status字段
            categoryMapper.updateById(subCategory);

            // 递归禁用子分类的子分类
            disableChildCategories(subCategory.getId());
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {

        // 检查分类是否存在
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Category category = categoryMapper.selectOne(queryWrapper);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查是否有子分类
        QueryWrapper<Category> subCategoryQuery = new QueryWrapper<>();
        subCategoryQuery.eq("parent_id", id);
        List<Category> subCategories = categoryMapper.selectList(subCategoryQuery);
        if (!subCategories.isEmpty()) {
            throw new RuntimeException("该分类下有子分类，无法删除");
        }

        // 删除分类
        categoryMapper.deleteById(id);
    }

    @Override
    public CategoryVO getCategoryById(Long id) {

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Category category = categoryMapper.selectOne(queryWrapper);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return convertToVO(category);
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (org.springblade.core.tool.utils.StringUtil.isBlank(tenantId)) {
            System.out.println("=== 获取所有分类 ===");
            System.out.println("租户ID为空，抛出异常");
            throw new RuntimeException("租户ID不能为空");
        }

        System.out.println("=== 获取所有分类 ===");
        System.out.println("使用的租户ID: " + tenantId);
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        System.out.println("获取到的分类数量: " + categories.size());
        for (Category category : categories) {
            System.out.println("分类ID: " + category.getId() + ", 名称: " + category.getName() + ", 父分类ID: " + category.getParentId());
        }
        return buildCategoryTree(categories);
    }

    /**
     * 获取所有分类（包括禁用状态）
     * 用于管理员系统
     * @return 分类树
     */
    public List<CategoryVO> getAllCategoriesWithStatus() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (org.springblade.core.tool.utils.StringUtil.isBlank(tenantId)) {
            System.out.println("=== 获取所有分类（包括禁用状态）===");
            System.out.println("租户ID为空，抛出异常");
            throw new RuntimeException("租户ID不能为空");
        }

        System.out.println("=== 获取所有分类（包括禁用状态）===");
        System.out.println("使用的租户ID: " + tenantId);
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        System.out.println("获取到的分类数量: " + categories.size());
        for (Category category : categories) {
            System.out.println("分类ID: " + category.getId() + ", 名称: " + category.getName() + ", 父分类ID: " + category.getParentId());
        }
        return buildCategoryTree(categories);
    }

    @Override
    public List<CategoryVO> getSubCategories(Long parentId) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (org.springblade.core.tool.utils.StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取子分类（包括禁用状态）
     * 用于管理员系统
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    public List<CategoryVO> getSubCategoriesWithStatus(Long parentId) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (org.springblade.core.tool.utils.StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getActiveCategories() {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 如果租户ID为空，抛出异常
        if (org.springblade.core.tool.utils.StringUtil.isBlank(tenantId)) {
            throw new RuntimeException("租户ID不能为空");
        }

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        return buildCategoryTree(categories);
    }

    /**
     * 将分类实体转换为VO
     * @param category 分类实体
     * @return 分类VO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        BeanUtils.copyProperties(category, categoryVO);
        // 处理字段映射差异
        categoryVO.setSort(category.getSortOrder());
        return categoryVO;
    }

    /**
     * 构建分类树
     * @param categories 所有分类
     * @return 分类树
     */
    private List<CategoryVO> buildCategoryTree(List<Category> categories) {
        // 转换所有分类为VO
        List<CategoryVO> categoryVOs = categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分类映射
        Map<Long, CategoryVO> categoryMap = categoryVOs.stream()
                .collect(Collectors.toMap(CategoryVO::getId, categoryVO -> categoryVO));

        // 构建分类树
        List<CategoryVO> rootCategories = new ArrayList<>();
        for (CategoryVO categoryVO : categoryVOs) {
            if (categoryVO.getParentId() == null || categoryVO.getParentId() == 0) {
                // 根分类
                rootCategories.add(categoryVO);
            } else {
                // 子分类
                CategoryVO parentVO = categoryMap.get(categoryVO.getParentId());
                if (parentVO != null) {
                    if (parentVO.getChildren() == null) {
                        parentVO.setChildren(new ArrayList<>());
                    }
                    parentVO.getChildren().add(categoryVO);
                }
            }
        }

        return rootCategories;
    }

    @Override
    @Transactional
    public void updateCategorySort(List<org.springblade.mall.service.CategoryService.SortRequest> sortData) {

        for (org.springblade.mall.service.CategoryService.SortRequest sortItem : sortData) {
            // 获取分类
            QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", sortItem.getId());
            Category category = categoryMapper.selectOne(queryWrapper);
            if (category != null) {
                // 更新排序
                category.setSortOrder(sortItem.getSort());
                category.setUpdatedAt(LocalDateTime.now());
                categoryMapper.updateById(category);
            }
        }
    }
}



