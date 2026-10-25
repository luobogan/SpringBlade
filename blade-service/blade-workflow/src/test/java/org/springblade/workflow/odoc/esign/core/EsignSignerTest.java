package org.springblade.workflow.odoc.esign.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EsignSigner} 签名/验签纯逻辑单测（不依赖网络与 Spring 容器）。
 *
 * <p>锁定两条最容易踩错的约定：</p>
 * <ul>
 *   <li>回调验签 = <b>hex</b>(HmacSHA256(timestamp + query + body))，忽略大小写比较；</li>
 *   <li>请求签名 = <b>Base64</b>(HmacSHA256(message))，与回调验签编码不同、不可混用。</li>
 * </ul>
 */
class EsignSignerTest {

	private static final String SECRET = "test-app-secret";

	private static String hexHmac(String message, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] data = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : data) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	@DisplayName("回调验签：hex 摘要且忽略大小写时通过")
	void callbackCheckHexIgnoreCase() {
		String timestamp = "1717171717171";
		String query = "businessId=abc";
		String body = "{\"action\":\"sign\"}";
		String sign = hexHmac(timestamp + query + body, SECRET);

		assertTrue(EsignSigner.callbackCheck(timestamp, query, body, SECRET, sign));
		assertTrue(EsignSigner.callbackCheck(timestamp, query, body, SECRET, sign.toUpperCase()));
	}

	@Test
	@DisplayName("回调验签：报文被改写 / 签名缺失 → 失败")
	void callbackCheckFails() {
		String timestamp = "1717171717171";
		String query = "businessId=abc";
		String body = "{\"action\":\"sign\"}";
		String sign = hexHmac(timestamp + query + body, SECRET);

		// body 被改写（XSS 过滤器改写报文即属此类）
		assertFalse(EsignSigner.callbackCheck(timestamp, query, "{\"action\":\"sign2\"}", SECRET, sign));
		// query 不一致
		assertFalse(EsignSigner.callbackCheck(timestamp, "businessId=xyz", body, SECRET, sign));
		// 时间戳不一致（重放）
		assertFalse(EsignSigner.callbackCheck("1717171717172", query, body, SECRET, sign));
		// 密钥不一致
		assertFalse(EsignSigner.callbackCheck(timestamp, query, body, "other-secret", sign));
		// 签名缺失
		assertFalse(EsignSigner.callbackCheck(timestamp, query, body, SECRET, null));
		assertFalse(EsignSigner.callbackCheck(timestamp, query, body, SECRET, ""));
	}

	@Test
	@DisplayName("请求签名与回调验签编码不同：Base64 签名不能通过 hex 验签")
	void requestSignatureIsBase64NotHex() {
		String message = "GET\napplication/json\n\n\nhttps://openapi.esign.cn/v3/seals";
		String base64 = EsignSigner.signature(message, SECRET);
		String hex = hexHmac(message, SECRET);

		assertNotEquals(base64, hex);
		assertEquals(Base64.getEncoder().encodeToString(hexHmacBytes(message, SECRET)), base64);
		// 用 Base64 去走回调验签口径必然不通过（防止实现里混用）
		assertFalse(EsignSigner.callbackCheck("", "", message, SECRET, base64));
	}

	@Test
	@DisplayName("Content-MD5：MD5 摘要后 Base64")
	void contentMd5() {
		String body = "{\"a\":1}";
		String md5 = EsignSigner.contentMd5(body);
		// Base64 长度固定 24（16 字节摘要 + 2 个 padding）
		assertEquals(24, md5.length());
		assertEquals(md5, EsignSigner.contentMd5(body));
		assertEquals("", EsignSigner.contentMd5(null));
	}

	@Test
	@DisplayName("sortApiUrl：query 按字段名字典序重排，重复参数直接报错")
	void sortApiUrl() {
		assertEquals("https://x/y?a=1&b=2&c=3",
			EsignSigner.sortApiUrl("https://x/y?c=3&a=1&b=2"));
		assertEquals("https://x/y", EsignSigner.sortApiUrl("https://x/y?"));
		assertEquals("https://x/y", EsignSigner.sortApiUrl("https://x/y"));
		// 重复参数直接报错（避免静默丢参数导致签名串不一致）
		assertThrows(IllegalArgumentException.class, () -> EsignSigner.sortApiUrl("https://x/y?a=1&a=2"));
	}

	private static byte[] hexHmacBytes(String message, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
