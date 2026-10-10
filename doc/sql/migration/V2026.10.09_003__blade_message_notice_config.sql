-- V2026.10.09_003__blade_message_notice_config.sql
-- 流程消息集成三期（消息中心设计文档 §10.8 T11.1，对齐 ecology ECOLOGY_MESSAGE_CONFIG 的用户级提醒开关）：
--   新建 blade_message_notice_config：用户级流程通知接收配置。
--     user_id + flow_key 唯一；flow_key='*' 表示全部流程（通配）。
--     enabled：1=接收 0=屏蔽。
--   过滤规则（发送侧 blade-message.sendNoticeToUsers 收口）：
--     精确配置（flow_key=具体流程key）优先于通配（'*'）；
--     无任何配置 → 默认接收。
--   即"表里只存偏离默认值的记录"：屏蔽=写 enabled=0；恢复接收=删行。
-- 幂等：表已存在则跳过，可重复执行。

SET @db = DATABASE();

SET @tbl_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'blade_message_notice_config'
);
SET @create_sql = 'CREATE TABLE blade_message_notice_config (`id` BIGINT NOT NULL COMMENT ''主键'', `tenant_id` VARCHAR(50) NULL COMMENT ''租户ID'', `user_id` BIGINT NOT NULL COMMENT ''用户ID（blade_user.id）'', `flow_key` VARCHAR(64) NOT NULL COMMENT ''流程定义key（proc_key）；* = 全部流程'', `enabled` INT NOT NULL DEFAULT 1 COMMENT ''是否接收 1=接收 0=屏蔽'', `create_time` DATETIME NULL COMMENT ''创建时间'', `create_user` BIGINT NULL COMMENT ''创建人'', `create_dept` BIGINT NULL COMMENT ''创建部门'', `update_time` DATETIME NULL COMMENT ''更新时间'', `update_user` BIGINT NULL COMMENT ''更新人'', `status` INT NULL COMMENT ''状态'', `is_deleted` INT NULL COMMENT ''是否已删除'', PRIMARY KEY (`id`), UNIQUE KEY `uk_blade_message_notice_config_user_flow` (`user_id`, `flow_key`), KEY `idx_blade_message_notice_config_tenant` (`tenant_id`)) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = ''流程通知用户级接收配置''';
SET @sql = IF(@tbl_exists = 0, @create_sql, 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
