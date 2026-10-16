-- =============================================================================
-- 迁移脚本 V2026.10.16_006：门户权限码细化（对标泛微 PortalRight 14 码 §17.2）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_006
-- 目标库    : blade
-- 影响表    : blade_menu（权限码按钮 6）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §17.2
--             泛微 PortalRight 枚举把「主题/移动/菜单/工具栏/素材库/协同/浮动区」拆为独立权限码；
--             改造前 blade-portal 只有 4 个权限码，主题/布局/元素库/登录皮肤/日志全部复用
--             portal_page_maint，粒度过粗。本迁移补齐 6 个细分权限码，后端 Controller 同步切换。
-- 说明      : 既有 portal_page_maint / portal_element_maint / portal_page_default /
--             portal_menu_maint 保留不动（向后兼容）；新码仅 administrator 授予，
--             PortalRightUtil.check 对 administrator 直通，存量部署无感知。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_theme_maint','portal_layout_maint','portal_element_lib_maint',
--                        'portal_login_template_maint','portal_log_view','portal_account_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_theme_maint','portal_layout_maint',
--                        'portal_element_lib_maint','portal_login_template_maint',
--                        'portal_log_view','portal_account_maint');
-- =============================================================================

USE `blade`;

-- 1) 细分权限码（category=2, is_component=0, path 空；仅作门禁码）------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, '门户细分权限码（对标泛微 PortalRight，供 PortalRightUtil.check）', 0, '000000', 1
FROM (
        SELECT 'portal_theme_maint'          AS `code`, '门户主题维护权限'   AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_layout_maint',          '门户布局模板维护权限', 2
  UNION ALL SELECT 'portal_element_lib_maint',     '门户元素库维护权限',   3
  UNION ALL SELECT 'portal_login_template_maint',  '登录页皮肤维护权限',   4
  UNION ALL SELECT 'portal_log_view',              '门户审计日志查看权限', 5
  UNION ALL SELECT 'portal_account_maint',         '门户个人设置管理权限', 6
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 2) 授权：仅超级管理员 ---------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_theme_maint','portal_layout_maint','portal_element_lib_maint',
                   'portal_login_template_maint','portal_log_view','portal_account_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 3) 校验输出 -------------------------------------------------------------------------------
SELECT m.`code`, m.`name`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm
          JOIN `blade_role` rr ON rr.`id` = rm.`role_id`
         WHERE rm.`menu_id` = m.`id` AND rr.`role_alias` = 'administrator') AS `admin_granted`
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_theme_maint','portal_layout_maint','portal_element_lib_maint',
                    'portal_login_template_maint','portal_log_view','portal_account_maint')
   AND m.`is_deleted` = 0
 ORDER BY m.`sort`;
