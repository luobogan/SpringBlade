-- =============================================================================
-- 迁移脚本 V2026.10.16_007：导航菜单源可配置（menu_source）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_007
-- 目标库    : blade
-- 影响表    : blade_portal_setting（ALTER 加列）
-- 变更类型  : DDL
-- 说明      : 门户首页「前端菜单(左)/后端菜单(顶)」的导航源可配置：
--              1 = 系统菜单（blade_menu，默认，保持现有行为）
--              2 = 门户私有菜单（blade_portal_menu.menu_type=front/top，对标泛微
--                  portalmenu leftmenu/topmenu 的“门户自有导航”语义）
--              设计蓝本：doc/md/ecology-portal-config-analysis.md §14.1
--              （泛微 LEFTMenuInfo/MAINMenuInfo 两表分型 vs 本项目同表分型的
--              本地化偏差处置：不拆物理表，以配置项声明导航源，见 §14 门户菜单）。
-- 是否幂等  : 是（ADD COLUMN 前判重）
-- 是否丢数据: 否（仅加列，存量行默认 1）
-- 回滚脚本  :
--   ALTER TABLE `blade_portal_setting` DROP COLUMN `menu_source`;
-- =============================================================================

USE `blade`;

SET @exist := (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
   WHERE `TABLE_SCHEMA` = DATABASE()
     AND `TABLE_NAME` = 'blade_portal_setting'
     AND `COLUMN_NAME` = 'menu_source'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE `blade_portal_setting`
     ADD COLUMN `menu_source` tinyint NOT NULL DEFAULT ''1''
       COMMENT ''导航菜单源：1系统菜单(blade_menu) 2门户私有菜单(blade_portal_menu front/top)''',
  'SELECT ''menu_source already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验输出 -------------------------------------------------------------------
SELECT `tenant_id`, `menu_source` FROM `blade_portal_setting` WHERE `is_deleted` = 0;
