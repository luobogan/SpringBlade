-- =============================================================================
-- 迁移脚本 V2026.10.15_010：门户页面访问热度统计 view_count
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_010
-- 目标库    : blade
-- 影响表    : blade_portal_page（ADD view_count）
-- 变更类型  : DDL
-- 是否幂等  : 是（information_schema 守卫加列）
-- 是否丢数据: 否
-- 说明      : /my-portal 与 /pre-login/portal 渲染时对目标页 view_count 原子 +1（热度统计）；
--              计数失败仅记日志，绝不影响门户渲染。
-- 回滚脚本  : ALTER TABLE `blade_portal_page` DROP COLUMN `view_count`;
-- =============================================================================

USE `blade`;

SET @dbname = DATABASE();
SET @tbl = 'blade_portal_page';
SET @col = 'view_count';
SET @s = IF(
  (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = @tbl AND column_name = @col) = 0,
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
         '` bigint DEFAULT ''0'' COMMENT ''访问次数（热度统计）'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验输出 -------------------------------------------------------------------
SELECT `id`, `page_code`, `page_name`, `view_count`
  FROM `blade_portal_page` WHERE `is_deleted` = 0 ORDER BY `view_count` DESC;
