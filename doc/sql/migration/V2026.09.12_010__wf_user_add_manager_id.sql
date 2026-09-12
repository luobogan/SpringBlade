-- V2026.09.12_010__wf_user_add_manager_id.sql
-- 流程节点操作者解析（E9 对齐）：给 blade_user 增加「主管」字段。
-- 上级类操作者（18 创建人上级 / 41 上级 / 6 字段-人员上级）解析为 manager_id 指向的用户，
-- 语义最贴近 E9 的 ManagerID（个人主管 / 直线经理）。
-- 幂等：列已存在则跳过，可重复执行。

SET @db = DATABASE();
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'blade_user'
      AND COLUMN_NAME = 'manager_id'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE blade_user ADD COLUMN manager_id BIGINT(20) DEFAULT NULL COMMENT ''主管用户ID（上级/直线经理）''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
