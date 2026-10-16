-- =============================================================================
-- 迁移脚本 003：门户主题中心 —— 菜单驱动化（/portal/theme）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_001
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（主题中心子菜单 1 + 权限按钮 1）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按 menu_id+role_id 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('portal_theme','portal_theme_manage');
--   DELETE FROM `blade_menu`
--    WHERE `code` IN ('portal_theme','portal_theme_manage');
--
-- 背景
--   门户引擎新增「主题库（themestyle）+ 主题个性化中心（themecenter）」，需要一个独立
--   的主题中心页供管理员维护主题（配色/Logo/背景）。沿用本项目「前端路由 100% 由
--   blade_menu 驱动」约定，登记菜单行即可（不碰 config/routes.ts）。
--
-- 前端解析规则（src/app.tsx，与 002 脚本一致）
--   * 普通子菜单（category=1 / is_open=0）：套 ProLayout 外壳、进左侧菜单、注册布局内路由。
--   * /portal/theme → module=Portal, page=Theme, rawPage=theme，buildComponentLoaders 候选含
--       ./pages/Portal/Theme/index.tsx（本脚本按此命名落盘组件）。
--   * toPascalCase('theme') = 'Theme'（commonWords 无需补），目录 Theme 大小写天然匹配。
--   * 主题中心写接口复用后端 PERM_PAGE_MAINT，前端用 usePageButtons('portal') 门禁，
--       故新增按钮 portal_theme_manage 仅作为权限码占位（与 portal_page_manage 同权）。
--
-- 授权范围
--   administrator（超级管理员）：门户全部菜单 + 两个管理按钮（含主题）
--   user（用户）              ：仅查看页（不含主题中心）
-- =============================================================================

USE `blade`;

-- 1) 主题中心页（普通子菜单，两段路径 → ./pages/Portal/Theme/index.tsx）----------------
--    path 末段连写 theme → Theme（目录名 Theme 与 toPascalCase 结果一致）。
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       p.`id`,
       'portal_theme', '门户主题', 'portal_theme', '/portal/theme', 'api', 5,
       1, 1, 0, 0,
       '门户主题中心（普通子菜单 → 组件 ./pages/Portal/Theme/index.tsx；管理员维护主题库）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_theme' AND m.`is_deleted` = 0);

-- 2) 权限按钮（category=2，is_component=0，path 留空，供前端按钮 code 门禁）--------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       'portal_theme_manage', '门户主题管理', 'portal_theme_manage', '', 'api', 3,
       2, 1, 0, 0, NULL, 0, '000000', 1
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal_theme_manage' AND `is_deleted` = 0);

-- 3) 授权：超级管理员拿主题中心菜单 + 主题管理按钮 ----------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND (
        (r.`role_alias` = 'administrator'
         AND m.`code` IN ('portal_theme','portal_theme_manage'))
     )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 4) 校验输出 ---------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`code`, m.`name`, m.`path`, m.`category`, m.`is_open`, m.`is_component`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS granted_roles
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_theme','portal_theme_manage')
   AND m.`is_deleted` = 0
 ORDER BY m.`category`, m.`sort`;
