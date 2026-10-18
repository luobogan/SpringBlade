-- =============================================================================
-- 迁移脚本 V2026.10.17_001：门户引擎六类菜单独立成左侧菜单项
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.17_001
-- 目标库    : blade
-- 影响表    : blade_menu（新增 6 条门户子菜单）、blade_role_menu（授权 administrator）
-- 设计蓝本  : D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\index.js:104-113
--             portalmenu 下 6 条同级路由（对标泛微门户引擎左侧菜单）
-- 说明      : 原先这 6 类门户菜单被合并在「门户菜单管理」(/portal/menumanage) 一页里
--             用 menuType 下拉切换，用户在左侧菜单点不到。本次按泛微口径拆成 6 个
--             独立菜单页，各自锁定一个 menu_type：
--               前端菜单      leftmenu      → menu_type=front   /portal/frontmenu
--               后端菜单      topmenu       → menu_type=top     /portal/topmenu
--               自定义菜单    custommenu    → 跨类型查看        /portal/custommenu
--               菜单样式库    menustylelib  → 菜单样式页签      /portal/menustylelib
--               工具栏菜单    toolbarmore   → menu_type=toolbar /portal/toolbar
--               个人通用设置菜单 accountmore → 个人设置          /portal/accountmore
--             前 4/6 项复用 src/pages/Portal/MenuManage、StyleLib 组件；
--             个人通用设置菜单是新增页（后端 /portal/account-more/get|save 已存在）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('portal_left_menu','portal_top_menu','portal_custom_menu',
--                         'portal_menu_style_lib','portal_toolbar_menu','portal_account_more');
--   DELETE FROM `blade_menu` WHERE `code` IN
--     ('portal_left_menu','portal_top_menu','portal_custom_menu',
--      'portal_menu_style_lib','portal_toolbar_menu','portal_account_more');
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 六个菜单页（category=1 普通子菜单，两段连写路径 → ./pages/Portal/{Page}/index.tsx）
--    ⚠️ path 末段大小写必须与磁盘目录严格一致（app.tsx buildComponentLoaders 规则）：
--       /portal/frontmenu    → FrontMenu
--       /portal/topmenu      → TopMenu
--       /portal/custommenu   → CustomMenu
--       /portal/menustylelib → MenuStyleLib（已在 app.tsx PAGE_DIR_ALIAS 登记别名）
--       /portal/toolbar      → Toolbar
--       /portal/accountmore  → AccountMore
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       v.`code`, v.`name`, v.`code`, v.`path`, 'api', v.`sort`,
       1, 1, 0, 0, v.`remark`, 0, '000000', 1
FROM `blade_menu` p
JOIN (
        SELECT 'portal_left_menu'     AS `code`, '前端菜单'         AS `name`, '/portal/frontmenu'    AS `path`, 20 AS `sort`,
               '门户前端（左侧）导航菜单，对标泛微 leftmenu；menu_type=front' AS `remark`
  UNION ALL SELECT 'portal_top_menu',     '后端菜单',     '/portal/topmenu',     21, '门户顶部导航菜单，对标泛微 topmenu；menu_type=top'
  UNION ALL SELECT 'portal_custom_menu',  '自定义菜单',   '/portal/custommenu',  22, '自建门户菜单（跨类型查看），对标泛微 custommenu'
  UNION ALL SELECT 'portal_menu_style_lib','菜单样式库',  '/portal/menustylelib',23, '门户菜单样式库（menuh/menuv），对标泛微 menustylelib'
  UNION ALL SELECT 'portal_toolbar_menu', '工具栏菜单',   '/portal/toolbar',     24, '门户工具栏「更多」下拉菜单，对标泛微 toolbarmore；menu_type=toolbar'
  UNION ALL SELECT 'portal_account_more', '个人通用设置菜单', '/portal/accountmore', 25, '个人通用设置（字体/布局/更多菜单显隐），对标泛微 accountmore'
) v
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = v.`code` AND m.`is_deleted` = 0);

-- -----------------------------------------------------------------------------
-- 2) 菜单纠偏：早期若以错误大小写/路径建过行，这里对齐为上表的 path
-- -----------------------------------------------------------------------------
UPDATE `blade_menu` SET `path` = '/portal/frontmenu'    WHERE `code` = 'portal_left_menu'      AND `is_deleted` = 0;
UPDATE `blade_menu` SET `path` = '/portal/topmenu'      WHERE `code` = 'portal_top_menu'       AND `is_deleted` = 0;
UPDATE `blade_menu` SET `path` = '/portal/custommenu'   WHERE `code` = 'portal_custom_menu'    AND `is_deleted` = 0;
UPDATE `blade_menu` SET `path` = '/portal/menustylelib' WHERE `code` = 'portal_menu_style_lib' AND `is_deleted` = 0;
UPDATE `blade_menu` SET `path` = '/portal/toolbar'      WHERE `code` = 'portal_toolbar_menu'   AND `is_deleted` = 0;
UPDATE `blade_menu` SET `path` = '/portal/accountmore'  WHERE `code` = 'portal_account_more'   AND `is_deleted` = 0;

-- -----------------------------------------------------------------------------
-- 3) 授权：沿用「门户菜单管理」的授权口径，仅 administrator
--    （菜单维护属管理端能力；普通用户看门户不需要这些页）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_left_menu','portal_top_menu','portal_custom_menu',
                   'portal_menu_style_lib','portal_toolbar_menu','portal_account_more')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- -----------------------------------------------------------------------------
-- 4) 校验输出：六条菜单都应出现，且 parent 挂在「门户」下
-- -----------------------------------------------------------------------------
SELECT m.`code`, m.`name`, m.`path`, m.`sort`, p.`name` AS `parent_name`
  FROM `blade_menu` m
  LEFT JOIN `blade_menu` p ON p.`id` = m.`parent_id`
 WHERE m.`code` IN ('portal_left_menu','portal_top_menu','portal_custom_menu',
                    'portal_menu_style_lib','portal_toolbar_menu','portal_account_more')
   AND m.`is_deleted` = 0
 ORDER BY m.`sort`;