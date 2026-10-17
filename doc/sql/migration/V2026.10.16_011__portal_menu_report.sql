-- =============================================================================
-- 迁移脚本 V2026.10.16_011：报表菜单（reportMenu）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_011
-- 目标库    : blade
-- 影响表    : blade_portal_menu（MODIFY 注释 + INSERT report 类型种子）
-- 变更类型  : DDL(注释) + DML
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §14.2 D（reportMenu 报表菜单）
--              泛微：维护「我的报表」报表类菜单（本质是 menutype=report 的子集，
--              根节点 id=10），无独立报表菜单表，复用 MAINMENUINFO/MAINMENUCONFIG；
--              数据源 MenuUtil("top","report",...)。
-- 本地化说明:
--              1) 不建新表——blade_portal_menu 同表分型新增 menu_type='report'，
--                 完整复用现有 CRUD/授权(share)/树(/menu/tree?menuType=report)能力；
--              2) 报表菜单不进首页顶部/左侧渲染（避免与业务导航竞争），
--                 供「我的报表」门户场景按 menuType=report 拉取；
--              3) 种子构成一棵独立树：根「我的报表」+ 子（报表设计器/统计仪表盘/
--                 数据报表），路由指向 JimuReport 约定路径（/report/*），
--                 管理员可在「门户菜单管理 → 报表菜单」维护。
-- 是否幂等  : 是（ALTER 前判重；种子按 名称+类型 判重）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE FROM `blade_portal_menu` WHERE `menu_type` = 'report' AND `is_system` = 0;
--   ALTER TABLE `blade_portal_menu`
--     MODIFY COLUMN `menu_type` varchar(32) NOT NULL
--       COMMENT '菜单类型：top顶部/front前端/toolbar工具栏/quick快捷入口';
-- =============================================================================

USE `blade`;

-- 1) menu_type 注释追加 report ---------------------------------------------------
ALTER TABLE `blade_portal_menu`
  MODIFY COLUMN `menu_type` varchar(32) NOT NULL
    COMMENT '菜单类型：top顶部/front前端/toolbar工具栏/quick快捷入口/report报表菜单';

-- 2) 种子：报表菜单独立树（根「我的报表」）------------------------------------------
INSERT INTO `blade_portal_menu`
  (`tenant_id`,`parent_id`,`menu_name`,`icon`,`route_url`,`menu_type`,`link_mode`,
   `sort_order`,`visible`,`is_system`,`is_deleted`,`status`,`create_user`,`create_dept`,`create_time`)
SELECT '000000',0,'我的报表','bar-chart','/report/list','report',1,1,1,0,0,1,
       1123598821738675201,1123598813738675201,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu`
                   WHERE `tenant_id`='000000' AND `menu_type`='report' AND `menu_name`='我的报表' AND `is_deleted`=0);

SET @report_root_id := (SELECT `id` FROM `blade_portal_menu`
                         WHERE `tenant_id`='000000' AND `menu_type`='report'
                           AND `menu_name`='我的报表' AND `is_deleted`=0 LIMIT 1);

-- 3) 种子：报表菜单子节点 -----------------------------------------------------------
INSERT INTO `blade_portal_menu`
  (`tenant_id`,`parent_id`,`menu_name`,`icon`,`route_url`,`menu_type`,`link_mode`,
   `sort_order`,`visible`,`is_system`,`is_deleted`,`status`,`create_user`,`create_dept`,`create_time`)
SELECT '000000',@report_root_id,v.`menu_name`,v.`icon`,v.`route_url`,'report',1,v.`sort_order`,1,0,0,1,
       1123598821738675201,1123598813738675201,NOW()
FROM (
        SELECT '报表设计器' AS `menu_name`, 'edit' AS `icon`, '/report/design' AS `route_url`, 1 AS `sort_order`
  UNION ALL SELECT '统计仪表盘', 'dashboard', '/report/dashboard', 2
  UNION ALL SELECT '数据报表',    'table',    '/report/view',     3
) v
WHERE @report_root_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_menu`
                   WHERE `tenant_id`='000000' AND `menu_type`='report'
                     AND `menu_name`=v.`menu_name` AND `is_deleted`=0);

-- 4) 校验输出 ---------------------------------------------------------------------
SELECT `id`, `parent_id`, `menu_name`, `route_url` FROM `blade_portal_menu`
 WHERE `menu_type` = 'report' AND `is_deleted` = 0 ORDER BY `parent_id`, `sort_order`;
