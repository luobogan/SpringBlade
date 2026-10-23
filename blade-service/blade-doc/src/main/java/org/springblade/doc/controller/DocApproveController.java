package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.service.IDocDetailService;
import org.springblade.doc.sign.DocUrlSigner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容引擎 - 审批回调（流程引擎集成反向）。
 *
 * <p>回调经时效签名 token 保护（复用 {@link DocUrlSigner}）：无效/过期即拒。
 * blade-workflow 侧 {@code WfBizCallbackListener} 检测 {@code DOC:} 标题前缀的流程完成事件后，
 * 经 Feign 调用本端点（需在 blade-workflow 增 IDocClient 客户端并共享签名密钥——跨模块接线，见交付说明）。</p>
 */
@RestController
@RequestMapping("/doc/approve")
@RequiredArgsConstructor
@Tag(name = "内容引擎-审批回调", description = "流程引擎审批完成回调（签名保护）")
public class DocApproveController {

    private final IDocDetailService detailService;
    private final DocUrlSigner signer;

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/callback-sign-url")
    @Operation(summary = "签发审批回调签名 URL", description = "供流程引擎生成带时效签名的回调地址")
    public R<String> signUrl(@RequestParam Long instId) {
        return R.data("/doc/approve/callback?instId=" + instId + "&approved=true&token=" + signer.sign("approve:" + instId));
    }

    @PreAuth("permitAll()")
    @PostMapping("/callback")
    @Operation(summary = "审批完成回调", description = "签名 token 有效则更新文档状态（通过→生效(2)/驳回→退回(4)）")
    public R<Boolean> callback(@RequestParam Long instId,
                               @RequestParam boolean approved,
                               @RequestParam String token) {
        if (!signer.verify("approve:" + instId, token)) {
            throw new ServiceException("回调签名无效或已过期");
        }
        detailService.onApprovalCompleted(instId, approved);
        return R.status(true);
    }
}
