package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocRecycle;
import org.springblade.doc.service.IDocRecycleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容引擎 - 回收站。
 *
 * <p><b>DET-P0-1 修复</b>：E9 回收站列表权限被整段注释致任意登录用户可拉取。
 * 本控制器列表/恢复/彻底删除一律 {@code @PreAuth(HAS_ROLE_DOC)}（内容管理员），宁缺勿滥。</p>
 */
@RestController
@RequestMapping("/doc/recycle")
@RequiredArgsConstructor
@Tag(name = "内容引擎-回收站", description = "列表/恢复/彻底删除（内容管理员）")
public class DocRecycleController {

    private final IDocRecycleService recycleService;

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @GetMapping("/list")
    @Operation(summary = "回收站列表", description = "内容管理员；修 DET-P0-1；支持按 secCategoryId 过滤")
    public R<List<DocRecycle>> list(@RequestParam(required = false) Long secCategoryId) {
        return R.data(recycleService.list(secCategoryId));
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/restore")
    @Operation(summary = "恢复文档", description = "内容管理员")
    public R<Boolean> restore(@RequestParam Long docId) {
        return R.status(recycleService.restore(docId));
    }

    /**
     * 移入回收站（删除文档）。
     *
     * <p>与 list/restore/purge 的角色门禁不同：删除是「谁创建/管理该文档谁可删」，
     * 故控制器只要求登录（{@code HAS_AUTH}），文档级权限由服务层 canManage 判定（fail-closed）。</p>
     */
    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/remove")
    @Operation(summary = "移入回收站", description = "删除文档（逻辑删除），可在回收站恢复")
    public R<Boolean> remove(@RequestParam Long docId) {
        return R.status(recycleService.removeToRecycle(docId));
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/purge")
    @Operation(summary = "彻底删除", description = "内容管理员")
    public R<Boolean> purge(@RequestParam Long docId) {
        return R.status(recycleService.purge(docId));
    }
}
