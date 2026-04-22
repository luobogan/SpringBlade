package org.springblade.mall.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springblade.mall.dto.ProductQueryDTO;
import org.springblade.mall.service.ProductService;
import org.springblade.mall.vo.ProductSpecAttributeVO;
import org.springblade.mall.vo.ProductVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 前端商品控制器 - 增强版
 * 处理前端的商品相关请求，路径为 /products
 * 支持高级查询、筛选、排序、分页、相关推荐等功能
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/front/products")
@Tag(name = "前端商品管理", description = "前端商品管理")
public class FrontProductController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(FrontProductController.class);

    private ProductService productService;

    /**
     * 高级查询商品列表（支持筛选、排序、分页）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param brandId 品牌ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param isRecommend 是否推荐
     * @param isNew 是否新品
     * @param isHot 是否热销
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 商品列表
     */
    @GetMapping("/advanced")
    @Operation(summary = "高级查询商品列表", description = "支持筛选、排序、分页的商品查询")
    public R<Map<String, Object>> getProductsAdvanced(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "12") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "品牌ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "是否推荐") @RequestParam(required = false) Boolean isRecommend,
            @Parameter(description = "是否新品") @RequestParam(required = false) Boolean isNew,
            @Parameter(description = "是否热销") @RequestParam(required = false) Boolean isHot,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            ProductQueryDTO queryDTO = new ProductQueryDTO();
            queryDTO.setCurrent(current);
            queryDTO.setPageSize(pageSize);
            queryDTO.setKeyword(keyword);
            queryDTO.setCategoryId(categoryId);
            queryDTO.setBrandId(brandId);
            queryDTO.setMinPrice(minPrice);
            queryDTO.setMaxPrice(maxPrice);
            queryDTO.setIsRecommend(isRecommend != null ? (isRecommend ? 1 : 0) : null);
            queryDTO.setIsNew(isNew != null ? (isNew ? 1 : 0) : null);
            queryDTO.setIsHot(isHot != null ? (isHot ? 1 : 0) : null);
            queryDTO.setSortBy(sortBy);
            queryDTO.setSortOrder(sortOrder);
            // status: 前端只显示上架商品，但这里不设置，由调用方决定是否过滤

            Map<String, Object> result = productService.getProductsWithFilters(queryDTO);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取所有商品（分页）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param categoryId 分类ID
     * @param isRecommend 是否推荐
     * @param isNew 是否新品
     * @param isHot 是否热销
     * @return 商品列表
     */
    @GetMapping
    @Operation(summary = "获取所有商品", description = "分页获取商品列表，可按分类筛选")
    public R<Map<String, Object>> getAllProducts(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "12") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否推荐") @RequestParam(required = false) Boolean isRecommend,
            @Parameter(description = "是否新品") @RequestParam(required = false) Boolean isNew,
            @Parameter(description = "是否热销") @RequestParam(required = false) Boolean isHot) {
        try {
            ProductQueryDTO queryDTO = new ProductQueryDTO();
            queryDTO.setCurrent(current);
            queryDTO.setPageSize(pageSize);
            queryDTO.setCategoryId(categoryId);
            queryDTO.setKeyword(keyword);
            queryDTO.setIsRecommend(isRecommend != null ? (isRecommend ? 1 : 0) : null);
            queryDTO.setIsNew(isNew != null ? (isNew ? 1 : 0) : null);
            queryDTO.setIsHot(isHot != null ? (isHot ? 1 : 0) : null);
            queryDTO.setStatus(1); // status: 前端只显示上架商品

            Map<String, Object> result = productService.getProductsWithFilters(queryDTO);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详情")
    public R<ProductVO> getProductById(@Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            HttpServletRequest request = WebUtil.getRequest();
            String tenantIdFromHeader = request != null ? request.getHeader("Tenant-Id") : null;
            log.debug("=== FrontProductController.getProductById 调试 ===");
            log.debug("商品ID: {}", id);
            log.debug("请求头中的Tenant-Id: {}", tenantIdFromHeader);
            ProductVO productVO = productService.getProductById(id);
            return R.data(productVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取相关推荐商品
     * @param id 商品ID
     * @return 推荐商品列表
     */
    @GetMapping("/{id}/related")
    @Operation(summary = "获取相关推荐商品", description = "根据商品ID获取相关推荐商品")
    public R<List<ProductVO>> getRelatedProducts(@Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            List<ProductVO> products = productService.getRelatedProducts(id);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取活跃商品
     * @return 活跃商品列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃商品", description = "获取活跃商品列表")
    public R<List<ProductVO>> getActiveProducts() {
        try {
            List<ProductVO> products = productService.getActiveProducts();
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取特色商品
     * @return 特色商品列表
     */
    @GetMapping("/featured")
    @Operation(summary = "获取特色商品", description = "获取特色商品列表")
    public R<List<ProductVO>> getFeaturedProducts() {
        try {
            List<ProductVO> products = productService.getFeaturedProducts();
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取新品
     * @return 新品列表
     */
    @GetMapping("/new")
    @Operation(summary = "获取新品", description = "获取新品列表")
    public R<List<ProductVO>> getNewProducts() {
        try {
            List<ProductVO> products = productService.getNewProducts();
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取热销商品
     * @return 热销商品列表
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热销商品", description = "获取热销商品列表")
    public R<List<ProductVO>> getHotProducts() {
        try {
            List<ProductVO> products = productService.getHotProducts();
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据分类获取商品
     * @param categoryId 分类ID
     * @return 商品列表
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "根据分类获取商品", description = "根据分类ID获取商品列表")
    public R<List<ProductVO>> getProductsByCategory(@Parameter(description = "分类ID") @PathVariable Long categoryId) {
        try {
            List<ProductVO> products = productService.getProductsByCategory(categoryId);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据品牌获取商品
     * @param brandId 品牌ID
     * @return 商品列表
     */
    @GetMapping("/brand/{brandId}")
    @Operation(summary = "根据品牌获取商品", description = "根据品牌ID获取商品列表")
    public R<List<ProductVO>> getProductsByBrand(@Parameter(description = "品牌ID") @PathVariable Long brandId) {
        try {
            List<ProductVO> products = productService.getProductsByBrand(brandId);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 搜索商品
     * @param keyword 搜索关键词
     * @return 商品列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    public R<List<ProductVO>> searchProducts(@Parameter(description = "搜索关键词") @RequestParam String keyword) {
        try {
            List<ProductVO> products = productService.searchProducts(keyword);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品规格属性列表
     * @param productId 商品ID
     * @return 规格属性列表
     */
    @GetMapping("/{productId}/spec-attributes")
    @Operation(summary = "获取商品规格属性", description = "根据商品ID获取规格属性列表")
    public R<List<ProductSpecAttributeVO>> getProductSpecAttributes(
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        try {
            List<ProductSpecAttributeVO> attributes = productService.getProductSpecAttributes(productId);
            return R.data(attributes);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品参数列表
     * @param id 商品ID
     * @return 商品参数列表
     */
    @GetMapping("/{id}/params")
    @Operation(summary = "获取商品参数", description = "根据商品ID获取参数列表")
    public R<List<Map<String, String>>> getProductParams(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            List<Map<String, String>> params = productService.getProductParams(id);
            return R.data(params);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



