package org.springblade.doc.sign;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.doc.config.DocProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 内容引擎 URL 签名（HMAC-SHA256 + 时效），加固 WPS/OnlyOffice 直连 + 浏览器直连的 content 端点。
 *
 * <p><b>WPS 统一改造 P1</b>：算法实现已抽取到 {@code blade-common} 的 {@link WpsUrlSigner}，
 * 与 blade-workflow 的 {@code OdocUrlSigner} 共用同一份实现；签名串格式
 * {@code exp.hex(hmac(resource:exp))} 保持不变，存量已签发 URL 完全兼容。</p>
 *
 * <p><b>WPS 统一改造 P2（密钥统一）</b>：优先使用共享 {@code blade.wps.url-sign-secret}，
 * 未配置时回退到原有 {@code blade.doc.url-sign-secret}。两侧（doc/odoc）统一为同一密钥后，
 * odoc 在 blade-doc 单回调入口验签才能通过（R2 / P3.1 前移）。</p>
 *
 * <p>端点校验：签名有效 或 已登录会话 任一通过，闭合 DOC-P0-4 可枚举下载。</p>
 */
@Component
public class DocUrlSigner {

	private final WpsUrlSigner delegate;

	public DocUrlSigner(WpsProperties wps, DocProperties doc) {
		String secret = StringUtils.hasText(wps.getUrlSignSecret())
			? wps.getUrlSignSecret() : doc.getUrlSignSecret();
		int ttl = wps.getUrlSignTtlSeconds() > 0
			? wps.getUrlSignTtlSeconds() : doc.getUrlSignTtlSeconds();
		this.delegate = new WpsUrlSigner(secret, ttl);
	}

	public String sign(String resource) {
		return delegate.sign(resource);
	}

	public boolean verify(String resource, String token) {
		return delegate.verify(resource, token);
	}
}
