-- =============================================================================
-- 迁移脚本 V2026.10.15_007：门户布局模板库 blade_portal_layout
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_007
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_layout；blade_menu（布局模板库页 + 按钮）、blade_role_menu
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；菜单/授权按 code/id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §12.3.5 / §12.5 P2（portallayout 布局库）
--              把页面级(owner=0)元素排列存为可复用模板，并从模板一键实例化新门户页（复用导入链路）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_layout','portal_layout_manage');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_layout','portal_layout_manage');
--   DROP TABLE IF EXISTS `blade_portal_layout`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_layout` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `layout_name`   varchar(128) NOT NULL                COMMENT '模板名称',
  `layout_type`   varchar(32)           DEFAULT 'cus'  COMMENT '模板类型：sys内置 cus自定义',
  `elements_json` text                 DEFAULT NULL   COMMENT '元素排列JSON（页面级元素快照）',
  `remark`        varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`    int                   DEFAULT '0'    COMMENT '是否删除',
  `status`        int                   DEFAULT '1'    COMMENT '状态',
  `create_user`   bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`   datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`   bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`   datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`layout_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户布局模板库（对标泛微 portallayout）';

-- 管理菜单：布局模板库（普通子菜单，两段连写路径 → ./pages/Portal/LayoutLib/index.tsx）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_layout', '布局模板库', 'portal_layout', '/portal/layoutlib', 'api', 9,
       1, 1, 0, 0,
       '门户布局模板库（普通子菜单 → 组件 ./pages/Portal/LayoutLib/index.tsx；path 末段须连写 layoutlib）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_layout' AND m.`is_deleted` = 0);

-- 权限按钮
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       'portal_layout_manage', '布局模板库按钮', 'portal_layout_manage', '', 'api', 1,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal_layout_manage' AND `is_deleted` = 0);

-- 授权：仅超级管理员
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_layout','portal_layout_manage')
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`);

-- 校验输出 ---------------------------------------------------------------------
SELECT COUNT(*) AS `layout_rows` FROM `blade_portal_layout` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path` FROM `blade_menu` m
 WHERE m.`code` IN ('portal_layout','portal_layout_manage') AND m.`is_deleted` = 0;
