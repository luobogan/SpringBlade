package org.springblade.workflow;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 直接在真实 MySQL(blade_workflow) 上执行 Flowable schema 创建（DB_SCHEMA_UPDATE_CREATE）。
 * 与线上引擎同版本(7.1.0)，确保 ACT_* 表结构与运行引擎一致。仅建表、不删。
 * 运行：mvn -pl blade-service/blade-workflow test -Dtest=CreateActTablesTest
 *
 * 默认 @Disabled：该测试连真实 MySQL，不应在常规构建中执行。需要重建/核对 ACT_* 表时再临时启用。
 */
@Disabled("连真实 MySQL 建表，常规构建不执行；需重建/核对 ACT_* 表时手动启用")
public class CreateActTablesTest {

    @Test
    public void createSchema() {
        String url = "jdbc:mysql://127.0.0.1:3306/blade_workflow"
                + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        cfg.setJdbcUrl(url);
        cfg.setJdbcDriver("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUsername("root");
        cfg.setJdbcPassword("123456");
        // 关键：显式 catalog，避免 isTablePresent 在 null catalog 下跨 schema 误判
        cfg.setDatabaseCatalog("blade_workflow");
        cfg.setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_CREATE);
        cfg.setAsyncExecutorActivate(false);

        ProcessEngine engine = cfg.buildProcessEngine();
        try {
            ManagementService ms = engine.getManagementService();
            Map<String, Long> tableCount = ms.getTableCount();
            long actTables = tableCount.keySet().stream()
                    .filter(n -> n.toUpperCase().startsWith("ACT_"))
                    .count();
            System.out.println("[CreateActTables] 总表映射数 = " + tableCount.size());
            System.out.println("[CreateActTables] ACT_* 表数量 = " + actTables);
            Long geProp = tableCount.get("ACT_GE_PROPERTY");
            System.out.println("[CreateActTables] ACT_GE_PROPERTY 行数 = " + geProp);
            if (actTables < 30) {
                throw new AssertionError("ACT_* 表创建不足，疑似失败: " + actTables);
            }
            System.out.println("==== 结论: ACT_* 表创建完成（" + actTables + " 张）====");
        } finally {
            engine.close();
            ProcessEngines.destroy();
        }
    }
}
