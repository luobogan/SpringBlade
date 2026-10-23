package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.service.IDocAttachmentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 内容引擎 - 附件管理（列表/上传/重命名/删除）。
 *
 * <p>写操作一律 POST（守 G1）；下载经已有签名链路（{@code /doc/sign-url}，守 DOC-P0-4）。</p>
 */
@RestController
@RequestMapping("/doc/attach")
@RequiredArgsConstructor
@Tag(name = "内容引擎-附件管理", description = "文档附件列表/上传/重命名/删除")
public class DocAttachmentController {

    private final IDocAttachmentService attachmentService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/list")
    @Operation(summary = "附件列表", description = "记录级 canRead")
    public R<List<DocImageFile>> list(@RequestParam Long docId) {
        return R.data(attachmentService.list(docId));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传附件", description = "canManage 校验后落盘并建附件记录")
    public R<DocImageFile> upload(@RequestParam Long docId,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) Integer docFileType) {
        try {
            String name = file.getOriginalFilename();
            return R.data(attachmentService.upload(docId, file.getBytes(), name, docFileType));
        } catch (IOException e) {
            throw new org.springblade.core.log.exception.ServiceException("文件读取失败");
        }
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/register")
    @Operation(summary = "登记系统通用附件", description = "前端直传 OSS 后回写元数据；canManage 校验后建外部附件记录(isExtFile=1)")
    public R<DocImageFile> register(@RequestParam Long docId,
                                    @RequestParam String ossFileId,
                                    @RequestParam String ossFileUrl,
                                    @RequestParam String fileName,
                                    @RequestParam(required = false) Long fileSize,
                                    @RequestParam(required = false) Integer docFileType) {
        return R.data(attachmentService.registerExternal(docId, ossFileId, ossFileUrl, fileName, fileSize, docFileType));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/rename")
    @Operation(summary = "重命名附件", description = "canManage 校验")
    public R<DocImageFile> rename(@RequestParam Long id, @RequestParam String fileName) {
        return R.data(attachmentService.rename(id, fileName));
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping("/delete")
    @Operation(summary = "删除附件", description = "canManage 校验")
    public R<Boolean> delete(@RequestParam Long id) {
        return R.data(attachmentService.delete(id));
    }
}
