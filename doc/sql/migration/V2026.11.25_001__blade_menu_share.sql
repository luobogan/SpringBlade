-- =============================================================================
-- 迁移脚本 V2026.11.25_001：系统菜单多主体可见范围
-- -----------------------------------------------------------------------------
-- 目标库：blade
-- 说明：
--   为 blade_menu.id 建立独立的多主体共享表。
--   解决“建模模块权限配置人员/部门/岗位/分部/安全级别后，用户仍看不到菜单”的问题。
--   share_type：
--     1 = 角色
--     2 = 人员
--     3 = 部门
--     4 = 分部
--     5 = 岗位
--     6 = 安全级别（showlevel/showlevel2 区间）
--     7 = 全员
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_menu_share` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000',
  `menu_id` bigint NOT NULL,
  `share_type` int NOT NULL,
  `share_id` bigint DEFAULT NULL,
  `include_sub` int NOT NULL DEFAULT 0,
  `showlevel` int DEFAULT NULL,
  `showlevel2` int DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1,
  `is_deleted` int NOT NULL DEFAULT 0,
  `create_user` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_bms_menu` (`tenant_id`, `menu_id`, `is_deleted`),
  KEY `idx_bms_subject` (`tenant_id`, `share_type`, `share_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单多主体可见范围';
