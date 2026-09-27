package org.springblade.formmode.ecology;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 泛微 ecology 数据源配置（临时功能：从 ecology 导入表单）
 * <p>连接参数直接写死在此处（临时内部使用，后续会整体关闭）。</p>
 * <p>关闭方式：在 application.yml / Nacos 配置 {@code ecology.enabled=false} 即可停用本数据源，
 * 不影响 blade 主库与其它功能。</p>
 * <p>注意：Hikari 只有 {@code jdbcUrl} 而非 {@code url}，故手动 setJdbcUrl，
 * 避免报 "jdbcUrl is required with driverClassName"。</p>
 */
@Configuration
//@ConditionalOnProperty(value = "ecology.enabled", havingValue = "true", matchIfMissing = true)
public class EcologyDataSourceConfig {

	// ============ 泛微 e-cology 数据源（SQL Server，见 weaver.properties）============
	// 来源：d:\Weaver2020\ecology\WEB-INF\prop\weaver.properties
	//   DriverClasses = com.microsoft.sqlserver.jdbc.SQLServerDriver
	//   ecology.url  = jdbc:sqlserver://127.0.0.1:1433;DatabaseName=ecology2020_demo
	//   ecology.user = sa  /  ecology.password = 1
	private static final String ECOLOGY_JDBC_URL =
		"jdbc:sqlserver://127.0.0.1:1433;databaseName=ecology2020_demo;encrypt=false;trustServerCertificate=true";
	private static final String ECOLOGY_USERNAME = "sa";
	private static final String ECOLOGY_PASSWORD = "1";
	private static final String ECOLOGY_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	// =============================================================

	/**
	 * 创建 ecology 数据源。
	 *
	 * 【重要】此处绝对不要加 @Bean 注解！
	 * Spring Boot 的 DataSourceAutoConfiguration 是 @ConditionalOnMissingBean(DataSource.class)，
	 * 一旦本类把 ecology 数据源注册成 DataSource Bean，blade 主库的 DataSource 就不会被自动创建，
	 * 导致 MyBatis 的 SqlSessionFactory 绑到 ecology 库，
	 * 进而出现 "Unknown column 'is_deleted' in 'where clause'"（查的是 ecology.workflow_bill，该表无 is_deleted）。
	 * 因此这里只作为普通方法使用，仅用于构造下面的 ecologyJdbcTemplate。
	 */
	private DataSource createEcologyDataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl(ECOLOGY_JDBC_URL);
		ds.setUsername(ECOLOGY_USERNAME);
		ds.setPassword(ECOLOGY_PASSWORD);
		ds.setDriverClassName(ECOLOGY_DRIVER);
		ds.setPoolName("ecology");
		return ds;
	}

	@Bean(name = "ecologyJdbcTemplate")
	public JdbcTemplate ecologyJdbcTemplate() {
		return new JdbcTemplate(createEcologyDataSource());
	}
}
