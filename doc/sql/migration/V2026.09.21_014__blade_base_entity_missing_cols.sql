-- -----------------------------------------------------------------------------
-- 迁移脚本：为 blade_menu_component / blade_top_menu_setting 补齐 TenantEntity 必备列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_014
-- 目标库    : blade（系统库）
-- 影响表    : blade_menu_component（修正）
--             blade_top_menu_setting（修正）
-- 变更类型  : 改表（DDL，补列 + 两列类型归一）
-- 是否幂等  : 是（information_schema 列存在性/类型守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否（仅 ADD COLUMN；create_user/update_user 仅把空串/非数字值置 NULL 后转 BIGINT，
--             现有 5 行该两列本就为空串，无信息丢失）
-- 回滚脚本  : ALTER TABLE `blade_top_menu_setting`
--               DROP COLUMN `create_user`, DROP COLUMN `create_dept`, DROP COLUMN `create_time`,
--               DROP COLUMN `update_user`, DROP COLUMN `update_time`, DROP COLUMN `status`,
--               DROP COLUMN `is_deleted`, DROP COLUMN `tenant_id`;
--             ALTER TABLE `blade_menu_component`
--               DROP COLUMN `create_dept`, DROP COLUMN `is_deleted`, DROP COLUMN `tenant_id`;
--             （create_user/update_user 的 varchar→bigint 归一不回滚：varchar 是旧版 Blade 遗留写法）
--
-- 背景：本地提交「feat: 新增租户管理功能及相关优化」把 TopMenuSetting 改为
--   extends TenantEntity，并新增 MenuComponent 实体（同样 extends TenantEntity），
--   但两张表未同步补列，导致 MyBatis-Plus 生成的基础查询报
--   Unknown column 'is_deleted' / 'create_dept' / 'tenant_id' in 'field list'。
--   与 V2026.09.21_013（wf_ 表补列）、V2026.09.21_011（wf_node_detail_filter 补列）
--   属同一类问题、同一处理口径。
--
-- 列定义口径：对齐本库其它 TenantEntity 表（如 blade_notice）的实际定义：
--   tenant_id varchar(12) DEFAULT '000000'、create_user/create_dept/update_user BIGINT、
--   create_time/update_time DATETIME、status INT、is_deleted INT NOT NULL DEFAULT 0。
--
-- 另：blade_menu_component 的 create_user/update_user 是旧版 Blade 的 varchar(64) 写法
--   （值为空串 ''），实体侧是 Long —— 空串映射 Long 会触发类型转换异常，
--   故一并归一为 BIGINT（先把空串/非数字值置 NULL，避免严格模式下 MODIFY 报错）。
-- =============================================================================

USE `blade`;

-- =============================================================================
-- 1) blade_menu_component（菜单组件）
-- =============================================================================
-- 1.1 create_user / update_user 归一：空串与非数字值先置 NULL（严格模式下 varchar→bigint 才能转换）
UPDATE `blade_menu_component` SET `create_user` = NULL
  WHERE `create_user` IS NOT NULL AND `create_user` NOT REGEXP '^[0-9]+$';
UPDATE `blade_menu_component` SET `update_user` = NULL
  WHERE `update_user` IS NOT NULL AND `update_user` NOT REGEXP '^[0-9]+$';

-- 1.2 create_user 类型归一（仅当仍为 varchar 时）
SET @t := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_menu_component'
    AND COLUMN_NAME = 'create_user' AND DATA_TYPE = 'varchar');
SET @sql := IF(@t = 1,
  'ALTER TABLE `blade_menu_component` MODIFY COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 update_user 类型归一（仅当仍为 varchar 时）
SET @t := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_menu_component'
    AND COLUMN_NAME = 'update_user' AND DATA_TYPE = 'varchar');
SET @sql := IF(@t = 1,
  'ALTER TABLE `blade_menu_component` MODIFY COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 create_dept
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_menu_component' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_menu_component` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 is_deleted
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_menu_component' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_menu_component` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''是否已删除''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 tenant_id
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_menu_component' AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_menu_component` ADD COLUMN `tenant_id` VARCHAR(12) DEFAULT ''000000'' COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2) blade_top_menu_setting（顶部菜单设置，纯关联表，当前 0 行）
-- =============================================================================
-- 2.1 create_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 create_dept
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 create_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'create_time');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `create_time` DATETIME NULL COMMENT ''创建时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.4 update_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.5 update_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `update_time` DATETIME NULL COMMENT ''修改时间''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.6 status
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `status` INT DEFAULT 1 COMMENT ''状态:1正常 0禁用''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.7 is_deleted
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''是否已删除''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.8 tenant_id
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blade_top_menu_setting' AND COLUMN_NAME = 'tenant_id');
SET @sql := IF(@col = 0,
  'ALTER TABLE `blade_top_menu_setting` ADD COLUMN `tenant_id` VARCHAR(12) DEFAULT ''000000'' COMMENT ''租户ID''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 校验：两表 8 个公共列计数（期望均为 1）
-- =============================================================================
SELECT t.TABLE_NAME,
       SUM(c.COLUMN_NAME = 'create_user') AS create_user,
       SUM(c.COLUMN_NAME = 'create_dept') AS create_dept,
       SUM(c.COLUMN_NAME = 'create_time') AS create_time,
       SUM(c.COLUMN_NAME = 'update_user') AS update_user,
       SUM(c.COLUMN_NAME = 'update_time') AS update_time,
       SUM(c.COLUMN_NAME = 'status')      AS status,
       SUM(c.COLUMN_NAME = 'is_deleted')  AS is_deleted,
       SUM(c.COLUMN_NAME = 'tenant_id')   AS tenant_id,
       MAX(IF(c.COLUMN_NAME = 'create_user', c.COLUMN_TYPE, '')) AS create_user_type
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_NAME IN ('blade_menu_component', 'blade_top_menu_setting')
GROUP BY t.TABLE_NAME
ORDER BY t.TABLE_NAME;
