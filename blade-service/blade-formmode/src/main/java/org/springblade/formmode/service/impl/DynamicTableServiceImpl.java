package org.springblade.formmode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.formmode.service.IDynamicTableService;
import org.springblade.formmode.utils.TableNameUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 动态表管理服务实现
 */
@Service
@RequiredArgsConstructor
public class DynamicTableServiceImpl implements IDynamicTableService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean createMainTable(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID'," +
                "`request_id` INT DEFAULT NULL COMMENT '关联流程ID'," +
                "`modedatacreater` INT DEFAULT NULL," +
                "`modedatacreatedate` VARCHAR(10) DEFAULT NULL," +
                "`modedatacreatetime` VARCHAR(8) DEFAULT NULL," +
                "`modedatamodifier` INT DEFAULT NULL," +
                "`modedatamodifydate` VARCHAR(10) DEFAULT NULL," +
                "`modedatamodifytime` VARCHAR(8) DEFAULT NULL," +
                "`mode_uuid` VARCHAR(100) DEFAULT NULL," +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态主数据表 " + tableName + "'";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean createMainTable(Long billId) {
        // 兼容旧接口：根据 billId 查表名
        String tableName = getMainTableName(billId);
        return createMainTable(tableName);
    }

    @Override
    public boolean createDetailTable(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT," +
                "`main_id` BIGINT DEFAULT NULL COMMENT '主表ID'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_main_id` (`main_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态明细表 " + tableName + "'";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean createDetailTable(Integer billId, int detailIndex) {
        // 兼容旧接口
        String tableName = getDetailTableName(billId.longValue(), detailIndex);
        return createDetailTable(tableName);
    }

    @Override
    public boolean addColumnToTable(String tableName, String columnName, String columnType, Integer length) {
        // columnType 已经是完整的 SQL 类型（如 VARCHAR(255)），直接使用
        String type = columnType != null ? columnType : "VARCHAR(255)";
        String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + type + " DEFAULT NULL";
        jdbcTemplate.execute(sql);
        return true;
    }

    @Override
    public boolean dropColumnFromTable(String tableName, String columnName) {
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`");
        return true;
    }

    @Override
    public boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean dropTable(String tableName) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
        return true;
    }

    @Override
    public String getMainTableName(Long billId) {
        return TableNameUtil.getMainTableName(billId);
    }

    @Override
    public String getDetailTableName(Long billId, int detailIndex) {
        return TableNameUtil.getDetailTableName(billId, detailIndex);
    }

}
