package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.config.WfWakeupProperties;
import org.springblade.workflow.controller.WfWakeupController.WakeupRequest;
import org.springblade.workflow.service.IWfWakeupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * S4 外部唤醒入口（2026-11-18）：默认<b>不存在</b>——{@code blade.workflow.wakeup.external-enabled=true} 时才注册 bean。
 *
 * <p>启用前必须：① 配置 {@code blade.workflow.wakeup.secret}；② 网关 {@code blade.secure.skip-url} 与
 * {@code blade.xss.skip-url} 同时放行 {@code /blade-workflow/wakeup/external}。</p>
 *
 * <p>签名口径：{@code HMAC-SHA256(secret, engineInstId + "|" + nonce + "|" + normalizedBody)}，
 * 请求头 {@code X-WF-Nonce} / {@code X-WF-Signature}（hex）。幂等由服务层保证——重复唤醒找不到等待
 * execution 时返回 false，不产生副作用。</p>
 */
@RestController
@RequestMapping("/wakeup")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blade.workflow.wakeup.external-enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "外部等待唤醒", description = "供外部系统唤醒等待节点（需验签）")
public class WfWakeupExternalController {

    private final IWfWakeupService wakeupService;
    private final WfWakeupProperties properties;

    @PostMapping("/external")
    @PreAuth("permitAll()")
    @Operation(summary = "外部唤醒（HMAC 验签）")
    public R<Boolean> external(@RequestBody WakeupRequest req,
                               @RequestHeader(value = "X-WF-Nonce", required = false) String nonce,
                               @RequestHeader(value = "X-WF-Signature", required = false) String signature) {
        verifySignature(req, nonce, signature);
        if ("message".equalsIgnoreCase(req.getType())) {
            return R.data(wakeupService.signalMessage(req.getEngineInstId(), req.getMessageName(), req.getVariables()));
        }
        return R.data(wakeupService.triggerReceive(req.getEngineInstId(), req.getNodeKey(), req.getVariables()));
    }

    private void verifySignature(WakeupRequest req, String nonce, String signature) {
        if (nonce == null || signature == null || properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new ServiceException("外部唤醒验签失败：缺少 nonce/签名或未配置密钥");
        }
        try {
            String data = String.valueOf(req.getEngineInstId()) + "|" + nonce + "|" + bodyOf(req);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b & 0xff));
            }
            if (!hex.toString().equalsIgnoreCase(signature)) {
                throw new ServiceException("外部唤醒验签失败：签名不匹配");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("外部唤醒验签异常：" + e.getMessage());
        }
    }

    /** 签名用的规范化 body（字段固定顺序，避免前端 JSON 顺序差异） */
    private static String bodyOf(WakeupRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("engineInstId=").append(req.getEngineInstId() == null ? "" : req.getEngineInstId());
        sb.append("&nodeKey=").append(req.getNodeKey() == null ? "" : req.getNodeKey());
        sb.append("&messageName=").append(req.getMessageName() == null ? "" : req.getMessageName());
        sb.append("&variables=").append(req.getVariables() == null ? "" : new java.util.TreeMap<>(req.getVariables()));
        return sb.toString();
    }
}
