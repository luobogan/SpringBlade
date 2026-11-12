package org.springblade.workflow.odoc;

import org.springblade.common.wps.WpsUrlSigner;
import org.springblade.workflow.odoc.config.WpsSettingStore;
import org.springframework.stereotype.Component;

/**
 * 公文内容/回调 URL 签名（加固 WPS/OnlyOffice 直连 + 浏览器直连的 permitAll 端点）。
 *
 * <p>算法统一委托公共 {@link WpsUrlSigner}；密钥/TTL 每次现场从 {@link WpsSettingStore}
 * 读取（与 blade-doc 同一张 {@code doc_engine_setting} 表），保证跨服务签发/验签同源，
 * 且前端保存设置后立即生效，无需重启。</p>
 *
 * <p>端点校验：签名有效 或 已登录会话 任一通过；签名防未授权枚举下载（E9 DOC-P0-4 同款）。</p>
 */
@Component
public class OdocUrlSigner {

	private final WpsUrlSigner delegate;

	public OdocUrlSigner(WpsSettingStore settings) {
		this.delegate = new WpsUrlSigner(
			() -> settings.current().getUrlSignSecret(),
			() -> settings.current().getUrlSignTtlSeconds());
	}

	public String sign(String resource) {
		return delegate.sign(resource);
	}

	public boolean verify(String resource, String token) {
		return delegate.verify(resource, token);
	}
}
