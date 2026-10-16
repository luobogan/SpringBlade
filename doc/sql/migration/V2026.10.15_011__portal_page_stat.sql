-- =============================================================================
-- 迁移脚本 V2026.10.15_011：门户页面访问趋势按日统计 blade_portal_page_stat
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_011
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_page_stat
-- 变更类型  : DDL + DML(索引)
-- 说明      : 在 blade_portal_page.view_count（累计热度）基础上，按「页面+日期+租户」
--              维度记录每日 PV，用于访问趋势报表。唯一键 (page_id, stat_date, tenant_id)
--              配合 INSERT ... ON DUPLICATE KEY UPDATE 做原子自增。
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_page_stat`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_page_stat` (
  `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `page_id`    bigint       NOT NULL                COMMENT '门户页面ID',
  `stat_date`  varchar(10)  NOT NULL                COMMENT '统计日期 yyyy-MM-dd',
  `view_count` bigint       NOT NULL DEFAULT '0'    COMMENT '当日访问量',
  `tenant_id`  varchar(32)  DEFAULT '000000'        COMMENT '租户ID',
  `is_deleted` int          NOT NULL DEFAULT '0'    COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_date_tenant` (`page_id`, `stat_date`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户页面按日访问统计';

-- 校验输出 -------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT
  FROM information_schema.columns
 WHERE table_schema = 'blade' AND table_name = 'blade_portal_page_stat'
 ORDER BY ORDINAL_POSITION;

-- 4) 管理菜单：门户访问分析（普通子菜单，/portal/stats → ./pages/Portal/Stats/index.tsx）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_stats', '门户访问分析', 'portal_stats', '/portal/stats', 'api', 11,
       1, 1, 0, 0,
       '门户访问分析（普通子菜单 → 组件 ./pages/Portal/Stats/index.tsx；访问趋势/热门页，需页面维护权限）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_stats' AND m.`is_deleted` = 0);

-- 5) 授权：门户访问分析页 → 超级管理员（role_alias=administrator），否则 /menu/routes 不返回该路由
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` = 'portal_stats'
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`);
