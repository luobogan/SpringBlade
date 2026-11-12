package org.springblade.workflow.odoc.config;

import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsUrlSigner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WPS WebOffice 公共配置装配（blade-workflow）。
 *
 * <p>生效值与 blade-doc 同源，统一由 {@link WpsSettingStore} 读 {@code doc_engine_setting}；
 * {@link WpsProperties} 仅作库表全空时的启动兜底，本服务 yml 不再维护 WPS 凭证。</p>
 */
@Configuration(proxyBeanMethods = false)
public class OdocWpsConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "blade.wps")
	public WpsProperties wpsProperties() {
		return new WpsProperties();
	}

	/** odoc 侧签名器：与 blade-doc 单回调入口共用同一把动态密钥 */
	@Bean
	public WpsUrlSigner wpsUrlSigner(WpsSettingStore settings) {
		return new WpsUrlSigner(
			() -> settings.current().getUrlSignSecret(),
			() -> settings.current().getUrlSignTtlSeconds());
	}
}
