package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocMould;
import org.springblade.doc.service.IDocMouldService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容引擎 - 显示模板。
 *
 * <p>批量删除为参数化 {@code in (?)}（修 DOC-P0-5 注入）；写操作用 POST。</p>
 */
@RestController
@RequestMapping("/doc/mould")
@RequiredArgsConstructor
@Tag(name = "内容引擎-模板", description = "模板列表/保存/批量删除")
public class DocMouldController {

    private final IDocMouldService mouldService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/list")
    @Operation(summary = "模板列表")
    public R<List<DocMould>> list() {
        return R.data(mouldService.list());
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/submit")
    @Operation(summary = "保存模板")
    public R<DocMould> save(@RequestBody DocMould mould) {
        return R.data(mouldService.save(mould));
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/delete")
    @Operation(summary = "批量删除", description = "参数化 in (?)（修 DOC-P0-5）")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.status(mouldService.batchDelete(ids) > 0);
    }
}
