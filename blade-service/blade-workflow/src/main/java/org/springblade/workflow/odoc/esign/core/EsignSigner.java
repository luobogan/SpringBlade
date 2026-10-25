package org.springblade.workflow.odoc.esign.core;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * e签宝 SaaS API V3 签名 / 验签工具。
 *
 * <p>移植自官方 Demo {@code cn.tsign.hz.comm.EsignEncryption}（Apache 2.0），按本项目风格重写。</p>
 *
 * <p><b>⚠️ 两种摘要编码不同，切勿混用</b>：</p>
 * <ul>
 *   <li><b>请求签名</b>：{@code Base64(HmacSHA256(message, secret))}</li>
 *   <li><b>回调验签</b>：{@code hex(HmacSHA256(timestamp + query + body, secret))}</li>
 * </ul>
 */
@Slf4j
public final class EsignSigner {

	private EsignSigner() {
	}

	/** 计算 Content-MD5：MD5 摘要后做 Base64（不是对 32 位十六进制串编码） */
	public static String contentMd5(String body) {
		if (body == null) {
			return "";
		}
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(body.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(md5.digest());
		} catch (Exception e) {
			throw new IllegalStateException("计算 Content-MD5 失败", e);
		}
	}

	/** 请求签名：Base64(HmacSHA256(message, secret)) */
	public static String signature(String message, String secret) {
		if (secret == null || secret.isEmpty()) {
			// 未配置 app-secret：直接返回空签名（调用方会因未就绪而降级），避免 HMAC 抛 Empty key
			return "";
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("计算请求签名失败", e);
		}
	}

	/**
	 * 拼接待签名字符串。
	 *
	 * <p>格式：{@code METHOD\nACCEPT\nContentMD5\nContentType\n[Date]\n[Headers\n]URL}</p>
	 */
	public static String appendSignData(String httpMethod, String contentMd5, String accept,
										String contentType, String headers, String date, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append(httpMethod).append('\n')
			.append(accept).append('\n')
			.append(contentMd5).append('\n')
			.append(contentType).append('\n');
		if (date == null || date.isEmpty()) {
			sb.append('\n');
		} else {
			sb.append(date).append('\n');
		}
		if (headers == null || headers.isEmpty()) {
			sb.append(url);
		} else {
			sb.append(headers).append('\n').append(url);
		}
		return sb.toString();
	}

	/** 时间戳（毫秒） */
	public static String timestamp() {
		return String.valueOf(System.currentTimeMillis());
	}

	/**
	 * 对接口地址中的 Query 参数按字段名 ASCII 字典序排序。
	 *
	 * <p>无 {@code ?} 直接返回；Query 为空则去掉 {@code ?}。</p>
	 */
	public static String sortApiUrl(String apiUrl) {
		if (apiUrl == null || !apiUrl.contains("?")) {
			return apiUrl;
		}
		int idx = apiUrl.indexOf('?');
		String path = apiUrl.substring(0, idx + 1);
		String query = apiUrl.substring(idx + 1);
		if (query.trim().isEmpty()) {
			return apiUrl.substring(0, apiUrl.length() - 1);
		}
		Map<String, String> map = new HashMap<>();
		for (String kv : query.split("&")) {
			int p = kv.indexOf('=');
			if (p <= 0) {
				continue;
			}
			String key = kv.substring(0, p);
			String value = kv.substring(p + 1);
			if (map.containsKey(key)) {
				throw new IllegalArgumentException("Query 参数重复：" + key);
			}
			map.put(key, value);
		}
		List<String> keys = new ArrayList<>(map.keySet());
		// 与官方 Demo 保持一致：先忽略大小写比较；相等时再按大小写敏感反向比较
		keys.sort((o1, o2) -> o1.compareToIgnoreCase(o2) == 0 ? -o1.compareTo(o2) : o1.compareToIgnoreCase(o2));
		StringBuilder qs = new StringBuilder();
		for (int i = 0; i < keys.size(); i++) {
			String k = keys.get(i);
			if (i > 0) {
				qs.append('&');
			}
			qs.append(k).append('=').append(map.get(k));
		}
		return path + qs;
	}

	/** 回调验签：hex(HmacSHA256(timestamp + query + body, secret)) 与签名值忽略大小写比较 */
	public static boolean callbackCheck(String timestamp, String query, String body, String secret, String signature) {
		if (signature == null || signature.isEmpty()) {
			return false;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			mac.update((String.valueOf(timestamp) + (query == null ? "" : query) + (body == null ? "" : body))
				.getBytes(StandardCharsets.UTF_8));
			return toHex(mac.doFinal()).equalsIgnoreCase(signature);
		} catch (Exception e) {
			log.warn("[esign] 回调验签异常：{}", e.getMessage());
			return false;
		}
	}

	/** byte[] → 十六进制小写字符串 */
	private static String toHex(byte[] data) {
		if (data == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			String s = Integer.toHexString(b & 0xFF);
			if (s.length() == 1) {
				sb.append('0');
			}
			sb.append(s);
		}
		return sb.toString();
	}
}
