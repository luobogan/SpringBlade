-- =============================================================================
-- 迁移脚本 V2026.10.14_004：机构默认首页 菜单 + 授权
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_004
-- 目标库    : blade
-- 影响表    : blade_menu（子菜单 1）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 说明      : 机构默认首页管理页（普通子菜单 → 组件 ./pages/Portal/DefaultPortal/index.tsx）。
--             管理类功能，仅授权 administrator。path 末段连写 defaultportal。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--    WHERE m.`code` = 'portal_default_org';
--   DELETE FROM `blade_menu` WHERE `code` = 'portal_default_org';
-- =============================================================================

USE `blade`;

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       p.`id`,
       'portal_default_org', '机构默认首页', 'portal_default_org', '/portal/defaultportal', 'api', 6,
       1, 1, 0, 0,
       '机构默认首页（普通子菜单 → 组件 ./pages/Portal/DefaultPortal/index.tsx；对标泛微 defaultportal）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_default_org' AND m.`is_deleted` = 0);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` = 'portal_default_org'
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

SELECT m.`id`, m.`code`, m.`name`, m.`path`, m.`category`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm
          JOIN `blade_role` rr ON rr.`id` = rm.`role_id`
         WHERE rm.`menu_id` = m.`id` AND rr.`role_alias` = 'administrator') AS `admin_granted`
  FROM `blade_menu` m
 WHERE m.`code` = 'portal_default_org' AND m.`is_deleted` = 0;
