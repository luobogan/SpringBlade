package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.mall.dto.ProductDTO;
import org.springblade.mall.dto.ProductQueryDTO;
import org.springblade.mall.dto.ProductSkuDTO;
import org.springblade.mall.service.ProductService;
import org.springblade.mall.vo.ProductSkuVO;
import org.springblade.mall.vo.ProductVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品控制器 - 增强版
 * 处理后台的商品相关请求，路径为 /blade-mall/product
 * 支持商品的增删改查、上下架、批量操作等功能
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/products")
@Tag(name = "商品管理", description = "商品管理")
public class ProductController extends BladeController {

    private ProductService productService;

    /**
     * 高级查询商品列表（支持筛选、排序、分页）
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param brandId 品牌ID
     * @param status 商品状态
     * @param isNew 是否新品
     * @param isHot 是否热销
     * @param isRecommend 是否推荐
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 商品列表
     */
    @GetMapping
    @Operation(summary = "高级查询商品列表", description = "传入筛选条件")
    public R<Map<String, Object>> getProducts(
            Query query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isNew,
            @RequestParam(required = false) Integer isHot,
            @RequestParam(required = false) Integer isRecommend,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            ProductQueryDTO queryDTO = new ProductQueryDTO();
            queryDTO.setCurrent(query.getCurrent());
            queryDTO.setPageSize(query.getSize());
            queryDTO.setKeyword(keyword);
            queryDTO.setCategoryId(categoryId);
            queryDTO.setBrandId(brandId);
            queryDTO.setStatus(status);
            queryDTO.setIsNew(isNew);
            queryDTO.setIsHot(isHot);
            queryDTO.setIsRecommend(isRecommend);
            queryDTO.setMinPrice(minPrice);
            queryDTO.setMaxPrice(maxPrice);
            queryDTO.setSortBy(sortBy);
            queryDTO.setSortOrder(sortOrder);

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
    @Operation(summary = "获取商品详情", description = "传入id")
    public R<ProductVO> getProductById(@PathVariable Long id) {
        try {
            ProductVO productVO = productService.getProductById(id);
            return R.data(productVO);
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
     * 搜索商品
     * @param keyword 搜索关键词
     * @return 商品列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    public R<List<ProductVO>> searchProducts(@RequestParam String keyword) {
        try {
            List<ProductVO> products = productService.searchProducts(keyword);
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
    public R<List<ProductVO>> getProductsByCategory(@PathVariable Long categoryId) {
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
    public R<List<ProductVO>> getProductsByBrand(@PathVariable Long brandId) {
        try {
            List<ProductVO> products = productService.getProductsByBrand(brandId);
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 新增商品
     * @param productDTO 商品信息
     * @return 新增的商品
     */
    @PostMapping
    @Operation(summary = "新增商品", description = "新增商品信息")
    public R<ProductVO> createProduct(@RequestBody ProductDTO productDTO) {
        try {
            ProductVO productVO = productService.createProduct(productDTO);
            return R.data(productVO, "商品创建成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量更新商品状态
     * @param ids 商品ID列表
     * @param status 状态值
     * @return 操作结果
     */
    @PostMapping("/batch/update-status")
    @Operation(summary = "批量更新商品状态", description = "批量更新商品状态")
    public R<Boolean> batchUpdateStatus(
            @RequestParam List<Long> ids,
            @RequestParam Integer status) {
        try {
            productService.batchUpdateProductStatus(ids, status);
            return R.data(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量删除商品
     * @param ids 商品ID列表
     * @return 操作结果
     */
    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除商品", description = "批量删除商品")
    public R<Boolean> batchDelete(@RequestParam List<Long> ids) {
        try {
            productService.batchDeleteProducts(ids);
            return R.data(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量恢复商品
     * @param ids 商品ID列表
     * @return 操作结果
     */
    @PostMapping("/batch/restore")
    @Operation(summary = "批量恢复商品", description = "批量恢复商品")
    public R<Boolean> batchRestore(@RequestParam List<Long> ids) {
        try {
            for (Long id : ids) {
                productService.restoreProduct(id);
            }
            return R.data(true);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 发布商品（上架）
     * @param id 商品ID
     * @return 操作结果
     */
    @PutMapping("/{id}/publish")
    @Operation(summary = "发布商品", description = "将商品状态设置为上架")
    public R<Boolean> publishProduct(@PathVariable Long id) {
        try {
            productService.updateProductStatus(id, 1); // 1表示上架状态
            return R.data(true, "商品上架成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 下架商品
     * @param id 商品ID
     * @return 操作结果
     */
    @PutMapping("/{id}/unpublish")
    @Operation(summary = "下架商品", description = "将商品状态设置为下架")
    public R<Boolean> unpublishProduct(@PathVariable Long id) {
        try {
            productService.updateProductStatus(id, 0); // 0表示下架状态
            return R.data(true, "商品下架成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新商品
     * @param id 商品ID
     * @param productDTO 商品信息
     * @param confirmEdit 是否确认编辑（已上架商品需要）
     * @return 更新后的商品
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "更新商品信息")
    public R<ProductVO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO, @RequestParam(required = false) Boolean confirmEdit) {
        try {
            ProductVO productVO = productService.updateProduct(id, productDTO, confirmEdit);
            return R.data(productVO, "商品更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取回收站商品列表
     * @return 已删除商品列表
     */
    @GetMapping("/recycle")
    @Operation(summary = "获取回收站商品", description = "获取回收站商品列表")
    public R<List<ProductVO>> getDeletedProducts() {
        try {
            List<ProductVO> products = productService.getDeletedProducts();
            return R.data(products);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取商品SKU列表
     * @param productId 商品ID
     * @return SKU列表
     */
    @GetMapping("/{productId}/skus")
    @Operation(summary = "获取商品SKU列表", description = "获取商品的SKU列表")
    public R<List<ProductSkuVO>> getProductSkus(@PathVariable Long productId) {
        try {
            List<ProductSkuVO> skus = productService.getProductSkus(productId);
            return R.data(skus);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 创建商品SKU
     * @param productId 商品ID
     * @param skuDTO SKU信息
     * @return 创建的SKU
     */
    @PostMapping("/{productId}/skus")
    @Operation(summary = "创建商品SKU", description = "为商品创建SKU")
    public R<ProductSkuVO> createProductSku(@PathVariable Long productId, @RequestBody ProductSkuDTO skuDTO) {
        try {
            ProductSkuVO sku = productService.createProductSku(productId, skuDTO);
            return R.data(sku, "SKU创建成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 批量创建商品SKU
     * @param productId 商品ID
     * @param skuDTOs SKU列表
     * @return 创建的SKU列表
     */
    @PostMapping("/{productId}/skus/batch")
    @Operation(summary = "批量创建商品SKU", description = "批量为商品创建SKU")
    public R<List<ProductSkuVO>> batchCreateProductSkus(@PathVariable Long productId, @RequestBody List<ProductSkuDTO> skuDTOs) {
        try {
            List<ProductSkuVO> skus = productService.batchCreateProductSkus(productId, skuDTOs);
            return R.data(skus, "SKU批量创建成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新商品SKU
     * @param id SKU ID
     * @param skuDTO SKU信息
     * @return 更新后的SKU
     */
    @PutMapping("/skus/{id}")
    @Operation(summary = "更新商品SKU", description = "更新SKU信息")
    public R<ProductSkuVO> updateProductSku(@PathVariable Long id, @RequestBody ProductSkuDTO skuDTO) {
        try {
            ProductSkuVO sku = productService.updateProductSku(id, skuDTO);
            return R.data(sku, "SKU更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除商品SKU
     * @param id SKU ID
     * @return 操作结果
     */
    @DeleteMapping("/skus/{id}")
    @Operation(summary = "删除商品SKU", description = "删除SKU")
    public R<Boolean> deleteProductSku(@PathVariable Long id) {
        try {
            productService.deleteProductSku(id);
            return R.data(true, "SKU删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



