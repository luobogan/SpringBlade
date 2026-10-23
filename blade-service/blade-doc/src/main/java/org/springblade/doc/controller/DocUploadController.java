package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 内容引擎 - 文件上传（上传加固，对齐 E9 maxUploadFileSize + 白名单）。
 *
 * <p>双重校验：① 扩展名白名单（{@link DocProperties#getAllowedFileTypes()}）；
 * ② 大小上限（{@link DocProperties#getMaxUploadSize()} MB）。任一不满足即拒绝。</p>
 */
@RestController
@RequestMapping("/doc/upload")
@RequiredArgsConstructor
@Tag(name = "内容引擎-上传", description = "文件上传（大小 + 类型白名单加固）")
public class DocUploadController {

    private final IDocFileStore fileStore;
    private final DocProperties properties;

    @PreAuth(DocConstant.HAS_AUTH)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "校验大小(≤maxUploadSize MB)与扩展名白名单，返回 fileId")
    public R<Long> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
            ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
            : "";
        if (!properties.getAllowedFileTypes().contains(ext)) {
            throw new ServiceException("不支持的文件类型: " + ext);
        }
        long maxBytes = (long) properties.getMaxUploadSize() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ServiceException("文件超过大小限制 " + properties.getMaxUploadSize() + "MB");
        }
        try {
            Long fileId = fileStore.save(file.getBytes());
            return R.data(fileId);
        } catch (IOException e) {
            throw new ServiceException("文件读取失败");
        }
    }
}
