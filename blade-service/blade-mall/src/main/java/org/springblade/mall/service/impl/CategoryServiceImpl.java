package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.mall.dto.CategoryDTO;
import org.springblade.mall.entity.Category;
import org.springblade.mall.entity.ImageFile;
import org.springblade.mall.mapper.MallCategoryMapper;
import org.springblade.mall.mapper.ImageFileMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private MallCategoryMapper categoryMapper;

    @Autowired
    private ImageFileMapper imageFileMapper;

    @Override
    @Transactional
    public CategoryVO createCategory(CategoryDTO categoryDTO) {
        // 优先使用 DTO 中的 tenantId（000000租户可以选择其他租户），否则从 TenantUtil 获取
        String tenantId = org.springblade.core.tool.utils.StringUtil.isNotBlank(categoryDTO.getTenantId())
            ? categoryDTO.getTenantId()
            : TenantUtil.getTenantId();

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

        // 处理图片ID - 从DTO中获取图片ID
        if (categoryDTO.getBannerId() != null) {
            category.setBannerId(categoryDTO.getBannerId());
        }
        if (categoryDTO.getImageId() != null) {
            category.setImageId(categoryDTO.getImageId());
        }
        if (categoryDTO.getIconId() != null) {
            category.setIconId(categoryDTO.getIconId());
        }

        // 处理字段映射差异
        category.setSortOrder(categoryDTO.getSort());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        // 保存分类（MyBatis-Plus 拦截器会自动处理租户隔离）
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

        // 处理图片ID - 从DTO中获取图片ID
        if (categoryDTO.getBannerId() != null) {
            category.setBannerId(categoryDTO.getBannerId());
        }
        if (categoryDTO.getImageId() != null) {
            category.setImageId(categoryDTO.getImageId());
        }
        if (categoryDTO.getIconId() != null) {
            category.setIconId(categoryDTO.getIconId());
        }

        // 更新其他字段
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        // 验证 parentId - 如果为0或负数，设置为null（顶级分类）
        Long parentIds = categoryDTO.getParentId();
        if (parentIds != null && parentIds <= 0) {
            parentIds = null;
        }
        category.setParentId(parentIds);
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

        String tenantId = TenantUtil.getTenantId();
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("tenant_id", tenantId);
        Category category = categoryMapper.selectOne(queryWrapper);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return convertToVO(category);
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        log.info("=== 获取所有分类 ===");
        String tenantId = TenantUtil.getTenantId();
        log.info("使用的租户ID: {}", tenantId);
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId);
        queryWrapper.eq("is_deleted", 0);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        log.info("获取到的分类数量: {}", categories.size());
        for (Category category : categories) {
            log.info("分类ID: {}, 名称: {}, 父分类ID: {}", category.getId(), category.getName(), category.getParentId());
        }
        return buildCategoryTree(categories);
    }

    /**
     * 获取所有分类（包括禁用状态）
     * 用于管理员系统
     * 000000租户获取所有分类，普通租户只获取自己的分类
     * @param tenantId 租户ID，null表示不隔离租户
     * @return 分类树
     */
    public List<CategoryVO> getAllCategoriesWithStatus(String tenantId) {
        log.info("=== 获取所有分类（包括禁用状态）===");
        log.info("传入的租户ID: {}", tenantId);
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();

        // 根据租户ID过滤
        if (tenantId != null) {
            queryWrapper.eq("tenant_id", tenantId);
        }
        queryWrapper.eq("is_deleted", 0);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        log.info("获取到的分类数量: {}", categories.size());
        for (Category category : categories) {
            log.info("分类ID: {}, 名称: {}, 父分类ID: {}, 租户ID: {}", category.getId(), category.getName(), category.getParentId(), category.getTenantId());
        }
        return buildCategoryTree(categories);
    }

    /**
     * 获取所有分类（包括禁用状态）
     * 用于管理员系统
     * @return 分类树
     */
    public List<CategoryVO> getAllCategoriesWithStatus() {
        return getAllCategoriesWithStatus(TenantUtil.getTenantId());
    }

    /**
     * 获取所有分类（按租户ID分组）
     * @param tenantId 租户ID，null表示不隔离租户
     * @return 按租户分组的分类列表
     */
    public List<CategoryVO> getAllCategoriesGroupedByTenant(String tenantId) {
        log.info("=== 获取所有分类（按租户分组）===");
        log.info("传入的租户ID: {}", tenantId);

        // 只有000000租户或null租户可以查看所有分类
        if (tenantId != null && !"000000".equals(tenantId)) {
            return getAllCategoriesWithStatus(tenantId);
        }

        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.orderByAsc("tenant_id", "sort");
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        log.info("获取到的分类数量: {}", categories.size());

        // 按租户ID分组
        Map<String, List<Category>> categoriesByTenant = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getTenantId() != null ? c.getTenantId() : "default"));

        // 构建带租户分组的分类树
        List<CategoryVO> result = new ArrayList<>();
        categoriesByTenant.forEach((tid, catList) -> {
            CategoryVO tenantGroup = new CategoryVO();
            // 使用租户ID+特殊后缀确保唯一性
            tenantGroup.setId(Long.parseLong(tid + "999"));
            tenantGroup.setName("租户: " + tid);
            tenantGroup.setTenantId(tid);
            tenantGroup.setTenantGroup(true);  // 标识为租户分组，不可操作
            tenantGroup.setChildren(buildCategoryTree(catList));
            result.add(tenantGroup);
        });

        return result;
    }

    /**
     * 获取所有分类（按租户ID分组）
     * 用于000000租户显示租户分组
     * @return 按租户分组的分类列表
     */
    public List<CategoryVO> getAllCategoriesGroupedByTenant() {
        return getAllCategoriesGroupedByTenant(TenantUtil.getTenantId());
    }

    @Override
    public List<CategoryVO> getSubCategories(Long parentId) {
        String tenantId = TenantUtil.getTenantId();
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("tenant_id", tenantId);
        queryWrapper.eq("is_deleted", 0);
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
        String tenantId = TenantUtil.getTenantId();
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("tenant_id", tenantId);
        queryWrapper.eq("is_deleted", 0);
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getActiveCategories() {
        String tenantId = TenantUtil.getTenantId();
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.eq("status", 1);
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

        // 查询并填充图片信息
        if (category.getIconId() != null) {
            categoryVO.setIconInfo(buildImageFileInfo(category.getIconId()));
        }
        if (category.getImageId() != null) {
            categoryVO.setImageInfo(buildImageFileInfo(category.getImageId()));
        }
        if (category.getBannerId() != null) {
            categoryVO.setBannerInfo(buildImageFileInfo(category.getBannerId()));
        }

        return categoryVO;
    }

    /**
     * 构建图片信息对象
     * @param imageFileId 图片文件ID
     * @return 图片信息
     */
    private CategoryVO.ImageFileInfo buildImageFileInfo(Long imageFileId) {
        try {
            ImageFile imageFile = imageFileMapper.selectById(imageFileId);
            if (imageFile != null) {
                CategoryVO.ImageFileInfo info = new CategoryVO.ImageFileInfo();
                info.setId(imageFile.getImagefileid());
                info.setFilename(imageFile.getImagefilename());
                info.setUrl(buildImageUrl(imageFile));
                info.setFilesize(imageFile.getFilesize());
                info.setFiletype(imageFile.getImagefiletype());
                info.setIszip(imageFile.getIszip());
                info.setEncrypt(imageFile.getIsaesencrypt() != null && imageFile.getIsaesencrypt() == 1);
                return info;
            }
        } catch (Exception e) {
            log.warn("查询图片信息失败, imageFileId={}: {}", imageFileId, e.getMessage());
        }
        return null;
    }

    /**
     * 构建图片访问URL
     * @param imageFile 图片文件实体
     * @return 访问URL
     */
    private String buildImageUrl(ImageFile imageFile) {
        if (imageFile == null || imageFile.getImagefileid() == null) {
            return null;
        }
        // 构建文件下载URL：/api/file/download/{imagefileid}
        return "/api/blade-mall/file/download/" + imageFile.getImagefileid();
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
