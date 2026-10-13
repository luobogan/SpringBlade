package org.springblade.workflow.odoc.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * odoc 模块配置装配：启用 {@link OdocProperties} 绑定，使其成为可用 Bean
 * （{@code RestTemplateOdocFileStore} 等通过构造注入）。
 */
@Configuration
@EnableConfigurationProperties(OdocProperties.class)
public class OdocConfiguration {
}
