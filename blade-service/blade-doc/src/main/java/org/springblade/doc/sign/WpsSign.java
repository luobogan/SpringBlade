package org.springblade.doc.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * WPS 文档中台签名（前后端同一规则）。
 *
 * <p>参与签名的参数按 key 字典序排序，拼接为 {@code key=value&key=value…}，追加 appSecret，做 SHA1。
 * 客户端签名转大写（透传给 jssdk 校验）；服务端校验大小写不敏感（WPS 中台下发，个别版本大小写不一致）。</p>
 */
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
