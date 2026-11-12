package org.springblade.doc.config;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * WPS WebOffice 公共配置装配（blade-doc）。
 *
 * <p>WPS 统一改造 P1-1.4：把 {@code blade-common} 的 {@link WpsProperties} 装配为 Bean，
 * 绑定 {@code blade.wps.*}。知识管理侧既有 {@code blade.doc.wps.*} 与库表
 * {@code doc_engine_setting} 仍按 {@link DocProperties} / {@code IDocEngineSettingService}
 * 读取（优先级更高），因此本 Bean 在 P1 阶段不改变任何现有行为，仅为公共回调层
 * （{@code DocWpsProvider}）提供统一配置入口。</p>
 *
 * <p>P3 收口：把明文 {@code blade.doc.wps.app-id/app-secret} 迁到 Nacos
 * {@code blade.wps.*} 并统一两侧 {@code url-sign-secret} 后，再由本 Bean 作为唯一来源。</p>
 */
@Configuration(proxyBeanMethods = false)
public class DocWpsConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "blade.wps")
	public WpsProperties wpsProperties() {
		return new WpsProperties();
	}

	/**
	 * 回调验签器：必须与 {@code DocUrlSigner}（签发 wpsToken 给前端）同源密钥，
	 * 否则所有 WPS 回调都会被拒。
	 *
	 * <p><b>WPS 统一改造 P2</b>：优先使用共享 {@code blade.wps.url-sign-secret}，未配置时
	 * 回退 {@code blade.doc.url-sign-secret}（P3.1 正式统一后只保留共享密钥）。这样 doc 与
	 * odoc 两侧共用一把密钥，odoc 在单回调入口的验签才能通过（R2 前移）。</p>
	 */
	@Bean
	public WpsUrlSigner wpsUrlSigner(WpsProperties wps, DocProperties docProperties) {
		String secret = StringUtils.hasText(wps.getUrlSignSecret())
			? wps.getUrlSignSecret() : docProperties.getUrlSignSecret();
		int ttl = wps.getUrlSignTtlSeconds() > 0
			? wps.getUrlSignTtlSeconds() : docProperties.getUrlSignTtlSeconds();
		return new WpsUrlSigner(secret, ttl);
	}
}
