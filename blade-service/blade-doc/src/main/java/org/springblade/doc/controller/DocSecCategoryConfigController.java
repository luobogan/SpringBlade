package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocSecCategoryApproveWfDetail;
import org.springblade.doc.entity.DocSecCategoryCoder;
import org.springblade.doc.entity.DocSecCategoryDocProperty;
import org.springblade.doc.service.IDocCategoryConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容引擎 - 目录配置子页：编号规则 / 文档属性可见性 / 审批工作流明细。
 *
 * <p>全 LambdaQueryWrapper 参数化（守 G2）；写操作一律 POST（守 G1）；端点 @PreAuth（落地规范 §2.2）。</p>
 */
@RestController
@RequestMapping("/doc/category-config")
@RequiredArgsConstructor
@Tag(name = "内容引擎-目录配置", description = "编号规则 / 文档属性可见性 / 审批工作流明细")
public class DocSecCategoryConfigController {

    private final IDocCategoryConfigService configService;

    // ───────────── 编号规则 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/coder/list")
    @Operation(summary = "编号规则列表", description = "按目录查成员（按排序号）")
    public R<List<DocSecCategoryCoder>> listCoder(@RequestParam Long categoryId) {
        return R.data(configService.listCoder(categoryId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/coder/submit")
    @Operation(summary = "保存编号规则成员", description = "新增或更新")
    public R<DocSecCategoryCoder> saveCoder(@RequestBody DocSecCategoryCoder e) {
        return R.data(configService.saveCoder(e));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/coder/delete")
    @Operation(summary = "删除编号规则成员", description = "按 id 删除")
    public R<Boolean> deleteCoder(@RequestParam Long id) {
        return R.data(configService.deleteCoder(id));
    }

    // ───────────── 文档属性可见性 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/property/list")
    @Operation(summary = "文档属性配置列表", description = "按目录查属性可见性/必填/顺序")
    public R<List<DocSecCategoryDocProperty>> listProperty(@RequestParam Long categoryId) {
        return R.data(configService.listProperty(categoryId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/property/submit")
    @Operation(summary = "保存文档属性配置", description = "新增或更新")
    public R<DocSecCategoryDocProperty> saveProperty(@RequestBody DocSecCategoryDocProperty e) {
        return R.data(configService.saveProperty(e));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/property/delete")
    @Operation(summary = "删除文档属性配置", description = "按 id 删除")
    public R<Boolean> deleteProperty(@RequestParam Long id) {
        return R.data(configService.deleteProperty(id));
    }

    // ───────────── 审批工作流明细 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/approve/list")
    @Operation(summary = "审批工作流明细列表", description = "按目录查审批流（生效/失效/批准）")
    public R<List<DocSecCategoryApproveWfDetail>> listApprove(@RequestParam Long categoryId) {
        return R.data(configService.listApprove(categoryId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/approve/submit")
    @Operation(summary = "保存审批工作流明细", description = "新增或更新")
    public R<DocSecCategoryApproveWfDetail> saveApprove(@RequestBody DocSecCategoryApproveWfDetail e) {
        return R.data(configService.saveApprove(e));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/approve/delete")
    @Operation(summary = "删除审批工作流明细", description = "按 id 删除")
    public R<Boolean> deleteApprove(@RequestParam Long id) {
        return R.data(configService.deleteApprove(id));
    }
}
