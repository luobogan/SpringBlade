package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.service.IDocCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - 知识中心（排行榜 / 最近文档 / 推荐阅读 / 我的文档数）。
 *
 * <p>只读 GET + @PreAuth；聚合查询参数化（守 G2）。</p>
 */
@RestController
@RequestMapping("/doc/center")
@RequiredArgsConstructor
@Tag(name = "内容引擎-知识中心", description = "排行榜/最近文档/推荐阅读/我的文档数")
public class DocCenterController {

    private final IDocCenterService centerService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/rank")
    @Operation(summary = "文档排行榜", description = "按点赞数聚合")
    public R<List<Map<String, Object>>> rank(@RequestParam(defaultValue = "10") int limit) {
        return R.data(centerService.rank(limit));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/latest")
    @Operation(summary = "最近文档", description = "按创建时间倒序")
    public R<List<DocDetail>> latest(@RequestParam(defaultValue = "10") int limit) {
        return R.data(centerService.latest(limit));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/recommend")
    @Operation(summary = "推荐阅读", description = "按评分均值聚合")
    public R<List<Map<String, Object>>> recommend(@RequestParam(defaultValue = "10") int limit) {
        return R.data(centerService.recommend(limit));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/mine")
    @Operation(summary = "我的文档数", description = "当前登录人创建文档统计")
    public R<Map<String, Object>> mine() {
        return R.data(centerService.mine());
    }
}
