package org.springblade.workflow.config;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import org.springblade.workflow.listener.WfEngineEventListener;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

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
     * ACT_* 表结构处理策略。默认 true：启动时若 ACT_GE_PROPERTY 记录的 schema 版本低于
     * 引擎期望版本，则自动执行升级脚本（如 8100→8101 补齐 DUE_DATE_/CLAIM_TIME_/CLAIMED_BY_ 等列），
     * 仅当表缺失时才建表——不会像 create 那样在表已存在时重试建表导致装配失败。
     * 这样可自愈 Flowable 版本升级带来的 schema 漂移（见 2026-09-26 ACT_HI_PROCINST 缺列事故）。
     * 若刻意要自行管理 schema（如外部 DBA 统一维护），可在配置里显式设为 none。
     */
    @Value("${blade.flowable.database-schema-update:true}")
    private String databaseSchemaUpdate;

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectProvider<WfEngineEventListener> wfEngineEventListenerProvider;

    public FlowableConfig(DataSource dataSource,
                          PlatformTransactionManager transactionManager,
                          ResourcePatternResolver resourcePatternResolver,
                          ObjectProvider<WfEngineEventListener> wfEngineEventListenerProvider) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.resourcePatternResolver = resourcePatternResolver;
        this.wfEngineEventListenerProvider = wfEngineEventListenerProvider;
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
        // ACT_* 表已存在，默认 true（启动时按版本自动升级，缺失时才建表），自愈 schema 漂移且不会撞已存在的表。
        // 如需在新库首次初始化：配置 blade.flowable.database-schema-update=create 启动一次，再改回 true/none。
        // 走 create 时仍依赖上面的 setDatabaseCatalog：否则 isTablePresent 会因 null catalog
        // 跨 schema 误判（扫到别的库里的 ACT_* 表），导致误判成"表不存在/版本不一致"。
        log.info("[FlowableConfig] databaseSchemaUpdate={}", databaseSchemaUpdate);
        configuration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        // 诊断：打印库内 ACT_GE_PROPERTY 真实记录的 schema 版本，一眼看出引擎期望版本与库实际版本的落差。
        // 若该值停在 8.1.0.0（8100）而引擎期望 8.1.0.1，即本次 DUE_DATE_ 缺列事故的根因；
        // 若为 null/表不存在则代表全新库，将由引擎建表分支处理。
        logDbSchemaVersion();
        // 开发环境关闭异步作业执行器（方案 C §3.4 前提：异步事件改由 job 独立事务派发，须禁用）
        configuration.setAsyncExecutorActivate(false);
        // 显式固化历史级别为 audit：保证 ACT_HI_COMMENT 可落库（审批轨迹迁移 ACT_HI_COMMENT 的硬门槛）。
        // Flowable 默认即为 audit，此处显式声明以消除歧义；表单快照所需的 full 级别不开启（见下沉迁移方案 §0）。
        // 注：手动装配未使用 flowable-spring-boot-starter，yml 的 flowable.history-level 不会生效，必须此处声明。
        configuration.setHistory("audit");
        // 禁止异步历史：HISTORIC_* 事件跨事务写入，开启后存在漏派风险（方案 C §3.4 要求保持关闭）
        configuration.setAsyncHistoryEnabled(false);
        // 方案C 事件驱动台账投影：仅当开关 blade.workflow.ledger-listener.enabled=true 时注册全局监听（默认关）。
        // 监听仅依赖 wf_* Mapper（无引擎 Service 依赖），故走 setEventListeners 直接装配（方案C §2.2 方式一），
        // 避免与 processEngineConfiguration 形成构造期循环依赖。关闭开关时 bean 不存在，不注册、完全回到方案A 双写。
        WfEngineEventListener ledgerListener = wfEngineEventListenerProvider.getIfAvailable();
        if (ledgerListener != null) {
            configuration.setEventListeners(List.of(ledgerListener));
            log.info("[FlowableConfig] 已注册方案C 台账事件监听 WfEngineEventListener（ledger-listener.enabled=true）");
        }
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

    /**
     * 诊断用：读取库内 ACT_GE_PROPERTY 的 schema 版本（及 history），打印到启动日志。
     * 单独走 DataSource JDBC，避免依赖尚未构建的 ProcessEngine；表不存在/查询失败均仅告警、不影响装配。
     */
    private void logDbSchemaVersion() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT NAME_, VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ IN ('schema.version', 'schema.history')")) {
            StringBuilder sb = new StringBuilder("ACT_GE_PROPERTY 现状 → ");
            boolean any = false;
            while (rs.next()) {
                any = true;
                sb.append(rs.getString("NAME_")).append('=').append(rs.getString("VALUE_")).append("; ");
            }
            if (any) {
                log.info("[FlowableConfig] {}", sb);
            } else {
                log.warn("[FlowableConfig] ACT_GE_PROPERTY 中无 schema.version/schema.history 记录，"
                    + "库可能尚未初始化（全新库，将由引擎按 {} 策略处理）", databaseSchemaUpdate);
            }
        } catch (Exception e) {
            log.warn("[FlowableConfig] 读取 ACT_GE_PROPERTY 失败（表可能不存在，属全新库正常情况）：{}", e.getMessage());
        }
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
