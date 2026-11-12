package org.springblade.doc.config;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WPS WebOffice 公共配置装配（blade-doc）。
 *
 * <p>生效值统一由 {@link WpsSettingStore} 从 {@code doc_engine_setting} 库表读取；
 * 本类只装配 {@link WpsProperties} 作为「库表全空」时的启动兜底，不再承载明文凭证。</p>
 */
@Configuration(proxyBeanMethods = false)
public class DocWpsConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "blade.wps")
	public WpsProperties wpsProperties() {
		return new WpsProperties();
	}

	/** 回调验签器：与前端 wpsToken 签发同源，密钥/TTL 现场读库表 */
	@Bean
	public WpsUrlSigner wpsUrlSigner(WpsSettingStore settings) {
		return new WpsUrlSigner(
			() -> settings.current().getUrlSignSecret(),
			() -> settings.current().getUrlSignTtlSeconds());
	}
}
