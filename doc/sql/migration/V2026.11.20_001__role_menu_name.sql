-- =============================================================================
-- 迁移脚本 V2026.11.20_001：角色级菜单显示名（不同角色看到不同菜单名）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.20_001
-- 目标库    : blade
-- 影响表    : blade_role_menu_name（新表）
-- 变更类型  : DDL + 数据
-- 是否幂等  : 是
-- 是否丢数据: 否
--
-- 背景
--   blade_menu.name 是全局单一名称，所有角色看到同一个菜单名。
--   本表让管理员为「角色 + 菜单」配置专属显示名：/menu/routes 生成时
--   按当前角色覆盖 name，前端直接用后端返回的 name 渲染，无需改前端。
--   未配置别名的角色仍用 blade_menu.name。
--
-- 回滚脚本  : DROP TABLE IF EXISTS blade_role_menu_name;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_role_menu_name` (
  `id`         BIGINT       NOT NULL COMMENT '主键',
  `role_id`    BIGINT       NOT NULL COMMENT '角色ID',
  `menu_id`    BIGINT       NOT NULL COMMENT '菜单ID',
  `name_override` VARCHAR(255) NOT NULL COMMENT '该角色下显示的菜单名',
  `tenant_id`  VARCHAR(12)  DEFAULT '000000' COMMENT '租户ID',
  `create_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`tenant_id`, `role_id`, `menu_id`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色级菜单显示名覆盖';
