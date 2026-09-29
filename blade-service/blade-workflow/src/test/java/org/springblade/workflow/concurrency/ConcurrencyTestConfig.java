package org.springblade.workflow.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 并发测试专用配置：在 {@link org.springblade.workflow.FlowableTestConfig}（内存 H2 + 真实 Flowable 引擎）
 * 之上，叠加本改造的「双写收口器」所需环境：
 *
 * <ul>
 *   <li>开启 {@code blade.workflow.instance-act-write.enabled=true}，使 {@code WfInstanceActWriter} 真正写回 ACT_*；</li>
 *   <li>提供 {@link JdbcTemplate}（与引擎共用同一 H2 DataSource，模拟生产「app 与引擎同库同事务」）；</li>
 *   <li>{@link EnableTransactionManagement} 使探针的 {@code @Transactional} 生效，验证「引擎 INSERT + 业务列 UPDATE
 *       落在同一事务」的原子性；</li>
 *   <li>{@link H2ActColumnExtender} 给 H2 的 {@code ACT_HI_PROCINST} 补上本改造新增业务列（生产由
 *       {@code act_add_columns.sql} 负责，H2 自动建表不含这些列）。</li>
 * </ul>
 */
@Configuration
@EnableTransactionManagement
public class ConcurrencyTestConfig {

	/** 强制开启双写开关，让 writer 在测试里真正产生 ACT_* 写入 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("blade.workflow.instance-act-write.enabled", "true");
		configurer.setProperties(props);
		return configurer;
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	/** 依赖 processEngine 保证 ACT_* 表已建好，再补业务列（不注入 ProcessEngine，避免与 factoryBean 歧义） */
	@Bean
	@DependsOn("processEngine")
	public H2ActColumnExtender h2ActColumnExtender(JdbcTemplate jdbcTemplate) {
		return new H2ActColumnExtender(jdbcTemplate);
	}
}
