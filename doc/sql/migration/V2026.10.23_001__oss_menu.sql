-- =============================================================================
-- 迁移脚本：对象存储菜单（系统管理下）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.23_001
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（新增「对象存储」页面菜单）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按 menu_id+role_id 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` = 'resource_oss';
--   DELETE FROM `blade_menu` WHERE `code` = 'resource_oss';
--
-- 背景
--   新增「对象存储」菜单，放在系统管理分组下。前端路由 100% 由 blade_menu 驱动：
--     category=1 + 有 path 的行 → 注册页面路由（渲染在 ProLayout/左侧菜单内）
--     组件文件 = ./pages/{path首段Pascal}/{path次段Pascal}/index.tsx
--       /resource/oss  →  ./pages/Resource/Oss/index.tsx
--
-- 后端接口：blade-resource 的 OssEndpoint（/api/blade-resource/oss/endpoint/*）
--
-- 默认存储行为（重要）
--   路径 /resource/oss 默认指向「本地存储」（blade_oss.category = 'local'）：
--   未显式启用任何第三方 OSS 配置时，该路径下的上传与文件读写一律走本地存储
--   （本地目录由 image_dir / file_dir 等字段指定）。
--   仅当在本页明确「启用」（status = 1）了一条第三方 OSS 配置（如七牛云 category='qiniu'）时，
--   才切换为该配置所对应的第三方 OSS 存储；同一租户仅一条配置生效，
--   停用或删除该第三方配置后，自动回退为默认的本地存储。
-- =============================================================================

USE `blade`;

-- 1) 找到「系统管理」父菜单 id（兼容中文名或 i18n key 两种存法）-------------------
SET @sys_id = (
  SELECT `id` FROM `blade_menu`
  WHERE `is_deleted` = 0
    AND (`name` = '系统管理' OR `name` = 'menu.系统管理')
  LIMIT 1
);

-- 2) 新增「对象存储」页面菜单-----------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       @sys_id,
       'resource_oss', 'menu.系统管理.对象存储', 'resource_oss', '/resource/oss', 'cloud', 50,
       1, 0, 1, 0,
       '对象存储管理页（菜单驱动路由；组件 ./pages/Resource/Oss/index.tsx）：配置 CRUD + 启用切换。默认指向本地存储（category=local）；仅当明确启用(status=1)第三方 OSS 配置（如七牛云 category=qiniu）时，才切换为该第三方 OSS 存储，停用/删除后回退本地存储',
       0, '000000', 1
FROM DUAL
WHERE @sys_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'resource_oss' AND `is_deleted` = 0);

-- 3) 授权给「超级管理员」角色-----------------------------------------------------
SET @m_oss = (SELECT `id` FROM `blade_menu` WHERE `code` = 'resource_oss' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), @m_oss, r.`id`
FROM `blade_role` r
WHERE r.`role_alias` = 'administrator' AND r.`is_deleted` = 0
  AND @m_oss IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = @m_oss AND rm.`role_id` = r.`id`);

-- 4) 校验输出 ------------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`code`, m.`path`, m.`category`, m.`is_open`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS granted_roles
FROM `blade_menu` m
WHERE m.`code` = 'resource_oss' AND m.`is_deleted` = 0;
