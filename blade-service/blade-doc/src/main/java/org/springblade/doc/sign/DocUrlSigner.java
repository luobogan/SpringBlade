package org.springblade.doc.sign;

import org.springblade.doc.config.DocProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.codec.Hex;

/**
 * 内容引擎 URL 签名（HMAC-SHA256 + 时效），加固 OnlyOffice 直连 + 浏览器直连的 content 端点。
 *
 * <p>与 blade-workflow 的 {@code OdocUrlSigner} 同款机制；端点校验：签名有效 或 已登录会话任一通过，
 * 闭合 DOC-P0-4 可枚举下载。</p>
 */
@Component
public class DocUrlSigner {

    private final DocProperties properties;

    public DocUrlSigner(DocProperties properties) {
        this.properties = properties;
    }

    public String sign(String resource) {
        long exp = (System.currentTimeMillis() / 1000) + properties.getUrlSignTtlSeconds();
        return exp + "." + hmac(resource + ":" + exp);
    }

    public boolean verify(String resource, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot < 0) {
            return false;
        }
        try {
            long exp = Long.parseLong(token.substring(0, dot));
            if (System.currentTimeMillis() / 1000 > exp) {
                return false;
            }
            return hmac(resource + ":" + exp).equals(token.substring(dot + 1));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getUrlSignSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(raw));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("doc URL 签名初始化失败", e);
        }
    }
}
