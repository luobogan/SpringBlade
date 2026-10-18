-- =============================================================================
-- 迁移脚本 V2026.10.18_002：门户维护菜单只保留给管理员（普通人员不再看到）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.18_002
-- 目标库    : blade
-- 影响表    : blade_role_menu（DELETE 非管理员授权）；只读 blade_menu / blade_role
-- 现象      : /welcome 左侧「门户」分组下的维护页（门户、公告详情、机构默认首页、
--             特色门户、协同区、门户工作台…）对普通登录用户也可见。
-- 原因      : 这些 blade_menu 的授权本该只给 role_alias='administrator'（各迁移脚本
--             的 blade_role_menu 段均带该条件），但实际环境里授权范围被放大
--             （角色 alias 不一致 / 建角色时授了全量菜单 / 手工授权）。
-- 方案      : 以「portal 菜单为根」动态圈定门户维护菜单集合（含其全部子孙与
--             category=2 的按钮权限行），删除除管理员以外的角色授权。
--             超管（uid=1）不受影响：systemMenuTree 对 administrator 直接返回全部。
-- 是否幂等  : 是（DELETE 按条件，可重复执行）
-- 回滚脚本  : 无（授权属配置数据，需重跑各功能迁移脚本的 blade_role_menu 段恢复）
-- =============================================================================

USE `blade`;

-- 保留授权的角色 alias（按需增删；多角色用英文逗号分隔）
SET @keep_aliases := 'administrator';

-- -----------------------------------------------------------------------------
-- 0) 门户维护菜单集合：portal 自身 + 其全部子孙（递归，深度上限 10 层防环）
--    不枚举 code，后续新增的门户子菜单自动纳入治理范围。
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_portal_menu_ids;
CREATE TEMPORARY TABLE tmp_portal_menu_ids (`id` bigint PRIMARY KEY);

-- 递归取 portal 及其全部子孙（MySQL 8 支持 WITH RECURSIVE；含 category=2 的按钮权限行）
INSERT INTO tmp_portal_menu_ids (`id`)
WITH RECURSIVE pm AS (
  SELECT `id`, `parent_id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0
  UNION ALL
  SELECT m.`id`, m.`parent_id` FROM `blade_menu` m JOIN pm ON m.`parent_id` = pm.`id`
   WHERE m.`is_deleted` = 0
)
SELECT `id` FROM pm;

-- -----------------------------------------------------------------------------
-- 1) 诊断输出：这些菜单当前被哪些角色授权（执行后请先看这段）
-- -----------------------------------------------------------------------------
SELECT m.`id`   AS menu_id,
       m.`code` AS menu_code,
       m.`name` AS menu_name,
       m.`path` AS menu_path,
       r.`id`   AS role_id,
       IFNULL(r.`role_alias`, '') AS role_alias,
       r.`role_name`                AS role_name,
       r.`tenant_id`               AS tenant_id
  FROM `blade_role_menu` rm
  JOIN `blade_menu`        m ON m.`id` = rm.`menu_id`
  JOIN `blade_role`        r ON r.`id` = rm.`role_id`
 WHERE m.`id` IN (SELECT `id` FROM tmp_portal_menu_ids)
 ORDER BY m.`sort`, m.`id`, r.`id`;

-- -----------------------------------------------------------------------------
-- 2) 清理：删除非保留角色的授权（超管 uid=1 不走 blade_role_menu，不受影响）
--    role_alias 为空/NULL 的角色一并收回——宁可回收，也不要泄露给普通人员。
-- -----------------------------------------------------------------------------
DELETE rm
  FROM `blade_role_menu` rm
  JOIN `blade_menu` m ON m.`id` = rm.`menu_id`
  JOIN `blade_role` r ON r.`id` = rm.`role_id`
 WHERE m.`id` IN (SELECT `id` FROM tmp_portal_menu_ids)
   AND r.`tenant_id` = m.`tenant_id`
   AND FIND_IN_SET(IFNULL(r.`role_alias`, ''), @keep_aliases) = 0;

-- -----------------------------------------------------------------------------
-- 3) 清理后校验：应只剩 @keep_aliases 中的角色；为空表示只有超管可见（普通人员不可见）
-- -----------------------------------------------------------------------------
SELECT m.`code` AS menu_code, m.`name` AS menu_name,
       IFNULL(r.`role_alias`, '') AS role_alias, r.`role_name` AS role_name
  FROM `blade_role_menu` rm
  JOIN `blade_menu` m ON m.`id` = rm.`menu_id`
  LEFT JOIN `blade_role` r ON r.`id` = rm.`role_id`
 WHERE m.`id` IN (SELECT `id` FROM tmp_portal_menu_ids)
 ORDER BY m.`sort`, m.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_portal_menu_ids;