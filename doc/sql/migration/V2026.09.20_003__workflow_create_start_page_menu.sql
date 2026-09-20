-- =============================================================================
-- 迁移脚本 003：「发起流程」页菜单驱动化（新建流程列表 → 点条目新标签打开发起页）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.20_003
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（新增 1 行「按钮型组件菜单」；已存在则对齐 is_open）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按 menu_id+role_id 不存在才插入；is_open 用 <> 2 才更新）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` = 'workflow_create_start';
--   DELETE FROM `blade_menu` WHERE `code` = 'workflow_create_start';
--
-- 背景
--   「新建流程」列表点流程卡片后，要在新标签页打开发起页（填表单 → startInstance），
--   且该标签页不要左侧菜单（无 ProLayout 外壳）。本项目约定「前端路由 100% 由 blade_menu 驱动」，
--   故采用与 Mall/Product、流程设计（workflow_design）相同的「按钮型组件菜单」机制
--   （菜单管理界面：类别=按钮 + 勾选「是否生成组件」），再用 is_open=2 声明为「独立页」。
--
-- 前端解析规则（见 src/app.tsx loopMenuItem / patchClientRoutes）
--   * /menu/buttons 把本行作为父菜单（workflow_create）的子按钮下发 → 前端存 localStorage；
--   * 命中 category===2 && isComponent===1 && path 的行 → 注册路由；
--       - is_open≠2：挂到 '/' 之下（渲染在 ProLayout/左侧菜单内，同 exceldesign/workflowdesign）；
--       - is_open=2：按「独立页」挂到**顶层**且带 layout:false → 不套 ProLayout/左侧菜单
--         （同名顶层静态路由优先，不会被顶掉）。
--   * 组件文件 = ./pages/{path首段Pascal}/{path次段Pascal}/{path末段Pascal}.tsx
--       /workflow/create/start  →  ./pages/Workflow/Create/Start.tsx   ← 文件名必须与此一致
--
-- 授权范围：role_alias='workflow' 的流程管理员角色（与 001 脚本同口径）。
-- =============================================================================

USE `blade`;

-- 1) 发起流程页（按钮型组件菜单 + 独立页 is_open=2，不出现在侧边栏）---------------
SET @p_id = (SELECT `id` FROM `blade_menu` WHERE `code` = 'workflow_create' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       @p_id,
       'workflow_create_start', '发起流程', 'workflow_create_start', '/workflow/create/start', 'api', 2,
       2, 1, 2, 1,
       '发起流程独立页（category=2 + is_component=1 + is_open=2 → 顶层挂载、无 ProLayout 外壳；组件 ./pages/Workflow/Create/Start.tsx）',
       0, '000000', 1
FROM DUAL
WHERE @p_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_create_start' AND `is_deleted` = 0);

-- 1.1) 已存在的行（早期按 is_open=0 建）对齐为独立页 --------------------------------
UPDATE `blade_menu`
   SET `is_open` = 2
 WHERE `code` = 'workflow_create_start' AND `is_deleted` = 0 AND `is_open` <> 2;

-- 2) 授权给「流程管理员」角色（role_alias='workflow'，同租户）---------------------
SET @m_id = (SELECT `id` FROM `blade_menu` WHERE `code` = 'workflow_create_start' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), @m_id, r.`id`
FROM `blade_role` r
WHERE r.`role_alias` = 'workflow' AND r.`is_deleted` = 0
  AND @m_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = @m_id AND rm.`role_id` = r.`id`);

-- 3) 校验输出 ------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`code`, m.`path`, m.`category`, m.`is_open`, m.`is_component`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS granted_roles
FROM `blade_menu` m
WHERE m.`code` = 'workflow_create_start' AND m.`is_deleted` = 0;
