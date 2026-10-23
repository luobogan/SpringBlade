package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.service.IDocSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容引擎 - 文档检索（C-10）。
 *
 * <p>记录级权限可见性（{@link IDocSearchService}，等价 E9 DocShareUtil.getOnlyHasRight）：
 * 仅返回「我创建的 + 共享给我 + 我管理的目录」文档；超管/内容管理员返回租户内全部。</p>
 */
@RestController
@RequestMapping("/doc/search")
@RequiredArgsConstructor
@Tag(name = "内容引擎-检索", description = "文档分页检索（记录级权限可见性）")
public class DocSearchController {

    private final IDocSearchService searchService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/page")
    @Operation(summary = "文档分页检索", description = "记录级权限可见性裁剪（创建/共享/管理）")
    public R<IPage<DocDetail>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Long secCategoryId) {
        return R.data(searchService.page(current, size, keyword, secCategoryId));
    }
}
