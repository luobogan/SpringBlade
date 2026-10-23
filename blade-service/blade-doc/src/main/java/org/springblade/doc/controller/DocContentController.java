package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.service.IDocLogService;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 内容引擎 - 内容/下载控制器。
 *
 * <p>闭合 DOC-P0-4（对齐《落地规范》§3/§5-2.5）：下载端点「签名 token 或已登录会话」任一通过，
 * 否则 fail-closed 拒绝；fileId 经签名绑定、不可裸枚举。</p>
 */
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
@Tag(name = "内容引擎-内容/下载", description = "签名 URL 或已登录会话任一通过（闭合 DOC-P0-4）")
public class DocContentController {

    private final IDocFileStore fileStore;
    private final DocUrlSigner signer;
    private final IDocLogService docLogService;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/sign-url")
    @Operation(summary = "签发内容签名 URL", description = "已登录换取带 token 的下载地址")
    public R<String> signUrl(@RequestParam String type, @RequestParam(required = false) Long id) {
        String path;
        String resource;
        if ("file".equals(type)) {
            path = "/doc/file/" + id + "/content";
            resource = String.valueOf(id);
        } else {
            throw new ServiceException("未知签名类型: " + type);
        }
        return R.data(path + "?token=" + signer.sign(resource));
    }

    @PreAuth("permitAll()")
    @GetMapping("/file/{fileId}/content")
    @Operation(summary = "按 fileId 取文件内容", description = "签名 token 或已登录会话任一通过；否则拒绝（闭合 DOC-P0-4）")
    public ResponseEntity<ByteArrayResource> fileContent(@PathVariable Long fileId,
                                                          @RequestParam(required = false) String token) {
        if (!authorized(String.valueOf(fileId), token)) {
            throw new ServiceException("无权限访问该文件内容");
        }
        Optional<byte[]> bytes = fileStore.download(fileId);
        if (bytes.isEmpty()) {
            throw new ServiceException("文件不存在");
        }
        docLogService.logDownload(null, fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes.get()));
    }

    /** 签名 token 或已登录会话任一通过 */
    private boolean authorized(String resource, String token) {
        return signer.verify(resource, token) || SecureUtil.getUser() != null;
    }
}
