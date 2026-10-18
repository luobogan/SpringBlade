-- =============================================================================
-- 迁移脚本 V2026.10.18_001：门户菜单种子 —— 顶级「流程中心」+ 4 个子菜单
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.18_001
-- 目标库    : blade
-- 影响表    : blade_portal_menu（INSERT 5 行，幂等）
-- 说明      : 在门户自有菜单（blade_portal_menu）里补一组「流程中心」导航，
--             子菜单路由复用系统已有的流程页面（blade_menu 已注册）：
--               新建流程  → /workflow/create
--               待办事宜  → /workflow/todo
--               已办事宜  → /workflow/done
--               我的请求  → /workflow/request
--             顶级「流程中心」只作分组，route_url 留空（点击不跳转，展开子级）。
--             menu_type 默认 front（门户左侧导航，对标泛微 leftmenu）；
--             若要放到门户顶部导航，把下面 @mt 改成 'top' 即可。
-- 图标      : 仅限前端 APP_ICON_MAP 已映射的图标名（cluster/edit/profile/solution/table），
--             未映射的名字会回退成 AppstoreOutlined。
-- 回滚脚本  :
--   DELETE FROM `blade_portal_menu`
--    WHERE `tenant_id` = '000000' AND `menu_type` = 'front' AND `is_system` = 0
--      AND (`menu_name` = '流程中心'
--           OR (`parent_id` = (SELECT `id` FROM `blade_portal_menu`
--                               WHERE `tenant_id` = '000000' AND `menu_type` = 'front'
--                                 AND `menu_name` = '流程中心' AND `is_deleted` = 0)
--               AND `menu_name` IN ('新建流程','待办事宜','已办事宜','我的请求')));
-- =============================================================================

USE `blade`;

-- 可调参数：菜单类型（front 前端左侧 / top 后端顶部）
SET @mt     := 'front';
SET @tenant := '000000';

-- -----------------------------------------------------------------------------
-- 1) 顶级：流程中心（route_url 留空 = 纯分组；sort 10 排在现有 front 种子之后）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_menu`
  (`tenant_id`, `parent_id`, `menu_name`, `icon`, `route_url`, `menu_type`, `link_mode`,
   `sort_order`, `visible`, `is_system`, `is_deleted`, `status`, `create_time`, `update_time`)
SELECT @tenant, 0, '流程中心', 'cluster', NULL, @mt, 1, 10, 1, 0, 0, 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_portal_menu`
   WHERE `tenant_id` = @tenant AND `menu_type` = @mt AND `menu_name` = '流程中心' AND `is_deleted` = 0
);

-- 取顶级 id（已存在则复用，保证脚本可重复执行）
SET @flow_root_id := (
  SELECT `id` FROM `blade_portal_menu`
   WHERE `tenant_id` = @tenant AND `menu_type` = @mt AND `menu_name` = '流程中心' AND `is_deleted` = 0
   LIMIT 1
);

-- -----------------------------------------------------------------------------
-- 2) 子菜单：新建流程 / 待办事宜 / 已办事宜 / 我的请求
--    判重口径：同租户 + 同类型 + 同父级 + 同名 → 重复执行不会插重
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_menu`
  (`tenant_id`, `parent_id`, `menu_name`, `icon`, `route_url`, `menu_type`, `link_mode`,
   `sort_order`, `visible`, `is_system`, `is_deleted`, `status`, `create_time`, `update_time`)
SELECT @tenant, @flow_root_id, v.`menu_name`, v.`icon`, v.`route_url`, @mt, 1,
       v.`sort_order`, 1, 0, 0, 1, NOW(), NOW()
FROM (
        SELECT '新建流程' AS `menu_name`, 'edit'    AS `icon`, '/workflow/create' AS `route_url`, 1 AS `sort_order`
  UNION ALL SELECT '待办事宜',            'profile',                '/workflow/todo',    2
  UNION ALL SELECT '已办事宜',            'solution',               '/workflow/done',    3
  UNION ALL SELECT '我的请求',            'table',                  '/workflow/request', 4
) v
WHERE @flow_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `blade_portal_menu` m
     WHERE m.`tenant_id` = @tenant AND m.`menu_type` = @mt
       AND m.`parent_id` = @flow_root_id AND m.`menu_name` = v.`menu_name` AND m.`is_deleted` = 0
  );

-- -----------------------------------------------------------------------------
-- 3) 校验输出：应看到 1 个顶级 + 4 个子级，parent_id 一致
-- -----------------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`menu_name`, m.`icon`, m.`route_url`, m.`menu_type`, m.`sort_order`
  FROM `blade_portal_menu` m
 WHERE m.`tenant_id` = @tenant AND m.`menu_type` = @mt AND m.`is_deleted` = 0
   AND (m.`menu_name` = '流程中心' OR m.`parent_id` = @flow_root_id)
 ORDER BY m.`parent_id`, m.`sort_order`;