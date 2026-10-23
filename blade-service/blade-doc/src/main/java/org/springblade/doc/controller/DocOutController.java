package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.mapper.DocDetailMapper;
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
 * 内容引擎 - 外发分享（N-5）。
 *
 * <p><b>修复 E9「外发无 token、无有效期、链接永久有效」</b>：外发链接必须持<b>带时效的签名 token</b>
 * （复用 {@link DocUrlSigner}，默认 5 分钟），过期即拒；从根上杜绝永久有效的外发漏洞。</p>
 */
@RestController
@RequestMapping("/doc/out")
@RequiredArgsConstructor
@Tag(name = "内容引擎-外发分享", description = "签名 + 有效期（闭合 N-5）")
public class DocOutController {

    private final DocUrlSigner signer;
    private final DocDetailMapper detailMapper;
    private final IDocFileStore fileStore;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/sign-url")
    @Operation(summary = "签发外发链接", description = "已登录；返回带时效签名 token 的外发 URL")
    public R<String> signUrl(@RequestParam Long docId) {
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        String path = "/doc/out/" + docId + "/content?token=" + signer.sign("out:" + docId);
        return R.data(path);
    }

    @PreAuth("permitAll()")
    @GetMapping("/{docId}/content")
    @Operation(summary = "外发内容（免登录）", description = "必须持有效签名 token；无 token/过期即拒（闭合 N-5）")
    public ResponseEntity<ByteArrayResource> content(@PathVariable Long docId,
                                                      @RequestParam(required = false) String token) {
        // ★ N-5：外发必须持有时效签名 token，杜绝永久有效
        if (!signer.verify("out:" + docId, token)) {
            throw new ServiceException("外发链接无效或已过期");
        }
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null || doc.getMainFileId() == null) {
            throw new ServiceException("文档不存在或未设置正文");
        }
        Optional<byte[]> bytes = fileStore.download(doc.getMainFileId());
        if (bytes.isEmpty()) {
            throw new ServiceException("文件不存在");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes.get()));
    }
}
