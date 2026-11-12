package org.springblade.doc.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * WPS 文档中台 SHA1 签名（{@code _w_*} 参数族）——<b>未启用，P3.4 降级保留</b>。
 *
 * <p>⚠️ 全项目零调用点，{@code DocEditorConfig.wpsSignature} 从未赋值，SDK 加载器
 * （v2.0.7）中也检不出 {@code _w_signature} / {@code X-WebOffice-Token}。
 * 当前实际鉴权链路是：{@code WpsUrlSigner} 签发 {@code wps:{fileId}} token →
 * 前端 {@code WebOfficeSDK.init({token})} → WPS 中台回调经 {@code X-WebOffice-Token} 回传 →
 * {@code WpsCallbackSupport.tokenValid} 硬校验（P3.3）。<b>不要把本类当作"已有验签"沿用。</b></p>
 *
 * <p>保留原因：v2.0.7 的回调签名口径尚未在 WPS 中台联调中确认（方案 §0.2 ③ / P2.7）。
 * 若联调证实中台使用 {@code _w_signature}，需重新核验本类算法后<b>再</b>接入，
 * 并在 {@code WpsCallbackController#authGuard} 中增加对应校验分支。未确认前请勿调用。</p>
 *
 * @deprecated 未启用且签名口径未核验，见类注释；实际鉴权走 {@link org.springblade.common.wps.WpsUrlSigner}。
 */
@Deprecated
public final class WpsSign {

    private WpsSign() {
    }

    /** 客户端 jssdk 签名：固定 _w_appid/_w_fileid/_w_time/_w_permission/_w_fname/_w_userid */
    public static String clientSign(String appId, String fileId, long time, String permission,
                                   String fname, String userId, String secret) {
        TreeMap<String, String> m = new TreeMap<>();
        m.put("_w_appid", appId);
        m.put("_w_fileid", fileId);
        m.put("_w_time", String.valueOf(time));
        m.put("_w_permission", permission);
        m.put("_w_fname", fname);
        m.put("_w_userid", userId);
        return sha1Hex(sortedKeyValue(m) + secret).toUpperCase();
    }

    /** 服务端回调校验：对请求中所有 _w_* 参数（排除 _w_signature）按规则签名并与 _w_signature 比对 */
    public static boolean serverVerify(Map<String, String> params, String secret) {
        String sig = params.get("_w_signature");
        if (sig == null || sig.isBlank()) {
            return false;
        }
        TreeMap<String, String> m = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("_w_") && !"_w_signature".equals(k) && e.getValue() != null) {
                m.put(k, e.getValue());
            }
        }
        return sha1Hex(sortedKeyValue(m) + secret).equalsIgnoreCase(sig);
    }

    private static String sortedKeyValue(TreeMap<String, String> m) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static String sha1Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 不可用", e);
        }
    }
}
