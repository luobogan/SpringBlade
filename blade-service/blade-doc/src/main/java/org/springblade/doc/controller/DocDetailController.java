package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.service.IDocDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容引擎 - 文档详情/保存/提交。
 *
 * <p>保存/提交的编辑权校验在服务层显式执行（修 DOC-P0-3，绝不禁用）；
 * 读取走记录级 canRead（落地规范 §3）。</p>
 */
@RestController
@RequestMapping("/doc/detail")
@RequiredArgsConstructor
@Tag(name = "内容引擎-文档详情", description = "详情/保存/提交")
public class DocDetailController {

    private final IDocDetailService detailService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/{id}")
    @Operation(summary = "文档详情", description = "已登录 + 记录级 canRead")
    public R<DocDetail> detail(@PathVariable Long id) {
        if (!detailService.canRead(id)) {
            throw new ServiceException("无权限查看该文档");
        }
        return R.data(detailService.getById(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/save")
    @Operation(summary = "保存草稿", description = "更新时显式 canManage 校验（修 DOC-P0-3）")
    public R<DocDetail> save(@RequestBody DocDetail doc) {
        return R.data(detailService.saveDoc(doc));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/submit")
    @Operation(summary = "提交", description = "显式 canManage + 状态机流转")
    public R<DocDetail> submit(@RequestParam Long id) {
        return R.data(detailService.submit(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/publish")
    @Operation(summary = "发布", description = "显式 canManage + 状态流转 → 生效(2)")
    public R<DocDetail> publish(@RequestParam Long id) {
        return R.data(detailService.publish(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/invalidate")
    @Operation(summary = "失效", description = "生效 → 失效(7)")
    public R<DocDetail> invalidate(@RequestParam Long id) {
        return R.data(detailService.invalidate(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/archive")
    @Operation(summary = "归档", description = "生效 → 归档(5)")
    public R<DocDetail> archive(@RequestParam Long id) {
        return R.data(detailService.archive(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/reopen")
    @Operation(summary = "重新打开", description = "失效/归档/作废 → 生效(1)")
    public R<DocDetail> reopen(@RequestParam Long id) {
        return R.data(detailService.reopen(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/void")
    @Operation(summary = "作废", description = "→ 作废(8)")
    public R<DocDetail> voidDoc(@RequestParam Long id) {
        return R.data(detailService.voidDoc(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/content")
    @Operation(summary = "读取正文", description = "记录级 canRead + 返回正文 HTML")
    public R<String> getContent(@RequestParam Long docId) {
        return R.data(detailService.getContent(docId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/content/save")
    @Operation(summary = "保存正文", description = "canManage 校验后写入 doc_detail_content")
    public R<Boolean> saveContent(@RequestParam Long docId, @RequestParam(required = false) String content) {
        detailService.saveContent(docId, content);
        return R.status(true);
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/check-out")
    @Operation(summary = "签出", description = "canManage + 未被签出时置签出状态与签出人")
    public R<DocDetail> checkOut(@RequestParam Long id) {
        return R.data(detailService.checkOut(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/check-in")
    @Operation(summary = "签入", description = "仅签出本人或文档管理者可签入")
    public R<DocDetail> checkIn(@RequestParam Long id) {
        return R.data(detailService.checkIn(id));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/force-check-in")
    @Operation(summary = "强制签入", description = "仅管理员/内容管理员，解除他人签出")
    public R<DocDetail> forceCheckIn(@RequestParam Long id) {
        return R.data(detailService.forceCheckIn(id));
    }
}
