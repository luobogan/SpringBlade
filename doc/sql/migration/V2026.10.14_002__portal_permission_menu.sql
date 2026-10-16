-- =============================================================================
-- 迁移脚本 V2026.10.14_002：门户权限按钮（category=2）+ blade_role_menu 授权
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_002
-- 目标库    : blade
-- 影响表    : blade_menu（权限按钮 3）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §13.4
--              泛微 PortalRight 权限码 + biz/right 反射策略 + PortalRightUtil.checkUserRight
-- 说明      : 三个权限码对应 PortalConstant.PERM_PAGE_MAINT / PERM_ELEMENT_MAINT / PERM_PAGE_DEFAULT，
--             由后端 PortalRightUtil.check(code) 直查 blade_role_menu 判定（administrator 已在下授全量）。
--             权限按钮为 category=2 && is_component=0（path 留空）：不注册路由、不进侧边菜单，仅作门禁码。
-- 授权范围  : administrator 全给；user 暂不给维护类权限（与现状一致，后续可按角色扩展）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--    WHERE m.`code` IN ('portal_page_maint','portal_element_maint','portal_page_default');
--   DELETE FROM `blade_menu`
--    WHERE `code` IN ('portal_page_maint','portal_element_maint','portal_page_default');
-- =============================================================================

USE `blade`;

-- 1) 权限按钮（category=2, is_component=0, path 空；挂 portal 菜单下）--------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, '门户权限按钮（仅门禁码，供 PortalRightUtil.check）', 0, '000000', 1
FROM (
        SELECT 'portal_page_maint'    AS `code`, '门户页面维护' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_element_maint', '门户元素维护', 2
  UNION ALL SELECT 'portal_page_default', '设为默认门户', 3
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 2) 授权：administrator 全给；user 不给维护类权限 -------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_page_maint', 'portal_element_maint', 'portal_page_default')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 3) 校验输出 ---------------------------------------------------------------------
SELECT m.`code`, m.`name`, m.`category`, m.`is_component`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm
          JOIN `blade_role` rr ON rr.`id` = rm.`role_id`
         WHERE rm.`menu_id` = m.`id` AND rr.`role_alias` = 'administrator') AS `admin_granted`
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_page_maint', 'portal_element_maint', 'portal_page_default')
   AND m.`is_deleted` = 0
 ORDER BY m.`sort`;
