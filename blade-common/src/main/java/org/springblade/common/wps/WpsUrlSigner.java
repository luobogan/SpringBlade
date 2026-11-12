package org.springblade.common.wps;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * WPS WebOffice 内容/回调 URL 签名（HMAC-SHA256 + 时效），纯 JDK 实现。
 *
 * <p>收编原先 {@code DocUrlSigner}(blade-doc) 与 {@code OdocUrlSigner}(blade-workflow)
 * 两份完全同款的实现，签名串格式保持 {@code exp.hex(hmac(resource:exp))} 不变，
 * 因此存量已签发 URL 与回调 token 均兼容。</p>
 *
 * <p><b>跨服务约束</b>：两侧必须使用同一 {@code urlSignSecret}；否则 A 服务签发的
 * content URL 在 B 服务无法通过校验。</p>
 */
public class WpsUrlSigner {

	private final Supplier<String> secretSupplier;
	private final IntSupplier ttlSupplier;

	public WpsUrlSigner(String secret, int ttlSeconds) {
		this(secret == null ? () -> "" : () -> secret, () -> ttlSeconds > 0 ? ttlSeconds : 300);
	}

	public WpsUrlSigner(WpsProperties properties) {
		this(properties.getUrlSignSecret(), properties.getUrlSignTtlSeconds());
	}

	/**
	 * 动态密钥构造：每次签发/校验现场取当前生效配置，供「编辑器设置」页面保存后立即生效
	 * （单一下发源为 {@code doc_engine_setting} 库表），无需重启服务。
	 */
	public WpsUrlSigner(Supplier<String> secretSupplier, IntSupplier ttlSupplier) {
		this.secretSupplier = secretSupplier == null ? () -> "" : secretSupplier;
		this.ttlSupplier = ttlSupplier == null ? () -> 300 : ttlSupplier;
	}

	/** 签发带时效的 token：{@code exp.hexHmac} */
	public String sign(String resource) {
		long exp = (System.currentTimeMillis() / 1000) + Math.max(1, ttlSupplier.getAsInt());
		return exp + "." + hmac(resource + ":" + exp);
	}

	/** 校验 token：未过期且 HMAC 相等 */
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
			String secret = secretSupplier.get();
			if (secret == null) {
				secret = "";
			}
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return hex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("WPS URL 签名初始化失败", e);
		}
	}

	/** 小写 hex，与 Spring {@code Hex.encode} 输出一致（保证存量 token 兼容） */
	private static String hex(byte[] raw) {
		StringBuilder sb = new StringBuilder(raw.length * 2);
		for (byte b : raw) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16));
			sb.append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}
}
