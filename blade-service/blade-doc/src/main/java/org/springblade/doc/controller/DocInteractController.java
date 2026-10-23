package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocReply;
import org.springblade.doc.entity.DocScore;
import org.springblade.doc.service.IDocInteractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内容引擎 - 文档交互（回复 / 点赞 / 评分）。
 *
 * <p>写操作一律 POST（守 G1，对治 E9「doPraise/addShare 等 GET 写」）；
 * 读取走记录级 canRead（落地规范 §3）。</p>
 */
@RestController
@RequestMapping("/doc/interact")
@RequiredArgsConstructor
@Tag(name = "内容引擎-文档交互", description = "回复 / 点赞 / 评分")
public class DocInteractController {

    private final IDocInteractService interactService;

    // ───────────── 回复 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/reply/list")
    @Operation(summary = "回复列表", description = "记录级 canRead")
    public R<List<DocReply>> listReply(@RequestParam Long docId) {
        return R.data(interactService.listReply(docId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/reply/save")
    @Operation(summary = "发表回复", description = "支持子回复(parentId)")
    public R<DocReply> saveReply(@RequestParam Long docId,
                                 @RequestParam String content,
                                 @RequestParam(required = false) Long parentId) {
        return R.data(interactService.saveReply(docId, content, parentId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/reply/delete")
    @Operation(summary = "删除回复", description = "仅回复本人或文档管理者")
    public R<Boolean> deleteReply(@RequestParam Long id) {
        return R.data(interactService.deleteReply(id));
    }

    // ───────────── 点赞 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/praise/info")
    @Operation(summary = "点赞概况", description = "{count, praised}")
    public R<Map<String, Object>> praiseInfo(@RequestParam Long docId) {
        return R.data(interactService.praiseInfo(docId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/praise/toggle")
    @Operation(summary = "点赞/取消", description = "幂等切换，返回切换后是否已赞")
    public R<Boolean> togglePraise(@RequestParam Long docId) {
        return R.data(interactService.togglePraise(docId));
    }

    // ───────────── 评分 ─────────────
    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/score/info")
    @Operation(summary = "评分概况", description = "{avg, count, myScore}")
    public R<Map<String, Object>> scoreInfo(@RequestParam Long docId) {
        return R.data(interactService.scoreInfo(docId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/score/save")
    @Operation(summary = "提交评分", description = "1-5 分，一人一评（重复提交即更新）")
    public R<DocScore> saveScore(@RequestParam Long docId, @RequestParam int score) {
        return R.data(interactService.saveScore(docId, score));
    }
}
