package org.springblade.mall.controller;

import org.springblade.mall.dto.CategoryDTO;
import org.springblade.mall.service.CategoryService;
import org.springblade.mall.vo.CategoryVO;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 分类控制器
 */
@RestController
@AllArgsConstructor
//AppConstant.APPLICATION_MALL_NAME +
@RequestMapping("/categories")
@Tag(name = "分类管理", description = "分类管理")
public class CategoryController extends BladeController {

    private CategoryService categoryService;

    /**
     * 从请求头获取租户ID，优先使用Token中的，否则使用Header中的
     * @return 租户ID
     */
    private String getTenantId() {
        // 优先从JWT Token获取（已登录用户）
        String tenantId = SecureUtil.getTenantId();
        if (org.springblade.core.tool.utils.StringUtil.isNotBlank(tenantId)) {
            return tenantId;
        }
        // 未登录时从请求头获取（前端发送的 Tenant-Id: 000000）
        HttpServletRequest request = org.springblade.core.tool.utils.WebUtil.getRequest();
        if (request != null) {
            tenantId = request.getHeader("Tenant-Id");
        }
        return tenantId;
    }

    /**
     * 创建分类
     * @param categoryDTO 分类信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "创建分类")
    public R<CategoryVO> createCategory(@Parameter(description = "分类信息") @Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryVO categoryVO = TenantUtil.use(getTenantId(), () -> categoryService.createCategory(categoryDTO));
            R<CategoryVO> result = R.data(categoryVO);
            result.setMsg("分类创建成功");
            return result;
        } catch (Exception e) {
            // 记录详细错误日志
            System.err.println("创建分类失败: " + e.getMessage());
            e.printStackTrace();
            // 返回简化后的错误消息给前端
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Out of range value")) {
                return R.fail("parentId 值无效，请选择有效的父分类");
            }
            return R.fail(errorMsg);
        }
    }

    /**
     * 更新分类
     * @param id 分类ID
     * @param categoryDTO 分类信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新分类")
    public R<CategoryVO> updateCategory(@Parameter(description = "分类ID") @PathVariable Long id, @Parameter(description = "分类信息") @Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryVO categoryVO = TenantUtil.use(getTenantId(), () -> categoryService.updateCategory(id, categoryDTO));
            R<CategoryVO> result = R.data(categoryVO);
            result.setMsg("分类更新成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除分类")
    public R<?> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            TenantUtil.use(getTenantId(), () -> { categoryService.deleteCategory(id); return null; });
            return R.success("分类删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "获取分类详情")
    public R<CategoryVO> getCategoryById(@Parameter(description = "分类ID") @PathVariable Long id) {
        try {
            CategoryVO categoryVO = TenantUtil.use(getTenantId(), () -> categoryService.getCategoryById(id));
            return R.data(categoryVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取所有分类（包括禁用状态）
     * @return 分类列表
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "获取所有分类")
    public R<List<CategoryVO>> getAllCategories() {
        try {
            List<CategoryVO> categories = TenantUtil.use(getTenantId(), () -> categoryService.getAllCategoriesWithStatus());
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类树（包括禁用状态）
     * @return 分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取分类树")
    public R<List<CategoryVO>> getCategoryTree() {
        try {
            List<CategoryVO> categories = TenantUtil.use(getTenantId(), () -> categoryService.getAllCategoriesWithStatus());
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取子分类（包括禁用状态）
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @GetMapping("/sub/{parentId}")
    @Operation(summary = "获取子分类", description = "获取子分类")
    public R<List<CategoryVO>> getSubCategories(@Parameter(description = "父分类ID") @PathVariable Long parentId) {
        try {
            List<CategoryVO> categories = TenantUtil.use(getTenantId(), () -> categoryService.getSubCategoriesWithStatus(parentId));
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取活跃分类
     * @return 活跃分类列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃分类", description = "获取活跃分类")
    public R<List<CategoryVO>> getActiveCategories() {
        try {
            List<CategoryVO> categories = TenantUtil.use(getTenantId(), () -> categoryService.getActiveCategories());
            return R.data(categories);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新分类排序
     * @param sortData 排序数据
     * @return 更新结果
     */
    @PutMapping("/sort")
    @Operation(summary = "更新分类排序", description = "更新分类排序")
    public R<?> updateCategorySort(@RequestBody List<org.springblade.mall.service.CategoryService.SortRequest> sortData) {
        try {
            TenantUtil.use(getTenantId(), () -> { categoryService.updateCategorySort(sortData); return null; });
            return R.success("排序更新成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
