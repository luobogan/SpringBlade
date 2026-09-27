package org.springblade.formmode.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 数据源诊断器（临时排查用，排查完可删除）
 *
 * 启动时打印 MyBatis 实际使用的 DataSource 的真实信息：
 * JDBC URL、MySQL 实例、当前库、workflow_bill 分布在哪些库、
 * 各库是否含 is_deleted 列，以及 DESCRIBE workflow_bill。
 *
 * 注意：项目存在主库 JdbcTemplate 与 ecologyJdbcTemplate 两个同类型 Bean，
 * 因此这里从 SqlSessionFactory 取 DataSource，确保诊断的就是 WorkflowBillMapper 查询所用的那个库。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DbDiagnoseRunner implements CommandLineRunner {

    private final SqlSessionFactory sqlSessionFactory;

    @Override
    public void run(String... args) {
        log.info("================ [DB诊断] 开始 ================");
        try {
            DataSource ds = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
            JdbcTemplate jt = new JdbcTemplate(ds);

            // 1. 真实 JDBC URL / MySQL 实例 / 当前库
            String url = jt.execute((ConnectionCallback<String>) conn -> conn.getMetaData().getURL());
            log.info("[DB诊断] 实际 JDBC URL : {}", url);

            String db = jt.queryForObject("SELECT DATABASE()", String.class);
            log.info("[DB诊断] 当前库 DATABASE(): {}", db);

            Map<String, Object> host = jt.queryForMap("SELECT @@hostname AS hostname, @@port AS port");
            log.info("[DB诊断] MySQL 实例: {}:{}", host.get("hostname"), host.get("port"));

            // 2. workflow_bill 分布在哪些库
            List<Map<String, Object>> tables = jt.queryForList(
                    "SELECT table_schema FROM information_schema.tables WHERE table_name = 'workflow_bill'");
            log.info("[DB诊断] 含 workflow_bill 表的库: {}", tables);

            // 3. 各库 workflow_bill 是否含 is_deleted
            List<Map<String, Object>> hasCol = jt.queryForList(
                    "SELECT table_schema, COUNT(*) AS cnt FROM information_schema.columns "
                            + "WHERE table_name = 'workflow_bill' AND column_name = 'is_deleted' GROUP BY table_schema");
            log.info("[DB诊断] 各库 workflow_bill 含 is_deleted 情况: {}", hasCol);

            // 4. 当前库的 workflow_bill 是否含 is_deleted（这是报错直接原因）
            Integer cnt = jt.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                            + "AND table_name = 'workflow_bill' AND column_name = 'is_deleted'", Integer.class);
            log.info("[DB诊断] 当前库({}) workflow_bill 含 is_deleted: {}", db,
                    (cnt != null && cnt > 0) ? "是" : "否 —— 这就是 Unknown column 'is_deleted' 的原因");

            // 5. DESCRIBE workflow_bill（当前库）
            List<Map<String, Object>> desc = jt.queryForList("DESCRIBE workflow_bill");
            log.info("[DB诊断] DESCRIBE {} .workflow_bill:", db);
            for (Map<String, Object> row : desc) {
                log.info("[DB诊断]   {} | {} | null={} | key={}",
                        row.get("Field"), row.get("Type"), row.get("Null"), row.get("Key"));
            }
        } catch (Exception e) {
            log.warn("[DB诊断] 执行失败: {}", e.getMessage());
        }
        log.info("================ [DB诊断] 结束 ================");
    }

}
