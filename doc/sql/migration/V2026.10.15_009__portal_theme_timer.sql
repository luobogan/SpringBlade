-- =============================================================================
-- 迁移脚本 V2026.10.15_009：门户主题定时切换（open_timer）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_009
-- 目标库    : blade
-- 影响表    : blade_portal_theme（ADD open_timer / timer_start / timer_end）
-- 变更类型  : DDL
-- 是否幂等  : 是（information_schema 守卫逐列加）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §16.1 / §18.2 / §18.3④
--              SystemLoginTemplate.openTimer 的拉模式（无 Quartz Job，渲染时按当前时间比对）。
--              门户主题同理：open_timer=1 且当前在 [timer_start,timer_end] 的主题，
--              在解析时优先于「租户默认主题」（但仍低于用户偏好与页面指定主题）。
-- 回滚脚本  :
--   ALTER TABLE `blade_portal_theme` DROP COLUMN `open_timer`;
--   ALTER TABLE `blade_portal_theme` DROP COLUMN `timer_start`;
--   ALTER TABLE `blade_portal_theme` DROP COLUMN `timer_end`;
-- =============================================================================

USE `blade`;

SET @dbname = DATABASE();
SET @tbl = 'blade_portal_theme';

-- open_timer
SET @col = 'open_timer';
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=@dbname AND table_name=@tbl AND column_name=@col)=0,
  CONCAT('ALTER TABLE `',@tbl,'` ADD COLUMN `',@col,'` int DEFAULT ''0'' COMMENT ''是否开启定时切换（0否 1是）'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- timer_start
SET @col = 'timer_start';
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=@dbname AND table_name=@tbl AND column_name=@col)=0,
  CONCAT('ALTER TABLE `',@tbl,'` ADD COLUMN `',@col,'` varchar(32) DEFAULT NULL COMMENT ''定时开始 yyyy-MM-dd HH:mm:ss'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- timer_end
SET @col = 'timer_end';
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=@dbname AND table_name=@tbl AND column_name=@col)=0,
  CONCAT('ALTER TABLE `',@tbl,'` ADD COLUMN `',@col,'` varchar(32) DEFAULT NULL COMMENT ''定时结束 yyyy-MM-dd HH:mm:ss'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验输出 -------------------------------------------------------------------
SELECT `id`, `theme_key`, `theme_name`, `open_timer`, `timer_start`, `timer_end`
  FROM `blade_portal_theme` WHERE `is_deleted` = 0 ORDER BY `id`;
