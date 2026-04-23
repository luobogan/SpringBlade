package org.springblade.mall.controller;

import org.springblade.mall.service.BrandService;
import org.springblade.mall.vo.BrandVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端品牌控制器
 * 处理前端的品牌相关请求，路径为 /brands
 */
@RestController
@AllArgsConstructor
@RequestMapping("/front/brands")
@Tag(name = "前端品牌管理", description = "前端品牌管理")
public class FrontBrandController extends BladeController {

    private BrandService brandService;

    /**
     * 获取所有品牌
     * @return 品牌列表
     */
    @GetMapping
    @Operation(summary = "获取所有品牌", description = "获取所有品牌")
    public R<List<BrandVO>> getAllBrands() {
        try {
            List<BrandVO> brands = brandService.getAllBrands();
            return R.data(brands);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取活跃品牌
     * @return 活跃品牌列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃品牌", description = "获取活跃品牌")
    public R<List<BrandVO>> getActiveBrands() {
        try {
            List<BrandVO> brands = brandService.getActiveBrands();
            return R.data(brands);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据ID获取品牌
     * @param id 品牌ID
     * @return 品牌详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取品牌", description = "根据ID获取品牌")
    public R<BrandVO> getBrandById(@Parameter(description = "品牌ID") @PathVariable Long id) {
        try {
            BrandVO brand = brandService.getBrandById(id);
            return R.data(brand);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}



