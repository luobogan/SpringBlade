package org.springblade.workflow.odoc.config;

import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * odoc 模块配置装配：启用 {@link OdocProperties} 与 {@link EsignProperties} 绑定，
 * 使其成为可用 Bean（{@code RestTemplateOdocFileStore}、{@code EsignServiceImpl} 等通过构造注入）。
 */
@Configuration
@EnableConfigurationProperties({OdocProperties.class, EsignProperties.class})
public class OdocConfiguration {
}
