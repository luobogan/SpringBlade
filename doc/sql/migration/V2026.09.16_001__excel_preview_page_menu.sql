-- -----------------------------------------------------------------------------
-- 迁移脚本：Excel 表单预览页改为「菜单驱动路由」
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.16_001
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（新增一行）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按「未授权过」才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` = 'excel_preview_page';
--   DELETE FROM `blade_menu` WHERE `code` = 'excel_preview_page';
--
-- 背景
--   原来 ExcelPreviewPage 是写死在 ant-design-pro/config/routes.ts 里的静态路由
--   （layout: false 的独立页），不符合本项目「前端路由 100% 由 blade_menu 驱动」的约定。
--   本次把它迁到菜单表，前端静态路由已删除。
--
-- 前端解析规则（见 src/app.tsx loopMenuItem，与既有 ExcelDesign/WorkflowDesign 一致）
--   * 组件路径 = ./pages/{Module}/{目录}/{组件}.tsx，其中：
--       Module = 路径第 1 段 PascalCase（formmode → FormMode）
--       目录   = 路径第 2 段 PascalCase（exceldesign → ExcelDesign）
--       组件   = 路径末段
--   * path=/formmode/exceldesign/ExcelPreviewPage
--       → ./pages/FormMode/ExcelDesign/ExcelPreviewPage.tsx  ✔
--   * 末段写成 PascalCase（而非全小写）是刻意为之：全小写末段会先被
--     Func.formatRoutePath 小写化，再被 toPascalCase 的「常用词回替」拆错
--     （excelpreviewpage → ExcelpreViewpage，preview 里的 view 被当成独立单词）；
--     app.tsx 的 pickLastSegment 对「末段本身含大写」的 path 会原样使用。
--
-- 字段含义
--   category     = 2 ：按钮/组件型菜单（不在侧边栏显示，仅作路由与权限载体）
--   is_component = 1 ：生成组件路由（前端据此注册路由）
--   is_open      = 2 ：字段本义即「是否打开新页面」。本行的语义扩展为「独立页」——
--                      app.tsx 的 collectStandalonePaths 判定：
--                      is_component=1 且 is_open=2 的路由会挂到**顶层**，
--                      不套 ProLayout/菜单外壳，等价于原先的 layout: false。
--                      （库里已有的 is_open=2 记录均为 is_component=0 的权限码或外链，
--                        不会被误判为独立页。）
--   tenant_id    ：跟随父菜单，保证多租户一致。
--
-- 授权范围：授予所有「已拥有父菜单 ExcelDesign（code='exceldesign'）」的角色，
--   即能看到设计器的人就能打开预览页；避免角色漏授权导致新标签页报 404。
--
-- ⚠️ 执行顺序：执行本脚本后，前端需重新构建/热更（app.tsx 已同步改造）。
-- =============================================================================

USE `blade`;

-- 父菜单：Excel 表单设计器（code='exceldesign'，即 /formmode/exceldesign/exceldesign）
SET @p_id = (
  SELECT `id` FROM `blade_menu`
  WHERE `code` = 'exceldesign' AND `is_deleted` = 0
  LIMIT 1
);

-- 1) 新增预览页菜单行（幂等）-------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT
  UUID_SHORT(),                                     -- id
  @p_id,                                            -- parent_id：ExcelDesign 设计器菜单
  'excel_preview_page',                             -- code
  '表单预览',                                        -- name
  'excel_preview_page',                             -- alias
  '/formmode/exceldesign/ExcelPreviewPage',         -- path（末段 PascalCase，勿改小写）
  '',                                               -- source
  2,                                                -- sort
  2,                                                -- category：组件型菜单（不进侧边栏）
  1,                                                -- action
  2,                                                -- is_open：独立页（不套布局）
  1,                                                -- is_component：生成路由
  'Excel 表单预览独立页（菜单驱动路由；is_open=2 = 不套 ProLayout）',
  0,                                                -- is_deleted
  (SELECT `tenant_id` FROM `blade_menu` WHERE `id` = @p_id),  -- tenant_id 跟随父菜单
  1                                                 -- status
FROM DUAL
WHERE @p_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `blade_menu` x
    WHERE x.`code` = 'excel_preview_page' AND x.`is_deleted` = 0
  );

-- 新菜单主键（供授权步骤使用）
SET @m_id = (
  SELECT `id` FROM `blade_menu`
  WHERE `code` = 'excel_preview_page' AND `is_deleted` = 0
  LIMIT 1
);

-- 2) 授权：所有已拥有父菜单的角色（幂等）-------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT
  UUID_SHORT(),
  @m_id,
  rm.`role_id`
FROM `blade_role_menu` rm
WHERE rm.`menu_id` = @p_id
  AND @m_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` x
    WHERE x.`menu_id` = @m_id AND x.`role_id` = rm.`role_id`
  );

-- 3) 校验（执行后应返回 1 行）-------------------------------------------------
SELECT
  m.`id`, m.`parent_id`, m.`code`, m.`path`, m.`category`, m.`is_open`, m.`is_component`, m.`tenant_id`
FROM `blade_menu` m
WHERE m.`code` = 'excel_preview_page' AND m.`is_deleted` = 0;
