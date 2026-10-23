package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocSecCategory;
import org.springblade.doc.service.ISecCategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容引擎 - 目录管理控制器。
 *
 * <p>对齐《泛微E9-内容引擎-SpringBlade落地规范》§3：每个端点 {@code @PreAuth}；
 * 内部 QueryWrapper 参数化（守 DOC-P0-2 / G2）；写操作用 POST/DELETE（守 G1）；
 * 分部隔离由 DataScope 追加（守 FNA-N1 / DET-P0-2 / CUBE-N1）。</p>
 */
@RestController
@RequestMapping("/doc/category")
@RequiredArgsConstructor
@Tag(name = "内容引擎-目录管理", description = "目录树/新建更新/删除")
public class DocSecCategoryController {

    private final ISecCategoryService categoryService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/tree")
    @Operation(summary = "目录列表", description = "已登录即可见；sub_company_id 分部隔离由 DataScope 统一追加")
    public R<List<DocSecCategory>> tree() {
        return R.data(categoryService.listTree());
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/submit")
    @Operation(summary = "新建/更新目录", description = "内容管理员；QueryWrapper 参数化")
    public R<DocSecCategory> submit(@RequestBody DocSecCategory category) {
        return R.data(categoryService.submit(category));
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @DeleteMapping("/{id}")
    @Operation(summary = "删除目录", description = "内容管理员；有子目录 fail-closed 拒绝")
    public R<Boolean> remove(@PathVariable Long id) {
        return R.status(categoryService.remove(id));
    }
}
