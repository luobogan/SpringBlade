package org.springblade.workflow.config;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flowable 引擎显式装配（自行集成，适配 Spring Boot 4.x）。
 * <p>
 * 不使用 flowable-spring-boot-starter，改为手动构建 ProcessEngine，
 * 绑定项目主数据源（dynamic-datasource 的 primary）与事务管理器，
 * 并自动部署 classpath:processes/ 下的 BPMN 定义。
 * </p>
 */
@Configuration
public class FlowableConfig {

    private static final Logger log = LoggerFactory.getLogger(FlowableConfig.class);

    /**
     * ACT_* 表结构处理策略。默认 none：不建表、不校验、不升级，启动时完全不碰 schema。
     * 库里 39 张 ACT_* 表已存在时务必保持 none，否则 create 会在每次重启执行建表脚本，
     * 撞上已存在的表导致 ProcessEngine 装配失败（表现为 processEngineFactory 抛异常）。
     * 换新库首次初始化时才在配置里临时改成 create，起来后再改回 none。
     */
    @Value("${blade.flowable.database-schema-update:none}")
    private String databaseSchemaUpdate;

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final ResourcePatternResolver resourcePatternResolver;

    public FlowableConfig(DataSource dataSource,
                          PlatformTransactionManager transactionManager,
                          ResourcePatternResolver resourcePatternResolver) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration() throws Exception {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        // 关键修复：显式指定 databaseCatalog。Flowable 的 isTablePresent() 在 databaseCatalog 为 null 时，
        // 会调用 getTables(null, null, 'ACT_GE_PROPERTY', ...) —— MySQL 会把 null catalog 当成“所有库”，
        // 误命中其它 schema（如 jeelowcode）里的 ACT_GE_PROPERTY，导致误判版本不一致而拒绝建表。
        // 显式绑定到本库后，getTables 只扫描 blade_workflow，空库时才能正确走建表分支。
        try (Connection catalogConn = dataSource.getConnection()) {
            String url = catalogConn.getMetaData().getURL();
            String db = parseDbFromUrl(url);
            // 兜底：若 JDBC URL 未在路径中携带库名（如 jdbc:mysql://host:3306/?databaseName=xxx 形式），
            // 退而取当前连接的默认 catalog，避免 databaseCatalog 未被设置而触发跨 schema 误判。
            if (db == null || db.isEmpty()) {
                db = catalogConn.getCatalog();
            }
            if (db != null && !db.isEmpty()) {
                configuration.setDatabaseCatalog(db);
                log.info("[FlowableConfig] 已显式设置 databaseCatalog={}", db);
            } else {
                log.warn("[FlowableConfig] 无法解析库名，未设置 databaseCatalog，"
                    + "Flowable 可能因 null catalog 跨 schema 误判 ACT_* 表而建表失败");
            }
        } catch (Exception ignore) {
            log.warn("[FlowableConfig] 解析 datasource 库名失败，未设置 databaseCatalog", ignore);
        }
        // ACT_* 表已存在，默认 none（不建表/不校验/不升级），启动时完全不碰 schema。
        // 如需在新库首次初始化：配置 blade.flowable.database-schema-update=create 启动一次，再改回 none。
        // 走 create 时仍依赖上面的 setDatabaseCatalog：否则 isTablePresent 会因 null catalog
        // 跨 schema 误判（扫到别的库里的 ACT_* 表），导致误判成"表不存在/版本不一致"。
        log.info("[FlowableConfig] databaseSchemaUpdate={}", databaseSchemaUpdate);
        configuration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        // 开发环境关闭异步作业执行器
        configuration.setAsyncExecutorActivate(false);
        // 自动部署流程定义
        configuration.setDeploymentResources(
            resourcePatternResolver.getResources("classpath*:processes/*.bpmn20.xml"));
        return configuration;
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactory() throws Exception {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

    @Bean
    public ProcessEngine processEngine() throws Exception {
        return processEngineFactory().getObject();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    private static String parseDbFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx + 1 >= path.length()) {
            return null;
        }
        String db = path.substring(idx + 1);
        return db.isEmpty() ? null : db;
    }

}
