package org.springblade.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.mall.dto.ProductDTO;
import org.springblade.mall.dto.ProductQueryDTO;
import org.springblade.mall.dto.ProductSkuDTO;
import org.springblade.mall.dto.ProductSpecificationDTO;
import org.springblade.mall.dto.ProductSpecAttributeDTO;
import org.springblade.mall.dto.ProductSpecValueDTO;
import org.springblade.mall.dto.SkuMatrixGenerateDTO;
import org.springblade.mall.dto.ProductAttributeValueDTO;
import org.springblade.mall.dto.ProductParamDTO;
import org.springblade.mall.dto.ProductAlbumImageDTO;
import org.springblade.mall.entity.*;
import org.springblade.mall.mapper.MallCategoryMapper;
import org.springblade.mall.mapper.*;
import org.springblade.mall.service.ProductService;
import org.springblade.mall.vo.ProductSpecificationVO;
import org.springblade.mall.vo.ProductSkuVO;
import org.springblade.mall.vo.ProductVO;
import org.springblade.mall.vo.ProductSpecAttributeVO;
import org.springblade.mall.vo.ProductSpecValueVO;
import org.springblade.mall.vo.SkuStockLogVO;
import org.springblade.mall.vo.ProductAlbumImageVO;
import org.springblade.mall.utils.SkuMatrixGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 商品服务实现类 - 增强版
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductColorMapper productColorMapper;

    @Autowired
    private ProductSizeMapper productSizeMapper;

    @Autowired
    private ProductSpecificationMapper productSpecificationMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ProductAttributeValueMapper productAttributeValueMapper;

    @Autowired
    private ProductParamMapper productParamMapper;

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private MallCategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private ProductSpecAttributeMapper productSpecAttributeMapper;

    @Autowired
    private ProductSpecValueMapper productSpecValueMapper;

    @Value("${blade.prop.upload-domain:http://127.0.0.1:81}")
    private String uploadDomain;

    @Override
    @Transactional
    public ProductVO createProduct(ProductDTO productDTO) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(productDTO.getCategoryId());
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查品牌是否存在
        Brand brand = brandMapper.selectById(productDTO.getBrandId());
        if (brand == null) {
            throw new RuntimeException("品牌不存在");
        }

        // 验证价格范围
        if (productDTO.getPrice() != null) {
            if (productDTO.getPrice().compareTo(new BigDecimal("99999999.99")) > 0) {
                throw new RuntimeException("价格不能超过99999999.99");
            }
        }

        // 验证原价范围
        if (productDTO.getOriginalPrice() != null) {
            if (productDTO.getOriginalPrice().compareTo(new BigDecimal("99999999.99")) > 0) {
                throw new RuntimeException("原价不能超过99999999.99");
            }
        }

        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();

        // 创建商品实体
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        // 设置租户ID
        product.setTenantId(tenantId);
        // 调试：检查富文本内容
        if (product.getDetailDescription() != null) {
            log.debug("=== 准备保存富文本内容调试 ===");
            log.debug("保存前 detailDescription 类型: {}", product.getDetailDescription().getClass().getName());
            log.debug("保存前 detailDescription 长度: {}", product.getDetailDescription().length());
            log.debug("保存前 detailDescription 前200字符: {}", product.getDetailDescription().substring(0, Math.min(200, product.getDetailDescription().length())));
            log.debug("保存前 detailDescription 是否包含 <html: {}", product.getDetailDescription().contains("<html"));
            log.debug("保存前 detailDescription 是否包含 <body: {}", product.getDetailDescription().contains("<body"));
            log.debug("保存前 detailDescription 是否包含 <p: {}", product.getDetailDescription().contains("<p"));
            log.debug("保存前 detailDescription 是否包含 <div: {}", product.getDetailDescription().contains("<div"));
        }
        product.setIsNew(productDTO.getIsNew() != null ? productDTO.getIsNew() : 0);
        product.setIsHot(productDTO.getIsHot() != null ? productDTO.getIsHot() : 0);
        product.setIsRecommend(productDTO.getIsRecommend() != null ? productDTO.getIsRecommend() : 0);
        product.setStatus(productDTO.getStatus() != null ? productDTO.getStatus() : 1);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // 保存商品
        log.info("=== 准备保存商品 ===");
        log.info("商品对象: {}", product);
        productMapper.insert(product);
        log.info("=== 商品保存成功 ===");
        log.info("商品ID: {}", product.getId());

        // 保存商品图片
        saveProductImages(product.getId(), productDTO.getImages());

        // 保存商品颜色
        saveProductColors(product.getId(), productDTO.getColors());

        // 保存商品尺寸
        saveProductSizes(product.getId(), productDTO.getSizes());

        // 保存商品规格
        saveProductSpecifications(product.getId(), productDTO.getSpecifications());

        // 保存商品SKU
        saveProductSkus(product.getId(), productDTO.getSkus());

        // 保存商品属性值
        saveProductAttributeValues(product.getId(), productDTO.getAttributeValues());

        // 保存商品参数值
        saveProductParamValues(product.getId(), productDTO.getParamValues());

        // 保存关联商品
        saveProductRelations(product.getId(), productDTO.getRelatedProducts(), productDTO.getBundleProducts(), productDTO.getRecommendProducts());

        // 保存商品相册图片
        saveProductAlbumImages(product.getId(), productDTO.getAlbumImages());

        // 保存商品规格属性
        if (productDTO.getSpecAttributes() != null && !productDTO.getSpecAttributes().isEmpty()) {
            saveProductSpecAttributes(product.getId(), productDTO.getSpecAttributes());
        }

        // 转换为VO返回
        return convertToVO(product);
    }

    @Override
    @Transactional
    public ProductVO updateProduct(Long id, ProductDTO productDTO) {
        return updateProduct(id, productDTO, null);
    }

    @Override
    @Transactional
    public ProductVO updateProduct(Long id, ProductDTO productDTO, Boolean confirmEdit) {

        // 获取商品
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查分类是否存在
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("id", productDTO.getCategoryId());
        Category category = categoryMapper.selectOne(categoryQuery);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查品牌是否存在
        QueryWrapper<Brand> brandQuery = new QueryWrapper<>();
        brandQuery.eq("id", productDTO.getBrandId());
        Brand brand = brandMapper.selectOne(brandQuery);
        if (brand == null) {
            throw new RuntimeException("品牌不存在");
        }

        // 编辑限制：已上架商品编辑需要二次确认
        if (product.getStatus() == 1 && (confirmEdit == null || !confirmEdit)) {
            throw new RuntimeException("商品已上架，编辑需要二次确认");
        }

        // 更新商品信息（保护不可编辑的字段）
        Integer originalSales = product.getSales();
        Integer originalViewCount = product.getViewCount();
        Integer originalFavoriteCount = product.getFavoriteCount();
        BigDecimal originalUserRating = product.getUserRating();
        Integer originalUserReviewCount = product.getUserReviewCount();

        BeanUtils.copyProperties(productDTO, product);

        // 恢复不可编辑的字段
        product.setSales(originalSales);
        product.setViewCount(originalViewCount);
        product.setFavoriteCount(originalFavoriteCount);
        product.setUserRating(originalUserRating);
        product.setUserReviewCount(originalUserReviewCount);

        product.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        productMapper.updateById(product);

        // 删除旧的关联数据
        deleteProductImages(id);
        deleteProductColors(id);
        deleteProductSizes(id);
        deleteProductSpecifications(id);
        deleteProductSkus(id);
        deleteProductAttributeValues(id);
        deleteProductParamValues(id);
        deleteProductRelations(id);
        deleteProductAlbumImages(id);
        deleteProductSpecAttributes(id);

        // 保存新的关联数据
        saveProductImages(id, productDTO.getImages());
        saveProductColors(id, productDTO.getColors());
        saveProductSizes(id, productDTO.getSizes());
        saveProductSpecifications(id, productDTO.getSpecifications());
        saveProductSkus(id, productDTO.getSkus());
        saveProductAttributeValues(id, productDTO.getAttributeValues());
        saveProductParamValues(id, productDTO.getParamValues());
        saveProductRelations(id, productDTO.getRelatedProducts(), productDTO.getBundleProducts(), productDTO.getRecommendProducts());
        saveProductAlbumImages(id, productDTO.getAlbumImages());
        if (productDTO.getSpecAttributes() != null && !productDTO.getSpecAttributes().isEmpty()) {
            saveProductSpecAttributes(id, productDTO.getSpecAttributes());
        }

        // 转换为VO返回
        return convertToVO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        // 检查商品是否存在
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 使用自定义的deleteProduct方法，直接更新删除字段
        int rows = productMapper.deleteProduct(id, LocalDateTime.now(), SecureUtil.getTenantId());
        if (rows == 0) {
            throw new RuntimeException("商品删除失败");
        }
    }

    @Override
    @Transactional
    public void restoreProduct(Long id) {
        // 使用自定义的restoreProduct方法，绕过@TableLogic限制
        int rows = productMapper.restoreProduct(id, LocalDateTime.now(), SecureUtil.getTenantId());
        if (rows == 0) {
            throw new RuntimeException("商品不存在或未被删除");
        }
    }

    @Override
    public List<ProductVO> getDeletedProducts() {
        // 使用自定义的selectDeletedProducts方法，绕过@TableLogic限制
        List<Product> products = productMapper.selectDeletedProducts(SecureUtil.getTenantId());
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVO getProductById(Long id) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("is_deleted", 0);
        // 移除status=1的限制，允许编辑任何状态的商品
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            // 调试信息
            log.warn("=== 商品查询调试 ===");
            log.warn("查询商品ID: {}", id);
            log.warn("当前租户ID: {}", SecureUtil.getTenantId());
            log.warn("商品不存在");
            throw new RuntimeException("商品不存在");
        }
        // 调试：检查富文本内容
        if (product.getDetailDescription() != null) {
            log.debug("=== 数据库富文本内容调试 ===");
            log.debug("数据库中 detailDescription 类型: {}", product.getDetailDescription().getClass().getName());
            log.debug("数据库中 detailDescription 长度: {}", product.getDetailDescription().length());
            log.debug("数据库中 detailDescription 前200字符: {}", product.getDetailDescription().substring(0, Math.min(200, product.getDetailDescription().length())));
            log.debug("数据库中 detailDescription 是否包含 <html: {}", product.getDetailDescription().contains("<html"));
            log.debug("数据库中 detailDescription 是否包含 <body: {}", product.getDetailDescription().contains("<body"));
            log.debug("数据库中 detailDescription 是否包含 <p: {}", product.getDetailDescription().contains("<p"));
            log.debug("数据库中 detailDescription 是否包含 <div: {}", product.getDetailDescription().contains("<div"));
        }
        return convertToVO(product);
    }

    @Override
    public List<ProductVO> getAllProducts() {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> getActiveProducts() {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> getFeaturedProducts() {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_recommend", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> getNewProducts() {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_new", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Integer> activeCategoryIds = activeCategories.stream()
                .map(category -> category.getId().intValue())
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> getProductsByCategory(Long categoryId) {

        List<Long> categoryIds = getCategoryAndChildrenIds(categoryId);

        // 筛选出启用状态的分类
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.in("id", categoryIds);
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (activeCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("category_id", activeCategoryIds);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取分类及其所有子类的ID列表
     * @param categoryId 分类ID
     * @return 分类及其所有子类的ID列表
     */
    private List<Long> getCategoryAndChildrenIds(Long categoryId) {
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);

        // 递归获取所有子分类（只考虑启用状态的分类）
        getChildCategoryIds(categoryId, categoryIds);

        return categoryIds;
    }

    /**
     * 递归获取子分类ID
     * @param parentId 父分类ID
     * @param categoryIds 分类ID列表，用于存储结果
     */
    private void getChildCategoryIds(Long parentId, List<Long> categoryIds) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("status", 1); // 只考虑启用状态的分类
        List<Category> subCategories = categoryMapper.selectList(queryWrapper);

        for (Category category : subCategories) {
            categoryIds.add(category.getId());
            // 递归获取子分类
            getChildCategoryIds(category.getId(), categoryIds);
        }
    }

    @Override
    public List<ProductVO> getProductsByBrand(Long brandId) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("brand_id", brandId);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> searchProducts(String keyword) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like("name", keyword)
                .or()
                .like("description", keyword)
        );
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Integer> activeCategoryIds = activeCategories.stream()
                .map(category -> category.getId().intValue())
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateProductStatus(Long id, Integer status) {

        // 获取商品
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        Product product = productMapper.selectOne(queryWrapper);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 更新状态
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        productMapper.updateById(product);
    }

    /**
     * 保存商品图片
     */
    private void saveProductImages(Long productId, List<String> images) {
        if (images != null && !images.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            int sort = 0;
            for (String image : images) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImageUrl(image);
                productImage.setSort(sort++);
                productImage.setTenantId(tenantId);
                productImageMapper.insert(productImage);
            }
        }
    }

    /**
     * 保存商品颜色
     */
    private void saveProductColors(Long productId, List<String> colors) {
        if (colors != null && !colors.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            for (String color : colors) {
                ProductColor productColor = new ProductColor();
                productColor.setProductId(productId);
                productColor.setColorName(color);
                productColor.setColorValue("#000000");
                productColor.setStock(0);
                productColor.setTenantId(String.valueOf(tenantId));
                productColorMapper.insert(productColor);
            }
        }
    }

    /**
     * 保存商品尺寸
     */
    private void saveProductSizes(Long productId, List<String> sizes) {
        if (sizes != null && !sizes.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            for (String size : sizes) {
                ProductSize productSize = new ProductSize();
                productSize.setProductId(productId);
                productSize.setSizeName(size);
                productSize.setStock(0);
                productSize.setTenantId(String.valueOf(tenantId));
                productSizeMapper.insert(productSize);
            }
        }
    }

    /**
     * 保存商品规格
     */
    private void saveProductSpecifications(Long productId, List<ProductSpecificationDTO> specifications) {
        if (specifications != null && !specifications.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            for (ProductSpecificationDTO spec : specifications) {
                ProductSpecification productSpecification = new ProductSpecification();
                productSpecification.setProductId(productId);
                productSpecification.setSpecName(spec.getName());
                // 使用新的getSpecValue()方法获取规格值
                productSpecification.setSpecValue(spec.getSpecValue());
                productSpecification.setTenantId(String.valueOf(tenantId));
                productSpecificationMapper.insert(productSpecification);
            }
        }
    }

    /**
     * 保存商品属性值
     */
    private void saveProductAttributeValues(Long productId, List<ProductAttributeValueDTO> attributeValues) {
        if (attributeValues != null && !attributeValues.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            for (ProductAttributeValueDTO attrValue : attributeValues) {
                ProductAttributeValue productAttributeValue = new ProductAttributeValue();
                productAttributeValue.setProductId(productId);
                productAttributeValue.setAttributeId(attrValue.getAttributeId() != null ? attrValue.getAttributeId().longValue() : null);
                productAttributeValue.setValue(attrValue.getValue());
                productAttributeValue.setTenantId(String.valueOf(tenantId));
                productAttributeValueMapper.insert(productAttributeValue);
            }
        }
    }

    /**
     * 保存商品参数值
     */
    private void saveProductParamValues(Long productId, List<ProductParamDTO> paramValues) {
        if (paramValues != null && !paramValues.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            for (ProductParamDTO paramValue : paramValues) {
                ProductParam productParam = new ProductParam();
                productParam.setProductId(productId);
                productParam.setParamName(paramValue.getParamName());
                productParam.setParamValue(paramValue.getValue());
                productParam.setSortOrder(0);
                productParam.setTenantId(String.valueOf(tenantId));
                productParamMapper.insert(productParam);
            }
        }
    }

    /**
     * 删除商品属性值
     */
    private void deleteProductAttributeValues(Long productId) {
        QueryWrapper<ProductAttributeValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productAttributeValueMapper.delete(queryWrapper);
    }

    /**
     * 删除商品参数值
     */
    private void deleteProductParamValues(Long productId) {
        QueryWrapper<ProductParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productParamMapper.delete(queryWrapper);
    }

    /**
     * 删除商品图片
     */
    private void deleteProductImages(Long productId) {
        QueryWrapper<ProductImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productImageMapper.delete(queryWrapper);
    }

    /**
     * 删除商品颜色
     */
    private void deleteProductColors(Long productId) {
        QueryWrapper<ProductColor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productColorMapper.delete(queryWrapper);
    }

    /**
     * 删除商品尺寸
     */
    private void deleteProductSizes(Long productId) {
        QueryWrapper<ProductSize> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productSizeMapper.delete(queryWrapper);
    }

    /**
     * 删除商品规格
     */
    private void deleteProductSpecifications(Long productId) {
        QueryWrapper<ProductSpecification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productSpecificationMapper.delete(queryWrapper);
    }

    /**
     * 删除商品SKU
     */
    private void deleteProductSkus(Long productId) {
        QueryWrapper<ProductSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productSkuMapper.delete(queryWrapper);
    }

    /**
     * 保存商品SKU
     */
    private void saveProductSkus(Long productId, List<ProductSkuDTO> skus) {
        if (skus != null && !skus.isEmpty()) {
            for (ProductSkuDTO skuDTO : skus) {
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);

                // 处理规格值：优先使用前端传递的specs对象
                String[] specValues = skuDTO.getSpecValues();
                if (specValues.length > 0) {
                    sku.setSpec1(specValues[0]);
                    sku.setSpec2(specValues.length > 1 ? specValues[1] : null);
                    sku.setSpec3(specValues.length > 2 ? specValues[2] : null);
                    sku.setSpec4(specValues.length > 3 ? specValues[3] : null);
                } else {
                    // 兼容旧格式
                    sku.setSpec1(skuDTO.getSpec1());
                    sku.setSpec2(skuDTO.getSpec2());
                    sku.setSpec3(skuDTO.getSpec3());
                    sku.setSpec4(skuDTO.getSpec4());
                }

                sku.setPrice(skuDTO.getPrice());
                sku.setOriginalPrice(skuDTO.getOriginalPrice());
                sku.setPromotionPrice(skuDTO.getPromotionPrice());
                sku.setStock(skuDTO.getStock());
                sku.setSkuCode(skuDTO.getSkuCode());
                sku.setSkuName(skuDTO.getSkuName());
                sku.setImage(skuDTO.getImage());
                sku.setStatus(skuDTO.getStatus() != null ? skuDTO.getStatus() : 1);
                sku.setCreatedAt(LocalDateTime.now());
                sku.setUpdatedAt(LocalDateTime.now());
                sku.setTenantId(String.valueOf(SecureUtil.getTenantId()));
                productSkuMapper.insert(sku);
            }
        }
    }

    /**
     * 保存商品相册图片
     */
    private void saveProductAlbumImages(Long productId, List<ProductAlbumImageDTO> albumImages) {
        if (albumImages != null && !albumImages.isEmpty()) {
            // 获取当前租户ID
            String tenantId = SecureUtil.getTenantId();
            int sort = 0;
            for (ProductAlbumImageDTO albumImage : albumImages) {
                ProductAlbumImage productAlbumImage = new ProductAlbumImage();
                productAlbumImage.setProductId(productId);
                productAlbumImage.setSkuId(albumImage.getSkuId());
                productAlbumImage.setColor(albumImage.getColor());
                productAlbumImage.setImageUrl(albumImage.getUrl());
                productAlbumImage.setSort(albumImage.getSort() != null ? albumImage.getSort() : sort++);
                productAlbumImage.setIsMain(albumImage.getIsMain() != null && albumImage.getIsMain() ? 1 : 0);
                productAlbumImage.setDescription(albumImage.getDescription());
                productAlbumImage.setWidth(albumImage.getWidth());
                productAlbumImage.setHeight(albumImage.getHeight());
                productAlbumImage.setSize(albumImage.getSize());
                productAlbumImage.setCreatedAt(LocalDateTime.now());
                productAlbumImage.setUpdatedAt(LocalDateTime.now());
                productAlbumImage.setTenantId(String.valueOf(tenantId));
                productAlbumImageMapper.insert(productAlbumImage);
            }
        }
    }



    /**
     * 删除商品规格属性
     */
    private void deleteProductSpecAttributes(Long productId) {
        // 删除规格属性值
        QueryWrapper<ProductSpecValue> specValueQuery = new QueryWrapper<>();
        QueryWrapper<ProductSpecAttribute> specAttrQuery = new QueryWrapper<>();
        specAttrQuery.eq("product_id", productId);
        List<ProductSpecAttribute> specAttributes = productSpecAttributeMapper.selectList(specAttrQuery);
        for (ProductSpecAttribute attr : specAttributes) {
            specValueQuery.clear();
            specValueQuery.eq("attribute_id", attr.getId());
            productSpecValueMapper.delete(specValueQuery);
        }
        // 删除规格属性
        productSpecAttributeMapper.delete(specAttrQuery);
    }

    /**
     * 删除商品相册图片
     */
    private void deleteProductAlbumImages(Long productId) {
        QueryWrapper<ProductAlbumImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productAlbumImageMapper.delete(queryWrapper);
    }

    /**
     * 保存商品关联
     */
    private void saveProductRelations(Long productId, List<Long> relatedProducts, List<Long> bundleProducts, List<Long> recommendProducts) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();
        // 保存相关商品
        if (relatedProducts != null && !relatedProducts.isEmpty()) {
            for (Long relatedProductId : relatedProducts) {
                ProductRelation relation = new ProductRelation();
                relation.setProductId(productId);
                relation.setRelatedProductId(relatedProductId);
                relation.setType(1); // 1-相关商品
                relation.setTenantId(String.valueOf(tenantId));
                productRelationMapper.insert(relation);
            }
        }

        // 保存搭配商品
        if (bundleProducts != null && !bundleProducts.isEmpty()) {
            for (Long bundleProductId : bundleProducts) {
                ProductRelation relation = new ProductRelation();
                relation.setProductId(productId);
                relation.setRelatedProductId(bundleProductId);
                relation.setType(2); // 2-搭配商品
                relation.setTenantId(String.valueOf(tenantId));
                productRelationMapper.insert(relation);
            }
        }

        // 保存推荐商品
        if (recommendProducts != null && !recommendProducts.isEmpty()) {
            for (Long recommendProductId : recommendProducts) {
                ProductRelation relation = new ProductRelation();
                relation.setProductId(productId);
                relation.setRelatedProductId(recommendProductId);
                relation.setType(3); // 3-推荐商品
                relation.setTenantId(String.valueOf(tenantId));
                productRelationMapper.insert(relation);
            }
        }
    }

    /**
     * 删除商品关联
     */
    private void deleteProductRelations(Long productId) {
        QueryWrapper<ProductRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        productRelationMapper.delete(queryWrapper);
    }

    /**
     * 将商品实体转换为VO
     */
    private ProductVO convertToVO(Product product) {
        ProductVO productVO = new ProductVO();


        // ==================== 基本字段映射 ====================
        productVO.setId(product.getId());
        productVO.setName(product.getName());
        productVO.setSubtitle(product.getSubtitle());
        productVO.setDescription(product.getDescription());
        productVO.setDetailDescription(product.getDetailDescription());
        productVO.setTags(product.getTags());
        productVO.setCategoryId(product.getCategoryId());
        productVO.setBrandId(product.getBrandId());
        productVO.setPrice(product.getPrice());
        productVO.setOriginalPrice(product.getOriginalPrice());
        productVO.setCostPrice(product.getCostPrice());
        productVO.setStock(product.getStock());
        productVO.setStockWarning(product.getStockWarning());
        productVO.setUnit(product.getUnit());
        productVO.setWeight(product.getWeight());
        productVO.setMinPurchase(product.getMinPurchase());
        productVO.setMaxPurchase(product.getMaxPurchase());
        productVO.setSales(product.getSales());
        productVO.setViewCount(product.getViewCount());
        productVO.setFavoriteCount(product.getFavoriteCount());
        productVO.setUserRating(product.getUserRating());
        productVO.setUserReviewCount(product.getUserReviewCount());
        productVO.setSortOrder(product.getSortOrder());
        productVO.setSort(product.getSort());
        productVO.setProductCode(product.getProductCode());
        productVO.setProductSn(product.getProductSn());

        // 赋值已售数量（兼容前端字段名）
        productVO.setSold(product.getSales());

        // ==================== 图片字段映射 ====================
        // 处理图片URL，将相对路径转换为完整URL
        String mainImageUrl = convertImageUrl(product.getMainImage());
        // 兼容旧字段名 image
        productVO.setImage(mainImageUrl);
        // 新字段名 mainImage
        productVO.setMainImage(mainImageUrl);

        // ==================== 状态字段映射 ====================
        productVO.setStatus(product.getStatus() != null && product.getStatus() == 1 ? "active" : "inactive");
        productVO.setIsNew(product.getIsNew() != null && product.getIsNew() == 1);
        productVO.setIsHot(product.getIsHot() != null && product.getIsHot() == 1);
        productVO.setIsRecommend(product.getIsRecommend() != null && product.getIsRecommend() == 1);
        productVO.setIsPreview(product.getIsPreview() != null && product.getIsPreview() == 1);
        productVO.setIsOnSale(product.getStatus() != null && product.getStatus() == 1);
        // 兼容旧字段名 isFeatured
        productVO.setIsFeatured(product.getIsRecommend() != null && product.getIsRecommend() == 1);

        // ==================== 促销和积分字段映射 ====================
        productVO.setPromotionType(product.getPromotionType());
        productVO.setPromotionId(product.getPromotionId());
        productVO.setGiftPoint(product.getGiftPoint());
        productVO.setGiftGrowth(product.getGiftGrowth());
        productVO.setUsePointLimit(product.getUsePointLimit());

        // ==================== 服务保证和SEO字段映射 ====================
        productVO.setServiceIds(product.getServiceIds());
        productVO.setDetailTitle(product.getDetailTitle());
        productVO.setDetailDesc(product.getDetailDesc());
        productVO.setKeywords(product.getKeywords());
        productVO.setNote(product.getNote());

        // ==================== 折扣相关计算 ====================
        if (product.hasDiscount()) {
            productVO.setHasDiscount(true);
            productVO.setDiscountRate(product.getDiscountRate());
        } else {
            productVO.setHasDiscount(false);
            productVO.setDiscountRate(BigDecimal.ZERO);
        }

        // ==================== 时间字段映射 ====================
        productVO.setCreatedAt(product.getCreatedAt());
        productVO.setUpdatedAt(product.getUpdatedAt());
        productVO.setOnShelfTime(product.getOnShelfTime());
        productVO.setOffShelfTime(product.getOffShelfTime());

        // ==================== 分类名称查询 ====================
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("id", product.getCategoryId());
        Category category = categoryMapper.selectOne(categoryQuery);
        if (category != null) {
            productVO.setCategoryName(category.getName());
        }

        // ==================== 品牌信息查询 ====================
        QueryWrapper<Brand> brandQuery = new QueryWrapper<>();
        brandQuery.eq("id", product.getBrandId());
        Brand brand = brandMapper.selectOne(brandQuery);
        if (brand != null) {
            productVO.setBrandName(brand.getName());
            productVO.setBrandLogo(brand.getLogo());
        }

        // ==================== 商品图片列表查询 ====================
        QueryWrapper<ProductImage> imageQuery = new QueryWrapper<>();
        imageQuery.eq("product_id", product.getId());
        imageQuery.orderByAsc("sort");
        List<ProductImage> images = productImageMapper.selectList(imageQuery);
        List<String> imageList = images.stream()
                .map(ProductImage::getImageUrl)
                .map(this::convertImageUrl)
                .collect(Collectors.toList());
        productVO.setImages(imageList);
        productVO.setAlbum(imageList); // 兼容前端字段名
        productVO.setImageCount(imageList.size());

        // ==================== 商品颜色列表查询 ====================
        QueryWrapper<ProductColor> colorQuery = new QueryWrapper<>();
        colorQuery.eq("product_id", product.getId());
        List<ProductColor> colors = productColorMapper.selectList(colorQuery);
        List<String> colorList = colors.stream()
                .map(ProductColor::getColorName)
                .collect(Collectors.toList());
        productVO.setColors(colorList);
        productVO.setColorCount(colorList.size());

        // ==================== 商品尺寸列表查询 ====================
        QueryWrapper<ProductSize> sizeQuery = new QueryWrapper<>();
        sizeQuery.eq("product_id", product.getId());
        List<ProductSize> sizes = productSizeMapper.selectList(sizeQuery);
        List<String> sizeList = sizes.stream()
                .map(ProductSize::getSizeName)
                .collect(Collectors.toList());
        productVO.setSizes(sizeList);
        productVO.setSizeCount(sizeList.size());

        // ==================== 商品规格列表查询 ====================
        QueryWrapper<ProductSpecification> specQuery = new QueryWrapper<>();
        specQuery.eq("product_id", product.getId());
        List<ProductSpecification> specifications = productSpecificationMapper.selectList(specQuery);
        List<ProductSpecificationVO> specVOs = specifications.stream()
                .map(spec -> {
                    ProductSpecificationVO specVO = new ProductSpecificationVO();
                    specVO.setId(spec.getId());
                    specVO.setProductId(spec.getProductId());
                    specVO.setName(spec.getSpecName()); // 手动映射字段名
                    specVO.setValue(spec.getSpecValue()); // 手动映射字段名
                    return specVO;
                })
                .collect(Collectors.toList());
        productVO.setSpecifications(specVOs);

        // ==================== 商品 SKU 列表查询 ====================
        QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
        skuQuery.eq("product_id", product.getId());
        List<ProductSku> skus = productSkuMapper.selectList(skuQuery);
        List<ProductSkuVO> skuVOs = skus.stream()
                .map(sku -> {
                    ProductSkuVO skuVO = new ProductSkuVO();
                    BeanUtils.copyProperties(sku, skuVO);
                    return skuVO;
                })
                .collect(Collectors.toList());
        productVO.setSkus(skuVOs);

        // ==================== 计算 SKU 总库存 ====================
        Integer totalStock = skus.stream()
                .mapToInt(sku -> sku.getStock() != null ? sku.getStock() : 0)
                .sum();
        productVO.setTotalSkuStock(totalStock);

        // ==================== 商品属性值列表查询 ====================
        QueryWrapper<ProductAttributeValue> attributeQuery = new QueryWrapper<>();
        attributeQuery.eq("product_id", product.getId());
        List<ProductAttributeValue> attributeValues = productAttributeValueMapper.selectList(attributeQuery);
        List<Map<String, Object>> attributeList = attributeValues.stream()
                .map(attr -> {
                    Map<String, Object> attrMap = new HashMap<>();
                    attrMap.put("attributeId", attr.getAttributeId());
                    attrMap.put("value", attr.getValue());
                    return attrMap;
                })
                .collect(Collectors.toList());
        productVO.setAttributeValues(attributeList);

        // ==================== 商品参数列表查询 ====================
        QueryWrapper<ProductParam> paramQuery = new QueryWrapper<>();
        paramQuery.eq("product_id", product.getId());
        List<ProductParam> params = productParamMapper.selectList(paramQuery);
        List<Map<String, String>> paramList = params.stream()
                .map(param -> {
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("name", param.getParamName());
                    paramMap.put("value", param.getParamValue());
                    return paramMap;
                })
                .collect(Collectors.toList());
        productVO.setParams(paramList);

        // ==================== 分类属性列表查询 ====================
        QueryWrapper<ProductSpecAttribute> specAttrQuery = new QueryWrapper<>();
        specAttrQuery.eq("product_id", product.getId());
        List<ProductSpecAttribute> specAttributes = productSpecAttributeMapper.selectList(specAttrQuery);
        List<ProductSpecAttributeVO> specAttrVOs = specAttributes.stream()
                .map(attr -> {
                    ProductSpecAttributeVO attrVO = new ProductSpecAttributeVO();
                    attrVO.setId(attr.getId());
                    attrVO.setProductId(attr.getProductId());
                    attrVO.setName(attr.getName());
                    attrVO.setSortOrder(attr.getSortOrder());

                    // 查询该属性的所有值
                    QueryWrapper<ProductSpecValue> specValueQuery = new QueryWrapper<>();
                    specValueQuery.eq("attribute_id", attr.getId());
                    List<ProductSpecValue> specValues = productSpecValueMapper.selectList(specValueQuery);

                    if (!specValues.isEmpty()) {
                        // 转换为 ProductSpecValueVO 列表
                        List<ProductSpecValueVO> valueVOs = specValues.stream()
                                .map(value -> {
                                    ProductSpecValueVO valueVO = new ProductSpecValueVO();
                                    valueVO.setId(value.getId());
                                    valueVO.setAttributeId(value.getAttributeId());
                                    valueVO.setValue(value.getValue());
                                    valueVO.setImage(value.getImage());
                                    valueVO.setSortOrder(value.getSortOrder());
                                    return valueVO;
                                })
                                .collect(Collectors.toList());
                        attrVO.setValues(valueVOs);
                    }

                    return attrVO;
                })
                .collect(Collectors.toList());
        productVO.setSpecAttributes(specAttrVOs);

        // ==================== 关联商品查询 ====================
        QueryWrapper<ProductRelation> relationQuery = new QueryWrapper<>();
        relationQuery.eq("product_id", product.getId());
        List<ProductRelation> relations = productRelationMapper.selectList(relationQuery);

        // 分类关联商品
        List<Long> relatedProductIds = relations.stream()
                .filter(r -> r.getType() == 1)
                .map(ProductRelation::getRelatedProductId)
                .collect(Collectors.toList());
        productVO.setRelatedProducts(relatedProductIds);

        // 搭配商品
        List<Long> bundleProductIds = relations.stream()
                .filter(r -> r.getType() == 2)
                .map(ProductRelation::getRelatedProductId)
                .collect(Collectors.toList());
        productVO.setBundleProducts(bundleProductIds);

        // 推荐商品
        List<Long> recommendProductIds = relations.stream()
                .filter(r -> r.getType() == 3)
                .map(ProductRelation::getRelatedProductId)
                .collect(Collectors.toList());
        productVO.setRecommendProducts(recommendProductIds);

        // ==================== 商品相册图片查询 ====================
        QueryWrapper<ProductAlbumImage> albumImageQuery = new QueryWrapper<>();
        albumImageQuery.eq("product_id", product.getId());
        albumImageQuery.orderByAsc("sort");
        List<ProductAlbumImage> albumImages = productAlbumImageMapper.selectList(albumImageQuery);
        List<ProductAlbumImageVO> albumImageVOs = albumImages.stream()
                .map(albumImage -> {
                    ProductAlbumImageVO albumImageVO = new ProductAlbumImageVO();
                    albumImageVO.setId(albumImage.getId());
                    albumImageVO.setUrl(convertImageUrl(albumImage.getImageUrl()));
                    albumImageVO.setSort(albumImage.getSort());
                    albumImageVO.setIsMain(albumImage.getIsMain() != null && albumImage.getIsMain() == 1);
                    albumImageVO.setDescription(albumImage.getDescription());
                    albumImageVO.setWidth(albumImage.getWidth());
                    albumImageVO.setHeight(albumImage.getHeight());
                    albumImageVO.setSize(albumImage.getSize());
                    albumImageVO.setType(albumImage.getType());
                    albumImageVO.setCreatedAt(albumImage.getCreatedAt() != null ? albumImage.getCreatedAt().toString() : null);
                    albumImageVO.setSkuId(albumImage.getSkuId());
                    albumImageVO.setColor(albumImage.getColor());
                    return albumImageVO;
                })
                .collect(Collectors.toList());
        productVO.setAlbumImages(albumImageVOs);

        return productVO;
    }

    /**
     * 转换图片URL
     * 保持相对路径，由前端处理完整URL构建
     */
    private String convertImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return imageUrl;
        }
        // 如果已经是完整URL，直接返回
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        // 保持相对路径，前端会处理完整URL构建
        return imageUrl;
    }

    @Override
    @Transactional
    public void batchDeleteProducts(List<Long> ids) {
        for (Long id : ids) {
            deleteProduct(id);
        }
    }

    @Override
    public Map<String, Object> getProductsWithFilters(ProductQueryDTO queryDTO) {

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0);

        // 只在status不为null时添加状态过滤
        // 对于admin端，status为null时查询所有状态的商品
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq("status", queryDTO.getStatus());
        }

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        // 先查询所有启用状态的分类ID
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Long> activeCategoryIds = activeCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        // 确保只返回所属分类处于启用状态的产品
        if (!activeCategoryIds.isEmpty()) {
            // 使用IN条件筛选产品的分类ID
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            // 如果没有启用的分类，返回空结果
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("list", new ArrayList<>());
            emptyResult.put("total", 0);
            emptyResult.put("current", queryDTO.getCurrent());
            emptyResult.put("pageSize", queryDTO.getPageSize());
            emptyResult.put("pages", 0);
            return emptyResult;
        }

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("name", queryDTO.getKeyword())
                .or()
                .like("description", queryDTO.getKeyword())
            );
        }

        if (queryDTO.getCategoryId() != null) {
            // 检查指定的分类是否处于启用状态
            Category category = categoryMapper.selectById(queryDTO.getCategoryId());
            if (category != null && category.getStatus() == 1) {
                // 获取分类及其所有子分类的ID
                List<Long> categoryIds = getCategoryAndChildrenIds(queryDTO.getCategoryId());
                queryWrapper.in("category_id", categoryIds);
            } else {
                // 如果指定的分类不存在或处于禁用状态，返回空结果
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("list", new ArrayList<>());
                emptyResult.put("total", 0);
                emptyResult.put("current", queryDTO.getCurrent());
                emptyResult.put("pageSize", queryDTO.getPageSize());
                emptyResult.put("pages", 0);
                return emptyResult;
            }
        }

        if (queryDTO.getBrandId() != null) {
            queryWrapper.eq("brand_id", queryDTO.getBrandId());
        }

        if (queryDTO.getIsNew() != null) {
            queryWrapper.eq("is_new", queryDTO.getIsNew());
        }

        if (queryDTO.getIsHot() != null) {
            queryWrapper.eq("is_hot", queryDTO.getIsHot());
        }

        if (queryDTO.getIsRecommend() != null) {
            queryWrapper.eq("is_recommend", queryDTO.getIsRecommend());
        }

        if (queryDTO.getMinPrice() != null) {
            queryWrapper.ge("price", queryDTO.getMinPrice());
        }

        if (queryDTO.getMaxPrice() != null) {
            queryWrapper.le("price", queryDTO.getMaxPrice());
        }

        String orderField = convertSortField(queryDTO.getSortBy());
        if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
            queryWrapper.orderByAsc(orderField);
        } else {
            queryWrapper.orderByDesc(orderField);
        }

        Page<Product> page = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        IPage<Product> productPage = productMapper.selectPage(page, queryWrapper);

        List<ProductVO> productVOs = productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", productVOs);
        result.put("total", productPage.getTotal());
        result.put("current", productPage.getCurrent());
        result.put("pageSize", productPage.getSize());
        result.put("pages", productPage.getPages());

        return result;
    }

    private String convertSortField(String sortBy) {
        return switch (sortBy) {
            case "price" -> "price";
            case "sales" -> "sales";
            case "stock" -> "stock";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "sortOrder" -> "sort_order";
            default -> "created_at";
        };
    }

    @Override
    public List<ProductVO> getHotProducts() {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_hot", 1);
        queryWrapper.eq("is_deleted", 0);

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Integer> activeCategoryIds = activeCategories.stream()
                .map(category -> category.getId().intValue())
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        queryWrapper.orderByDesc("sales");
        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void batchUpdateProductStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            updateProductStatus(id, status);
        }
    }

    @Override
    @Transactional
    public void publishProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus(1);
        product.setOnShelfTime(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void unpublishProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus(0);
        product.setOffShelfTime(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void setRecommend(Long id, Integer isRecommend) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setIsRecommend(isRecommend);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void setNew(Long id, Integer isNew) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setIsNew(isNew);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void setHot(Long id, Integer isHot) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setIsHot(isHot);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void adjustStock(Long id, Integer quantity, String remark) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        Integer newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new RuntimeException("库存不足");
        }
        product.setStock(newStock);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    public List<ProductVO> getRelatedProducts(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        queryWrapper.ne("id", id);

        if (product.getCategoryId() != null) {
            // 确保分类处于启用状态
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null && category.getStatus() == 1) {
                queryWrapper.eq("category_id", product.getCategoryId());
            } else {
                return new ArrayList<>();
            }
        }

        // 添加分类状态筛选：只返回所属分类处于启用状态的产品
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.eq("status", 1);
        List<Category> activeCategories = categoryMapper.selectList(categoryQuery);
        List<Integer> activeCategoryIds = activeCategories.stream()
                .map(category -> category.getId().intValue())
                .collect(Collectors.toList());

        if (!activeCategoryIds.isEmpty()) {
            queryWrapper.in("category_id", activeCategoryIds);
        } else {
            return new ArrayList<>();
        }

        queryWrapper.orderByDesc("is_recommend");
        queryWrapper.orderByDesc("sales");
        queryWrapper.last("LIMIT 10");

        List<Product> products = productMapper.selectList(queryWrapper);
        return products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateProductStock(Long productId) {
        // 查询该商品的所有SKU
        QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
        skuQuery.eq("product_id", productId);
        List<ProductSku> skus = productSkuMapper.selectList(skuQuery);

        // 计算总库存
        Integer totalStock = skus.stream()
                .mapToInt(sku -> sku.getStock() != null ? sku.getStock() : 0)
                .sum();

        // 更新商品的库存字段
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setStock(totalStock);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }
    }

    @Override
    public Map<String, Object> getProductStats() {
        QueryWrapper<Product> queryWrapper;

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0);
        long totalProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("is_deleted", 0);
        long activeProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0);
        queryWrapper.eq("is_deleted", 0);
        long inactiveProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_recommend", 1);
        queryWrapper.eq("is_deleted", 0);
        long recommendProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_new", 1);
        queryWrapper.eq("is_deleted", 0);
        long newProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_hot", 1);
        queryWrapper.eq("is_deleted", 0);
        long hotProducts = productMapper.selectCount(queryWrapper);

        queryWrapper = new QueryWrapper<>();
        queryWrapper.le("stock", 10);
        queryWrapper.eq("is_deleted", 0);
        long lowStockProducts = productMapper.selectCount(queryWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);
        stats.put("inactiveProducts", inactiveProducts);
        stats.put("recommendProducts", recommendProducts);
        stats.put("newProducts", newProducts);
        stats.put("hotProducts", hotProducts);
        stats.put("lowStockProducts", lowStockProducts);

        return stats;
    }

    @Override
    public List<ProductSkuVO> getProductSkus(Long productId) {
        QueryWrapper<ProductSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.orderByAsc("id");
        List<ProductSku> skus = productSkuMapper.selectList(queryWrapper);
        return skus.stream()
                .map(this::convertToSkuVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductSkuVO createProductSku(Long productId, ProductSkuDTO skuDTO) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        ProductSku sku = new ProductSku();
        BeanUtils.copyProperties(skuDTO, sku);
        sku.setProductId(productId);
        sku.setStatus(skuDTO.getStatus() != null ? skuDTO.getStatus() : 1);
        sku.setCreatedAt(LocalDateTime.now());
        sku.setUpdatedAt(LocalDateTime.now());

        productSkuMapper.insert(sku);
        return convertToSkuVO(sku);
    }

    @Override
    @Transactional
    public ProductSkuVO updateProductSku(Long id, ProductSkuDTO skuDTO) {
        ProductSku sku = productSkuMapper.selectById(id);
        if (sku == null) {
            throw new RuntimeException("SKU不存在");
        }

        BeanUtils.copyProperties(skuDTO, sku, "id", "productId", "createdAt"); // @SuppressWarnings("null")
        sku.setUpdatedAt(LocalDateTime.now());

        productSkuMapper.updateById(sku);
        return convertToSkuVO(sku);
    }

    @Override
    @Transactional
    public void deleteProductSku(Long id) {
        ProductSku sku = productSkuMapper.selectById(id);
        if (sku == null) {
            throw new RuntimeException("SKU不存在");
        }
        productSkuMapper.deleteById(id);
    }

    @Override
    @Transactional
    public List<ProductSkuVO> batchCreateProductSkus(Long productId, List<ProductSkuDTO> skuDTOs) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        QueryWrapper<ProductSku> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("product_id", productId);
        productSkuMapper.delete(deleteWrapper);

        List<ProductSkuVO> result = new ArrayList<>();
        for (ProductSkuDTO skuDTO : skuDTOs) {
            ProductSku sku = new ProductSku();
            BeanUtils.copyProperties(skuDTO, sku);
            sku.setProductId(productId);
            sku.setStatus(skuDTO.getStatus() != null ? skuDTO.getStatus() : 1);
            sku.setCreatedAt(LocalDateTime.now());
            sku.setUpdatedAt(LocalDateTime.now());
            productSkuMapper.insert(sku);
            result.add(convertToSkuVO(sku));
        }

        return result;
    }

    private ProductSkuVO convertToSkuVO(ProductSku sku) {
        ProductSkuVO vo = new ProductSkuVO();
        BeanUtils.copyProperties(sku, vo);
        return vo;
    }

    // ==================== SKU矩阵生成和规格属性管理 ====================

    @Autowired
    private SkuStockLogMapper skuStockLogMapper;

    @Autowired
    private ProductAlbumImageMapper productAlbumImageMapper;

    @Override
    @Transactional
    public List<ProductSkuVO> generateSkuMatrix(SkuMatrixGenerateDTO generateDTO) {
        Long productId = generateDTO.getProductId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 保存规格属性
        List<ProductSpecAttribute> savedAttributes = new ArrayList<>();
        Map<Long, List<ProductSpecValue>> valuesMap = new HashMap<>();

        for (ProductSpecAttributeDTO attrDTO : generateDTO.getAttributes()) {
            ProductSpecAttribute attribute = new ProductSpecAttribute();
            attribute.setProductId(productId);
            attribute.setName(attrDTO.getName());
            attribute.setSortOrder(attrDTO.getSortOrder() != null ? attrDTO.getSortOrder() : 0);
            attribute.setCreatedAt(LocalDateTime.now());
            attribute.setUpdatedAt(LocalDateTime.now());
            productSpecAttributeMapper.insert(attribute);
            savedAttributes.add(attribute);

            // 保存属性值
            List<ProductSpecValue> savedValues = new ArrayList<>();
            if (attrDTO.getValues() != null) {
                for (ProductSpecValueDTO valueDTO : attrDTO.getValues()) {
                    ProductSpecValue value = new ProductSpecValue();
                    value.setAttributeId(attribute.getId());
                    value.setValue(valueDTO.getValue());
                    value.setImage(valueDTO.getImage());
                    value.setSortOrder(valueDTO.getSortOrder() != null ? valueDTO.getSortOrder() : 0);
                    value.setCreatedAt(LocalDateTime.now());
                    value.setUpdatedAt(LocalDateTime.now());
                    productSpecValueMapper.insert(value);
                    savedValues.add(value);
                }
            }
            valuesMap.put(attribute.getId(), savedValues);
        }

        // 如果需要清空现有SKU
        if (generateDTO.getClearExisting() != null && generateDTO.getClearExisting()) {
            QueryWrapper<ProductSku> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("product_id", productId);
            productSkuMapper.delete(deleteWrapper);
        }

        // 生成SKU矩阵
        List<ProductSku> generatedSkus = SkuMatrixGenerator.generateSkuMatrix(
                productId,
                savedAttributes,
                valuesMap,
                generateDTO.getBasePrice(),
                generateDTO.getBaseStock()
        );

        // 保存生成的SKU
        List<ProductSkuVO> result = new ArrayList<>();
        for (ProductSku sku : generatedSkus) {
            productSkuMapper.insert(sku);
            result.add(convertToSkuVO(sku));
        }

        return result;
    }

    @Override
    public List<ProductSpecAttributeVO> getProductSpecAttributes(Long productId) {
        QueryWrapper<ProductSpecAttribute> attrWrapper = new QueryWrapper<>();
        attrWrapper.eq("product_id", productId);
        attrWrapper.orderByAsc("sort_order");
        List<ProductSpecAttribute> attributes = productSpecAttributeMapper.selectList(attrWrapper);

        return attributes.stream().map(attr -> {
            ProductSpecAttributeVO attrVO = new ProductSpecAttributeVO();
            BeanUtils.copyProperties(attr, attrVO);

            // 查询属性值
            QueryWrapper<ProductSpecValue> valueWrapper = new QueryWrapper<>();
            valueWrapper.eq("attribute_id", attr.getId());
            valueWrapper.orderByAsc("sort_order");
            List<ProductSpecValue> values = productSpecValueMapper.selectList(valueWrapper);

            attrVO.setValues(values.stream().map(val -> {
                ProductSpecValueVO valVO = new ProductSpecValueVO();
                BeanUtils.copyProperties(val, valVO);
                return valVO;
            }).collect(Collectors.toList()));

            return attrVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ProductSpecAttributeVO> saveProductSpecAttributes(Long productId, List<ProductSpecAttributeDTO> attributes) {
        // 获取当前租户ID
        String tenantId = SecureUtil.getTenantId();

        // 删除旧的规格属性
        QueryWrapper<ProductSpecAttribute> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("product_id", productId);
        List<ProductSpecAttribute> oldAttributes = productSpecAttributeMapper.selectList(deleteWrapper);
        for (ProductSpecAttribute oldAttr : oldAttributes) {
            QueryWrapper<ProductSpecValue> valueDeleteWrapper = new QueryWrapper<>();
            valueDeleteWrapper.eq("attribute_id", oldAttr.getId());
            productSpecValueMapper.delete(valueDeleteWrapper);
        }
        productSpecAttributeMapper.delete(deleteWrapper);

        // 保存新的规格属性
        List<ProductSpecAttributeVO> result = new ArrayList<>();
        for (ProductSpecAttributeDTO attrDTO : attributes) {
            ProductSpecAttribute attribute = new ProductSpecAttribute();
            attribute.setProductId(productId);
            attribute.setName(attrDTO.getName());
            attribute.setSortOrder(attrDTO.getSortOrder() != null ? attrDTO.getSortOrder() : 0);
            attribute.setCreatedAt(LocalDateTime.now());
            attribute.setUpdatedAt(LocalDateTime.now());
            attribute.setTenantId(String.valueOf(tenantId));
            productSpecAttributeMapper.insert(attribute);

            ProductSpecAttributeVO attrVO = new ProductSpecAttributeVO();
            BeanUtils.copyProperties(attribute, attrVO);

            // 保存属性值
            List<ProductSpecValueVO> valueVOs = new ArrayList<>();
            if (attrDTO.getValues() != null) {
                for (ProductSpecValueDTO valueDTO : attrDTO.getValues()) {
                    ProductSpecValue value = new ProductSpecValue();
                    value.setAttributeId(attribute.getId());
                    value.setValue(valueDTO.getValue());
                    value.setImage(valueDTO.getImage());
                    value.setSortOrder(valueDTO.getSortOrder() != null ? valueDTO.getSortOrder() : 0);
                    value.setCreatedAt(LocalDateTime.now());
                    value.setUpdatedAt(LocalDateTime.now());
                    value.setTenantId(String.valueOf(tenantId));
                    productSpecValueMapper.insert(value);

                    ProductSpecValueVO valueVO = new ProductSpecValueVO();
                    BeanUtils.copyProperties(value, valueVO);
                    valueVOs.add(valueVO);
                }
            }
            attrVO.setValues(valueVOs);
            result.add(attrVO);
        }

        return result;
    }

    @Override
    @Transactional
    public void deleteProductSpecAttribute(Long attributeId) {
        // 删除属性值
        QueryWrapper<ProductSpecValue> valueWrapper = new QueryWrapper<>();
        valueWrapper.eq("attribute_id", attributeId);
        productSpecValueMapper.delete(valueWrapper);

        // 删除属性
        productSpecAttributeMapper.deleteById(attributeId);
    }

    // ==================== SKU库存日志管理 ====================

    @Override
    public List<SkuStockLogVO> getSkuStockLogs(Long skuId) {
        QueryWrapper<SkuStockLog> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId);
        wrapper.orderByDesc("created_at");
        List<SkuStockLog> logs = skuStockLogMapper.selectList(wrapper);

        return logs.stream().map(log -> {
            SkuStockLogVO vo = new SkuStockLogVO();
            BeanUtils.copyProperties(log, vo);
            // 设置变动类型文本
            switch (log.getType()) {
                case 1:
                    vo.setTypeText("入库");
                    break;
                case 2:
                    vo.setTypeText("出库");
                    break;
                case 3:
                    vo.setTypeText("调整");
                    break;
                default:
                    vo.setTypeText("未知");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void adjustSkuStock(Long skuId, Integer quantity, Integer type, String remark) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new RuntimeException("SKU不存在");
        }

        Integer beforeStock = sku.getStock();
        Integer afterStock = beforeStock + quantity;

        if (afterStock < 0) {
            throw new RuntimeException("库存不足，无法调整");
        }

        // 更新SKU库存
        sku.setStock(afterStock);
        sku.setUpdatedAt(LocalDateTime.now());
        productSkuMapper.updateById(sku);

        // 记录库存变动日志
        SkuStockLog log = new SkuStockLog();
        log.setSkuId(skuId);
        log.setProductId(sku.getProductId());
        log.setType(type);
        log.setQuantity(quantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        skuStockLogMapper.insert(log);
    }

    @Override
    public List<Map<String, String>> getProductParams(Long productId) {
        QueryWrapper<ProductParam> paramQuery = new QueryWrapper<>();
        paramQuery.eq("product_id", productId);
        List<ProductParam> params = productParamMapper.selectList(paramQuery);

        return params.stream()
                .map(param -> {
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("name", param.getParamName());
                    paramMap.put("value", param.getParamValue());
                    // 为了兼容前端，添加icon字段
                    paramMap.put("icon", "⚙️");
                    return paramMap;
                })
                .collect(Collectors.toList());
    }
}



