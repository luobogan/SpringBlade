package org.springblade.workflow.odoc.config;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * WPS WebOffice 公共配置装配（blade-workflow）。
 *
 * <p>WPS 统一改造 P1-1.4：与 blade-doc 装配同一份 {@link WpsProperties} 契约。</p>
 *
 * <p>WPS 统一改造 P2：本 Bean 正式参与运行期判定——odoc 在 blade-doc 单回调入口验签，
 * 因此 odoc 签发 wpsToken / content 签名必须用与 blade-doc 回调入口<b>相同</b>的密钥
 * （{@link #wpsUrlSigner}，优先 {@code blade.wps.url-sign-secret}，回退 {@code odoc.url-sign-secret}）。
 * 未设共享密钥时仍回退 odoc 本地密钥，保持存量 OnlyOffice 链路不受影响。</p>
 */
@Configuration(proxyBeanMethods = false)
public class OdocWpsConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "blade.wps")
	public WpsProperties wpsProperties() {
		return new WpsProperties();
	}

	/**
	 * odoc 侧 WPS 签名器：签发 wpsToken（前端 → WPS 中台 → blade-doc 回调入口验签）、
	 * 签发/验证 content 下载 URL。必须优先与 blade-doc 共用 {@code blade.wps.url-sign-secret}。
	 */
	@Bean
	public WpsUrlSigner wpsUrlSigner(WpsProperties wps, OdocProperties odoc) {
		String secret = StringUtils.hasText(wps.getUrlSignSecret())
			? wps.getUrlSignSecret() : odoc.getUrlSignSecret();
		int ttl = wps.getUrlSignTtlSeconds() > 0
			? wps.getUrlSignTtlSeconds() : odoc.getUrlSignTtlSeconds();
		return new WpsUrlSigner(secret, ttl);
	}
}
