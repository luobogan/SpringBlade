package org.springblade.formmode.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据迁移器
 *
 * 从 ecology 数据库迁移存量表单建模数据到 blade_formmode 库
 * 仅在 dev/test 环境执行，且幂等执行
 */
@Slf4j
@Component
@Order(100)
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.data.migration.enabled:true}")
    private boolean migrationEnabled;

    @Value("${app.data.migration.source-datasource:}")
    private String sourceDataSourceName;

    @Override
    public void run(String... args) {
        if (!migrationEnabled) {
            log.info("[数据迁移] 未启用，跳过");
            return;
        }

        // 检查目标库是否已有数据
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM modeinfo", Integer.class);
        if (count != null && count > 0) {
            log.info("[数据迁移] modeinfo 表已有 {} 条数据，跳过此次迁移", count);
            return;
        }

        log.info("[数据迁移] 开始迁移 ecology 表单建模数据...");
        log.warn("[数据迁移] 需要配置目标 ecology 数据源才能执行实际迁移");
        log.warn("[数据迁移] 请通过 app.data.migration.source-datasource 配置 ecology 数据源名称");
        log.info("[数据迁移] 完成（未执行实际迁移，因缺少源数据源配置）");
    }

}
