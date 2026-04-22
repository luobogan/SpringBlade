package org.springblade.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.mall.dto.CategoryParamTemplateDTO;
import org.springblade.mall.service.CategoryParamTemplateService;
import org.springblade.mall.vo.CategoryParamTemplateVO;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类参数模板控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/admin/category-param-templates")
@Tag(name = "分类参数模板管理", description = "分类参数模板管理")
public class CategoryParamTemplateController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(CategoryParamTemplateController.class);

    private CategoryParamTemplateService categoryParamTemplateService;

    /**
     * 简单测试端点
     */
    @GetMapping("/test")
    @Operation(summary = "测试端点", description = "测试端点")
    public R<String> test() {
        log.info("收到测试请求");
        return R.success("测试成功");
    }

    /**
     * 创建参数模板
     */
    @PostMapping
    @Operation(summary = "创建参数模板", description = "创建参数模板")
    public R<CategoryParamTemplateVO> createParamTemplate(
            @Parameter(description = "参数模板信息") @Valid @RequestBody CategoryParamTemplateDTO dto) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.createParamTemplate(dto);
            R<CategoryParamTemplateVO> result = R.data(paramTemplate);
            result.setMsg("参数模板创建成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新参数模板
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新参数模板", description = "更新参数模板")
    public R<CategoryParamTemplateVO> updateParamTemplate(
            @Parameter(description = "参数模板ID") @PathVariable Long id,
            @Parameter(description = "参数模板信息") @Valid @RequestBody CategoryParamTemplateDTO dto) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.updateParamTemplate(id, dto);
            R<CategoryParamTemplateVO> result = R.data(paramTemplate);
            result.setMsg("参数模板更新成功");
            return result;
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 删除参数模板
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除参数模板", description = "删除参数模板")
    public R<?> deleteParamTemplate(@Parameter(description = "参数模板ID") @PathVariable Long id) {
        try {
            categoryParamTemplateService.deleteParamTemplate(id);
            return R.success("参数模板删除成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取参数模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取参数模板详情", description = "获取参数模板详情")
    public R<CategoryParamTemplateVO> getParamTemplateById(@Parameter(description = "参数模板ID") @PathVariable Long id) {
        try {
            CategoryParamTemplateVO paramTemplate = categoryParamTemplateService.getParamTemplateById(id);
            return R.data(paramTemplate);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 获取分类的所有参数模板
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "获取分类的所有参数模板", description = "获取分类的所有参数模板")
    public R<List<CategoryParamTemplateVO>> getParamTemplatesByCategoryId(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        log.info("收到获取分类参数模板请求，categoryId: {}", categoryId);
        try {
            List<CategoryParamTemplateVO> paramTemplates = categoryParamTemplateService.getParamTemplatesByCategoryId(categoryId);
            log.info("返回参数模板数据，数量: {}", paramTemplates.size());
            return R.data(paramTemplates);
        } catch (Exception e) {
            log.error("获取分类参数模板失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询参数模板
     */
    @GetMapping
    @Operation(summary = "分页查询参数模板", description = "分页查询参数模板")
    public R<IPage<CategoryParamTemplateVO>> getParamTemplatePage(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            IPage<CategoryParamTemplateVO> result = categoryParamTemplateService.getParamTemplatePage(categoryId, page, pageSize);
            return R.data(result);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 复制参数模板到其他分类
     */
    @PostMapping("/copy")
    @Operation(summary = "复制参数模板到其他分类", description = "复制参数模板到其他分类")
    public R<?> copyParamTemplatesToCategory(
            @Parameter(description = "复制参数请求") @RequestBody CopyParamRequest request) {
        try {
            categoryParamTemplateService.copyParamTemplatesToCategory(request.getSourceCategoryId(), request.getTargetCategoryId());
            return R.success("复制成功");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 复制参数请求DTO
     */
    static class CopyParamRequest {
        private Long sourceCategoryId;
        private Long targetCategoryId;

        public Long getSourceCategoryId() {
            return sourceCategoryId;
        }

        public void setSourceCategoryId(Long sourceCategoryId) {
            this.sourceCategoryId = sourceCategoryId;
        }

        public Long getTargetCategoryId() {
            return targetCategoryId;
        }

        public void setTargetCategoryId(Long targetCategoryId) {
            this.targetCategoryId = targetCategoryId;
        }
    }
}



