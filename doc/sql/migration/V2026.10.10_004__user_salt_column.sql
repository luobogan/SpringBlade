-- 补齐 blade_user 的 salt 列（密码盐值；实体 User.salt 默认映射 salt 列）
-- 注意：salt 已存在于基线建表脚本(blade.sql/blade20261009.sql)与 alter_blade_user_hrm.sql 中，
-- 故用 information_schema 守卫保证幂等，避免重复列报错(Duplicate column name 'salt')。
-- 列已存在时本脚本为空操作，不会改动现有 varchar(64) 定义。

SET @db = 'blade';
SET @tbl = 'blade_user';
SET @col = 'salt';

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) > 0,
    'SELECT 1',
    'ALTER TABLE `blade_user` ADD COLUMN `salt` varchar(255) DEFAULT NULL COMMENT ''密码盐值(预留,配合加盐算法惰性升级)'''
  )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
