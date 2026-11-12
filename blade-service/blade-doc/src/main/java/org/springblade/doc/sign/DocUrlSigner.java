package org.springblade.doc.sign;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.config.WpsSettingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 内容引擎 URL 签名（HMAC-SHA256 + 时效），加固 WPS/OnlyOffice 直连 + 浏览器直连的 content 端点。
 *
 * <p>算法统一委托 {@link WpsUrlSigner}；密钥/TTL 每次现场从 {@link WpsSettingStore}
 * 读取（单一数据源 {@code doc_engine_setting}），前端保存设置后立即生效，无需重启。</p>
 *
 * <p>端点校验：签名有效 或 已登录会话 任一通过，闭合 DOC-P0-4 可枚举下载。</p>
 */
@Component
public class DocUrlSigner {

	private final WpsUrlSigner delegate;

	@Autowired
	public DocUrlSigner(WpsSettingStore settings) {
		this.delegate = new WpsUrlSigner(
			() -> settings.current().getUrlSignSecret(),
			() -> settings.current().getUrlSignTtlSeconds());
	}

	/** 兼容构造器：仅供协议层单测在脱离 Spring 容器时注入固定密钥 */
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
