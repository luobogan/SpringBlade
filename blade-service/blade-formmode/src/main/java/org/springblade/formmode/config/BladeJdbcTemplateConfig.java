package org.springblade.formmode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 主库（blade）JdbcTemplate 配置
 *
 * 【重要 · 不要删除】
 * EcologyDataSourceConfig 注册了名为 ecologyJdbcTemplate 的 JdbcTemplate Bean，
 * 而 Spring Boot 的 JdbcTemplateAutoConfiguration 是 @ConditionalOnMissingBean(JdbcOperations.class)，
 * 会整体退让、不再自动创建主库 jdbcTemplate。
 * 若不在此显式声明并标记 @Primary，则所有按类型注入 JdbcTemplate 的地方
 * （WorkflowBillServiceImpl / FormModeServiceImpl / FormDataServiceImpl 等）
 * 都会拿到 ecology 的 JdbcTemplate，导致建表、删表、表单数据 CRUD 全部误操作 ecology 库。
 *
 * 此处的 DataSource 即主库 DataSource（由 DataSourceAutoConfiguration 自动创建）。
 */
@Configuration
public class BladeJdbcTemplateConfig {

	@Bean
	@Primary
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

}
