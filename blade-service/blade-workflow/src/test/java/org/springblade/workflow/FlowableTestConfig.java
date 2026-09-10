package org.springblade.workflow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springblade.workflow.config.FlowableConfig;
import org.springblade.workflow.service.impl.ProcessServiceImpl;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 隔离式测试配置：不依赖 Nacos / MySQL / Redis，
 * 仅用内存 H2 数据源装配 FlowableConfig 与 ProcessServiceImpl，
 * 用于验证 Flowable 7 + Spring 集成在 Boot4/JDK21 下的可用性。
 */
@Configuration
@Import({FlowableConfig.class, ProcessServiceImpl.class})
public class FlowableTestConfig {

	/**
	 * 解析 FlowableConfig 上的 @Value("${blade.flowable.database-schema-update:none}")。
	 *
	 * <p>本测试用的是内存 H2，每次都是空库，且 FlowableConfig 会在引擎构建时自动部署
	 * processes/*.bpmn20.xml（需要 ACT_* 表），所以这里必须覆盖成建表策略 true。
	 * 生产默认 none（表已存在、启动不碰 schema），两者互不影响。</p>
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("blade.flowable.database-schema-update", "true");
		configurer.setProperties(props);
		return configurer;
	}

	@Bean
	public DataSource dataSource() {
		SimpleDriverDataSource ds = new SimpleDriverDataSource();
		ds.setDriverClass(org.h2.Driver.class);
		ds.setUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1");
		ds.setUsername("sa");
		ds.setPassword("");
		return ds;
	}

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public ResourcePatternResolver resourcePatternResolver() {
		return new PathMatchingResourcePatternResolver();
	}

}
