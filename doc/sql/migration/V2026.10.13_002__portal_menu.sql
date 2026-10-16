-- =============================================================================
-- 迁移脚本 002：配置驱动门户 —— 菜单驱动化（查看页 /portal + 设计器 /portal/design）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_002
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（父菜单 1 + 设计器组件页 1 + 权限按钮 2）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按 menu_id+role_id 不存在才插入；is_open 用 <> 2 才更新）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('portal','portal_design','portal_page_manage','portal_element_manage');
--   DELETE FROM `blade_menu`
--    WHERE `code` IN ('portal','portal_design','portal_page_manage','portal_element_manage');
--
-- 背景
--   本项目约定「前端路由 100% 由 blade_menu 驱动」（见 config/routes.ts 注释与 src/app.tsx
--   loopMenuItem / patchClientRoutes），故门户查看页与设计器均**不**改 config/routes.ts，
--   改由本脚本登记菜单行。
--
-- 前端解析规则（src/app.tsx）
--   * 单段顶级路径（如 /message → ./pages/Message/index.tsx）走 pathParts.length === 1 分支：
--       候选 = ./pages/{Module}/index.tsx、./pages/{Module}/{Module}.tsx
--       ⚠️ 但**按钮型组件页的收集只发生在 pathParts.length >= 2 的分支里**！
--          若门户查看页用单段路径 /portal，其下的 /portal/design（按钮型组件页）
--          永远不会注册路由 → 直接 404。故查看页必须用**两段路径** /portal/home，
--          对应磁盘 src/pages/Portal/Home/index.tsx。
--   * 按钮型组件页（category=2 && is_component=1 && path）注册路由，is_open=2 为「独立页」，
--       顶层挂载且 layout:false（不套 ProLayout/左侧菜单）。
--       ⚠️ 但按钮型路由依赖 /menu/buttons 预加载，启动早期 token 未就绪会 401 → 整批缺失 → 404，
--          故本脚本的设计器**不用按钮型**，改为普通子菜单（category=1/is_open=0）。
--   * buildComponentLoaders 候选顺序（module=Portal, page=Design, rawPage=design, midDirs=[]）：
--       ① ./pages/Portal/design/design.tsx
--       ② ./pages/Portal/design/design/index.tsx
--       ③ ./pages/Portal/Design/Design.tsx      ← **本脚本按此命名落盘**
--       ④ ./pages/Portal/Design/Design/index.tsx
--     ⚠️ 注意：`./pages/Portal/Design/index.tsx` **不在候选内**，故设计器主文件必须命名为
--        Design.tsx（放在 src/pages/Portal/Design/ 目录下）。
--   * 无 path 的行（category=2 权限按钮）不注册路由，仅供前端按钮 code 门禁。
--
-- 授权范围
--   administrator（超级管理员）：查看页 + 设计器 + 两个管理按钮
--   user（用户）              ：仅查看页
-- =============================================================================

USE `blade`;

-- 1) 门户查看页（顶级菜单，两段路径 → ./pages/Portal/Home/index.tsx）----------------
--    必须两段：app.tsx 仅在 pathParts.length >= 2 的分支里收集按钮型组件页，
--    单段 /portal 会导致其下的 /portal/design（设计器）无法注册路由。
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       0,
       'portal', '门户', 'portal', '/portal/home', 'api', 5,
       1, 1, 0, 0,
       '配置驱动门户查看页（两段路径 → 组件 ./pages/Portal/Home/index.tsx；必须两段，否则其下按钮型组件页不注册）',
       0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0);

-- 1.1) 已存在的行（早期按单段 /portal 建）对齐为两段路径 ------------------------------
UPDATE `blade_menu`
   SET `path` = '/portal/home'
 WHERE `code` = 'portal' AND `is_deleted` = 0 AND `path` <> '/portal/home';

-- 2) 门户设计器（按钮型组件菜单 + 独立页 is_open=2，全屏无 ProLayout 外壳）----------
--    组件落盘：src/pages/Portal/Design/Design.tsx（见文件头「buildComponentLoaders」说明）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       'portal_design', '门户设计器', 'portal_design', '/portal/design', 'api', 2,
       1, 1, 0, 0,
       '门户设计器（普通子菜单 → 组件 ./pages/Portal/Design/Design.tsx；不用按钮型是为了规避按钮数据预加载失败导致 404）',
       0, '000000', 1
       FROM DUAL
       WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal_design' AND `is_deleted` = 0);

       -- 2.1) 历史遗留的按钮型配置（category=2 / is_component=1 / is_open=2）→ 对齐为普通子菜单 -----
       --     按钮型页面的路由依赖 /menu/buttons，启动早期 token 未就绪时会 401 → 按钮型路由整批缺失
       --     → 直接输 URL 刷新会概率性 404（本项目既有问题，也影响流程发起/审批、Excel 设计等）。
       --     改用普通子菜单后路由由 /menu/routes 决定（应用内已有重试），稳定可预期。
       UPDATE `blade_menu`
       SET `category` = 1,
        `action` = 1,
        `is_open` = 0,
        `is_component` = 0,
        `remark` = '门户设计器（已由按钮型组件页改为普通子菜单，规避按钮数据预加载失败导致 404）'
       WHERE `code` = 'portal_design' AND `is_deleted` = 0;

-- 2.2) 门户页面管理页（普通子菜单，两段路径 → ./pages/Portal/PageManage/index.tsx）---
--     ⚠️ path 末段必须「连写」：toPascalCase 只按 commonWords 词典还原连写复合词
--        （pagemanage → PageManage ✓），带连字符会得到 Page-Manage（目录不存在 → 组件导入失败 → 404）。
--        既有约定同样如此：/formmode/formmanage → FormManage、/formmode/fieldmanage → FieldManage。
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       p.`id`,
       'portal_page_list', '门户页面管理', 'portal_page_list', '/portal/pagemanage', 'api', 3,
       1, 1, 0, 0,
       '门户页面管理（普通子菜单 → 组件 ./pages/Portal/PageManage/index.tsx；path 末段须连写）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_page_list' AND m.`is_deleted` = 0);

-- 2.3) 已存在的旧路径（早期误用连字符 /portal/page-manage）→ 对齐为连写 -----------------
UPDATE `blade_menu` SET `path` = '/portal/pagemanage'
 WHERE `code` = 'portal_page_list' AND `is_deleted` = 0 AND `path` <> '/portal/pagemanage';

-- 2.4) 公告详情页（普通子菜单，两段**连写**路径 → ./pages/Portal/NoticeDetail/index.tsx）--
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       p.`id`,
       'portal_notice_detail', '公告详情', 'portal_notice_detail', '/portal/noticedetail', 'api', 4,
       1, 1, 0, 0,
       '公告详情页（由门户「通知公告」元素点击标题进入；组件 ./pages/Portal/NoticeDetail/index.tsx）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_notice_detail' AND m.`is_deleted` = 0);

-- 3) 权限按钮（category=2，is_component=0，path 留空，供前端按钮 code 门禁）--------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_page_manage'    AS `code`, '门户页面管理' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_element_manage', '门户元素管理', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 4) 授权：超级管理员拿全部；普通用户仅查看页 -------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND (
        (r.`role_alias` = 'administrator'
         AND m.`code` IN ('portal','portal_design','portal_page_list','portal_notice_detail','portal_page_manage','portal_element_manage'))
     OR (r.`role_alias` = 'user'
         AND m.`code` IN ('portal', 'portal_notice_detail'))
      )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 5) 校验输出 ---------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`code`, m.`name`, m.`path`, m.`category`, m.`is_open`, m.`is_component`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS granted_roles
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal','portal_design','portal_page_manage','portal_element_manage')
   AND m.`is_deleted` = 0
 ORDER BY m.`category`, m.`sort`;