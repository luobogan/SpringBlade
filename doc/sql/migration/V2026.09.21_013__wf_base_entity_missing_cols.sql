-- -----------------------------------------------------------------------------
-- 迁移脚本：为 6 张 wf_ 表补齐 TenantEntity/BaseEntity 必备公共列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_013
-- 目标库    : blade_workflow
-- 影响表    : wf_custom_operation（修正）
--             wf_custom_operation_action（修正）
--             wf_custom_operation_right（修正）
--             wf_node_default_sign（修正）
--             wf_node_timeout（修正）
--             wf_subflow_request（修正）
-- 变更类型  : 改表（DDL，仅补列）
-- 是否幂等  : 是（information_schema 列存在性守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否（仅 ADD COLUMN，均带默认值，存量行自动取值）
-- 回滚脚本  : 对上述 6 张表分别执行
--               ALTER TABLE `{表名}` DROP COLUMN `create_user`, DROP COLUMN `create_dept`,
--                 DROP COLUMN `update_user`, DROP COLUMN `update_time`,
--                 DROP COLUMN `status`, DROP COLUMN `is_deleted`;
--             （wf_subflow_request 回滚时不要 DROP `status`：该列是建表自带的业务状态列）
--
-- 背景：实体均继承 org.springblade.core.mp.base.TenantEntity（其父类 BaseEntity 含
--   create_user / create_dept / create_time / update_user / update_time / status / is_deleted），
--   而下列建表脚本只建了 tenant_id + create_time，漏建其余公共列，导致 MyBatis-Plus
--   生成的基础查询（SELECT ... FROM xxx WHERE is_deleted = 0）报
--   Unknown column 'create_user' in 'field list'。
--
--   首个暴露点：WfCustomOperationMapper 查询 wf_custom_operation（节点「自定义操作」按钮）；
--   同一缺陷还潜藏在另外 5 张表上（同一次批量建表漏列 / 同一种建表模板），
--   一旦对应 Mapper 被调用即会同样报错，故一并在本脚本修正。
--
--   与 V2026.09.21_011（wf_node_detail_filter 补列）属同一类问题、同一处理口径。
--
-- 列定义口径：对齐本库多数表（如 wf_process_node / wf_custom_action）的实际定义，
--   其中 is_deleted 用 INT（本库 14 张表为 INT，仅 011 补的那张为 TINYINT）。
-- =============================================================================

USE `blade_workflow`;

-- =============================================================================
-- 1) wf_custom_operation（节点自定义操作按钮）
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2) wf_custom_operation_action（自定义操作动作明细）
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_action' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_action` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 3) wf_custom_operation_right（自定义操作权限矩阵）
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_custom_operation_right' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_custom_operation_right` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 4) wf_node_default_sign（按操作类型默认签字意见）
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_default_sign' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_default_sign` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 5) wf_node_timeout（节点超时规则）
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_node_timeout` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 6) wf_subflow_request（主/子流程请求关系）
--    注：本表建表时已有业务列 `status`（tinyint，子流程归档状态），此处不再补 status。
-- =============================================================================
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_subflow_request` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_subflow_request` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_subflow_request` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_subflow_request` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_subflow_request` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 校验：每张表的 6 个公共列计数（期望 6；wf_subflow_request 的 status 为建表自带，同样计 1）
-- =============================================================================
SELECT t.TABLE_NAME,
       SUM(c.COLUMN_NAME = 'create_user') AS create_user,
       SUM(c.COLUMN_NAME = 'create_dept') AS create_dept,
       SUM(c.COLUMN_NAME = 'update_user') AS update_user,
       SUM(c.COLUMN_NAME = 'update_time') AS update_time,
       SUM(c.COLUMN_NAME = 'status')      AS status,
       SUM(c.COLUMN_NAME = 'is_deleted')  AS is_deleted
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_NAME IN ('wf_custom_operation', 'wf_custom_operation_action',
                       'wf_custom_operation_right', 'wf_node_default_sign',
                       'wf_node_timeout', 'wf_subflow_request')
GROUP BY t.TABLE_NAME
ORDER BY t.TABLE_NAME;
