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
import org.springblade.workflow.listener.WfBizCallbackListener;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ObjectProvider<WfBizCallbackListener> wfBizCallbackListenerProvider;

    /** 诊断用：装配阶段解析出的实际库名 / 库内 schema 版本 / history，统一打印自检结论 */
    private String resolvedCatalog;
    private String dbSchemaVersion;
    private String dbSchemaHistory;

    public FlowableConfig(DataSource dataSource,
                          PlatformTransactionManager transactionManager,
                          ResourcePatternResolver resourcePatternResolver,
                          ObjectProvider<WfEngineEventListener> wfEngineEventListenerProvider,
                          ObjectProvider<WfBizCallbackListener> wfBizCallbackListenerProvider) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.resourcePatternResolver = resourcePatternResolver;
        this.wfEngineEventListenerProvider = wfEngineEventListenerProvider;
        this.wfBizCallbackListenerProvider = wfBizCallbackListenerProvider;
    }

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration() throws Exception {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        // 关键修复：显式指定 databaseCatalog。Flowable 的 isTablePresent() 在 databaseCatalog 为 null 时，
        // 会调用 getTables(null, null, 'ACT_GE_PROPERTY', ...) —— MySQL 会把 null catalog 当成“所有库”，
        // 误命中其它 schema（如 jeelowcode）里的 ACT_GE_PROPERTY，导致误判版本不一致而拒绝建表。
        // 显式绑定到本库（实际库名由 JDBC URL 动态解析，现为 blade）后，getTables 只扫描该 catalog，空库时才能正确走建表分支。
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
                this.resolvedCatalog = db;
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
        configuration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        // 诊断自检：一次性聚合 databaseCatalog + databaseSchemaUpdate + 引擎期望版本 vs 库实际版本，打印一行对比结论。
        logFlowableSummary();
        // 关闭异步作业执行器（方案 C §3.4 前提：本模块未使用定时器边界事件 / 异步节点，故保持关闭；一旦要支持定时器须开启，并配套 §3.4 的「C1 降级为最终一致 + 常驻对账修复 + deadletter 告警」三项兜底）
        configuration.setAsyncExecutorActivate(false);
        // 显式固化历史级别为 audit：保证 ACT_HI_COMMENT 可落库（审批轨迹迁移 ACT_HI_COMMENT 的硬门槛）。
        // Flowable 默认即为 audit，此处显式声明以消除歧义；表单快照所需的 full 级别不开启（见下沉迁移方案 §0）。
        // 注：手动装配未使用 flowable-spring-boot-starter，yml 的 flowable.history-level 不会生效，必须此处声明。
        configuration.setHistory("audit");
        // 禁止异步历史：HISTORIC_* 事件跨事务写入，开启后存在漏派风险（方案 C §3.4 要求保持关闭）
        configuration.setAsyncHistoryEnabled(false);
        // ⚠️ 硬约束（方案C 启用真实反写时必读）：上面两行（关闭异步历史 + 关闭异步作业执行器）是方案C 杜绝
        // 定时/异步漏派漂移的【前提】。若将来把 blade.workflow.ledger-listener.enabled=true 并在
        // WfStateProjector 内落地真实反写，必须同步把 WfEngineEventListener.isFailOnException() 改回 true
        // （C1 强一致：写 wf_* 失败即回滚引擎事务，台账与引擎同生共死）。当前监听处于影子模式、isFailOnException=false
        // 仅打日志不回滚，是【阶段1 验证期】的安全值；一旦开启真实反写却忘了改回 true，事件同事务派发虽已无跨事务漏派，
        // 但台账写库失败不会回滚引擎，仍会留下"引擎成/台账丢"的漂移——此开关只是消除异步那一维，强一致那维须靠 isFailOnException 兜底。
        // 方案C 事件驱动台账投影：仅当开关 blade.workflow.ledger-listener.enabled=true 时注册全局监听（默认关）。
        // 监听仅依赖 wf_* Mapper（无引擎 Service 依赖），故走 setEventListeners 直接装配（方案C §2.2 方式一），
        // 避免与 processEngineConfiguration 形成构造期循环依赖。关闭开关时 bean 不存在，不注册、完全回到方案A 双写。
        WfEngineEventListener ledgerListener = wfEngineEventListenerProvider.getIfAvailable();
        if (ledgerListener != null) {
            configuration.setEventListeners(List.of(ledgerListener));
            log.info("[FlowableConfig] 已注册方案C 台账事件监听 WfEngineEventListener（ledger-listener.enabled=true）");
        }
        // P3-2 业务回调监听：人员状态流转流程审批完成后回调 blade-system 落库 person_status。
        // 与台账监听同构（只依赖 wf_* Mapper，无引擎 Service），故同样走 setEventListeners 直接装配。
        // 独立开关 blade.workflow.biz-callback.enabled（默认 true），关闭即完全回到「只有台账、无业务回调」。
        WfBizCallbackListener bizListener = wfBizCallbackListenerProvider.getIfAvailable();
        if (bizListener != null) {
            configuration.setEventListeners(List.of(bizListener));
            log.info("[FlowableConfig] 已注册 P3 业务回调监听 WfBizCallbackListener（biz-callback.enabled=true）");
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
        ProcessEngine engine = processEngineFactory().getObject();
        // 确认引擎真的构建成功（而非只配了 bean 没起来）。构建失败（如 schema 升级/建表异常）会在此前抛错，
        // 这行就不会出现——它本身即“引擎没起来”的否定信号。
        String engineVersion = resolveEngineSchemaVersion();
        log.info("[FlowableConfig] [引擎就绪] ProcessEngine 已成功构建 ✓ name={} | 引擎版本={}",
            engine.getName(), (engineVersion != null ? engineVersion : "未知"));
        return engine;
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
     * 诊断自检：聚合 databaseCatalog / databaseSchemaUpdate / 引擎期望版本 / 库实际版本，
     * 打印一行“一目了然”的对比结论。单独走 DataSource JDBC，不依赖尚未构建的 ProcessEngine；
     * 表不存在/查询失败视为全新库，不影响装配。
     */
    private void logFlowableSummary() {
        String engineExpected = resolveEngineSchemaVersion();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT NAME_, VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_ IN ('schema.version', 'schema.history')")) {
            while (rs.next()) {
                String name = rs.getString("NAME_");
                String value = rs.getString("VALUE_");
                if ("schema.version".equals(name)) {
                    this.dbSchemaVersion = value;
                } else if ("schema.history".equals(name)) {
                    this.dbSchemaHistory = value;
                }
            }
        } catch (Exception e) {
            log.debug("[FlowableConfig] 读取 ACT_GE_PROPERTY 失败（全新库常见，将按建表处理）：{}", e.getMessage());
        }

        String catalog = (this.resolvedCatalog != null) ? this.resolvedCatalog : "未解析/Null(可能跨schema误判)";
        String actual = (this.dbSchemaVersion != null) ? this.dbSchemaVersion : "N/A(全新库)";

        String conclusion;
        if (engineExpected == null) {
            conclusion = "引擎版本未知，无法比对（请检查 flowable-engine 依赖）";
        } else if (this.dbSchemaVersion == null) {
            conclusion = "库未初始化，启动将按 schemaUpdate 策略建表/升级";
        } else if (engineExpected.equals(this.dbSchemaVersion)) {
            conclusion = "版本一致 ✓";
        } else if ("true".equalsIgnoreCase(databaseSchemaUpdate) || "create".equalsIgnoreCase(databaseSchemaUpdate)) {
            conclusion = "库版本偏低/不一致，启动将自动升级(" + this.dbSchemaVersion + "→" + engineExpected + ")";
        } else {
            conclusion = "⚠ 库版本(" + this.dbSchemaVersion + ")≠引擎期望(" + engineExpected
                + ")，且 schemaUpdate=none 不自动升级，可能缺列/报错(如 DUE_DATE_)";
        }

        log.info("[FlowableConfig] [映射自检] catalog={} | databaseSchemaUpdate={} | 引擎期望版本={} | 库实际版本={} | 结论: {}",
            catalog, databaseSchemaUpdate, engineExpected, actual, conclusion);
        if (this.dbSchemaHistory != null) {
            log.info("[FlowableConfig] [映射自检] schema.history={}", this.dbSchemaHistory);
        }
    }

    /**
     * 从 flowable-engine jar 的升级脚本资源反推引擎“期望 schema 版本”：取所有
     * flowable.all.upgradestep.*.to.{to}.engine.sql 中最大的 to 段（如 8101），
     * 规整为 8.1.0.1 形式——这正是 Flowable 自身升级链要抵达的目标版本，且与库内
     * ACT_GE_PROPERTY.schema.version 格式一致，可直接比对。不依赖任何被移除的版本常量类。
     */
    private static String resolveEngineSchemaVersion() {
        try {
            URL loc = SpringProcessEngineConfiguration.class.getProtectionDomain()
                .getCodeSource().getLocation();
            if (loc == null || !loc.getPath().endsWith(".jar")) {
                return null;
            }
            try (JarFile jar = new JarFile(new File(loc.toURI()))) {
                Pattern p = Pattern.compile("org/flowable/db/upgrade/flowable\\.all\\.upgradestep\\.\\d+\\.to\\.(\\d+)\\.engine\\.sql$");
                String maxTo = null;
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    Matcher m = p.matcher(name);
                    if (m.matches()) {
                        String to = m.group(1);
                        if (maxTo == null || to.compareTo(maxTo) > 0) {
                            maxTo = to;
                        }
                    }
                }
                return (maxTo != null) ? formatVersion(maxTo) : null;
            }
        } catch (Exception e) {
            log.debug("[FlowableConfig] 反推引擎期望 schema 版本失败：{}", e.getMessage());
            return null;
        }
    }

    /** 把 4 位升级编号（如 8101 / 6411）规整为带点的 schema 版本（8.1.0.1 / 6.4.1.1） */
    private static String formatVersion(String fourDigits) {
        if (fourDigits == null || fourDigits.length() != 4) {
            return fourDigits;
        }
        return fourDigits.charAt(0) + "." + fourDigits.charAt(1) + "."
            + fourDigits.charAt(2) + "." + fourDigits.charAt(3);
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
