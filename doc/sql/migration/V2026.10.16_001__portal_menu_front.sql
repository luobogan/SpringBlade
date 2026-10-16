-- =============================================================================
-- 迁移脚本 V2026.10.16_001：门户前端菜单（左侧导航 front）种子数据
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_001
-- 目标库    : blade
-- 影响表    : blade_portal_menu（INSERT 种子）
-- 变更类型  : DML
-- 说明      : 「前端菜单」= 门户左侧导航（对标泛微 portalmenu leftmenu，menu_type=front）。
--              运行时 GET /blade-portal/menu/tree?menuType=front 返回（按授权过滤，
--              无授权记录=全员可见）。本脚本仅撒演示种子，管理员可在「门户菜单管理」维护。
-- 回滚脚本  : DELETE FROM `blade_portal_menu` WHERE `menu_type` = 'front';
-- =============================================================================

USE `blade`;

INSERT INTO `blade_portal_menu`
  (`tenant_id`, `parent_id`, `menu_name`, `icon`, `route_url`, `menu_type`, `link_mode`,
   `sort_order`, `visible`, `is_system`, `is_deleted`, `status`, `create_user`, `create_dept`, `create_time`, `update_time`)
SELECT '000000', 0, '门户首页', 'home', '/portal/home', 'front', 1, 1, 1, 0, 0, 1, 1123598821738675201, 1123598813738675201, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `tenant_id`='000000' AND `menu_type`='front' AND `menu_name`='门户首页' AND `is_deleted`=0);

INSERT INTO `blade_portal_menu`
  (`tenant_id`, `parent_id`, `menu_name`, `icon`, `route_url`, `menu_type`, `link_mode`,
   `sort_order`, `visible`, `is_system`, `is_deleted`, `status`, `create_user`, `create_dept`, `create_time`, `update_time`)
SELECT '000000', 0, '进入后台', 'dashboard', '/', 'front', 1, 2, 1, 0, 0, 1, 1123598821738675201, 1123598813738675201, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `tenant_id`='000000' AND `menu_type`='front' AND `menu_name`='进入后台' AND `is_deleted`=0);

INSERT INTO `blade_portal_menu`
  (`tenant_id`, `parent_id`, `menu_name`, `icon`, `route_url`, `menu_type`, `link_mode`,
   `sort_order`, `visible`, `is_system`, `is_deleted`, `status`, `create_user`, `create_dept`, `create_time`, `update_time`)
SELECT '000000', 0, '门户设计', 'appstore', '/portal/design', 'front', 1, 3, 1, 0, 0, 1, 1123598821738675201, 1123598813738675201, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `tenant_id`='000000' AND `menu_type`='front' AND `menu_name`='门户设计' AND `is_deleted`=0);

-- 校验输出 -------------------------------------------------------------------
SELECT `menu_type`, COUNT(*) AS `cnt` FROM `blade_portal_menu` WHERE `is_deleted` = 0 GROUP BY `menu_type`;
