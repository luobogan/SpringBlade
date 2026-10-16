-- =============================================================================
-- 迁移脚本 V2026.10.16_003：快捷搜索管理 + 插件管理（对标 quicksearchmanage / pluginmanage）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_003
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_quick_search / blade_portal_quick_search_share /
--                    blade_portal_plugin；
--             blade_menu（两个管理页 + 按钮 + 权限码）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §14.2 H（quicksearchmanage 快捷搜索管理）
--             §14.2 G（pluginmanage 插件管理：pid/文件路径/是否启用/排序/类型）
--             快捷搜索总开关已落 blade_portal_setting.quick_search_enabled（V2026.10.16_002），
--             本迁移只负责搜索类型表 + 使用限制共享 + 插件登记表。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_quick_search','portal_plugin',
--                        'portal_quick_search_manage','portal_plugin_manage',
--                        'portal_quick_search_maint','portal_plugin_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_quick_search','portal_plugin',
--                        'portal_quick_search_manage','portal_plugin_manage',
--                        'portal_quick_search_maint','portal_plugin_maint');
--   DROP TABLE IF EXISTS `blade_portal_quick_search_share`;
--   DROP TABLE IF EXISTS `blade_portal_quick_search`;
--   DROP TABLE IF EXISTS `blade_portal_plugin`;
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 快捷搜索类型表（对标 quick_search_types §14.2 H）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_quick_search` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `module`       varchar(64)  NOT NULL                COMMENT '所属模块（如 workflow/doc/notice）',
  `type`         varchar(64)  NOT NULL                COMMENT '搜索类型标识（模块内唯一）',
  `custom_name`  varchar(128) NOT NULL                COMMENT '搜索类型显示名',
  `icon`         varchar(255)          DEFAULT NULL   COMMENT '图标（antd 图标名或图片URL）',
  `url`          varchar(512)          DEFAULT NULL   COMMENT '搜索跳转地址（站内路由或外部URL）',
  `link_mode`    int          NOT NULL DEFAULT '1'    COMMENT '链接模式（1站内路由 2外部链接）',
  `is_use`       int          NOT NULL DEFAULT '1'    COMMENT '是否启用（0停用 1启用）',
  `is_sys`       int          NOT NULL DEFAULT '0'    COMMENT '是否系统内置（1内置不可删）',
  `order_num`    int          NOT NULL DEFAULT '0'    COMMENT '排序号（越小越靠前）',
  `remark`       varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`   int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_module_type` (`tenant_id`, `module`, `type`),
  KEY `idx_is_use` (`is_use`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快捷搜索类型（对标泛微 quick_search_types §14.2）';

-- 2) 快捷搜索使用限制（对标 commonshareinfo module=qs §14.3；1全员/2部门/3个人/4角色/5分部）------
CREATE TABLE IF NOT EXISTS `blade_portal_quick_search_share` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `search_id`   bigint      NOT NULL                COMMENT '快捷搜索类型ID（blade_portal_quick_search.id）',
  `share_type`  int         NOT NULL                COMMENT '共享范围（1全员 2部门 3个人 4角色 5分部）',
  `share_id`    bigint               DEFAULT '0'    COMMENT '共享对象ID（部门ID/用户ID/角色ID/分部ID；全员为0）',
  `is_deleted`  int         NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int         NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_id` (`search_id`),
  KEY `idx_share` (`share_type`, `share_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快捷搜索使用限制（对标泛微 commonshareinfo）';

-- 3) 插件登记表（对标 hppluginsetting §14.2 G）-------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_plugin` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `pid`          varchar(128) NOT NULL               COMMENT '插件标识（租户内唯一）',
  `plugin_desc`  varchar(255)          DEFAULT NULL   COMMENT '插件描述',
  `file_path`    varchar(512)          DEFAULT NULL   COMMENT '插件文件路径（JS/CSS 资源地址）',
  `is_use`       int          NOT NULL DEFAULT '1'    COMMENT '是否启用（0停用 1启用）',
  `order_num`    int          NOT NULL DEFAULT '0'    COMMENT '排序号（越小越靠前）',
  `p_type`       varchar(32)  NOT NULL DEFAULT 'js'   COMMENT '插件类型（js脚本/css样式/component组件）',
  `remark`       varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`   int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_pid` (`tenant_id`, `pid`),
  KEY `idx_is_use` (`is_use`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户插件登记（对标泛微 hppluginsetting §14.2）';

-- 4) 种子：常用快捷搜索类型（系统内置不可删）--------------------------------------------
INSERT INTO `blade_portal_quick_search`
  (`tenant_id`,`module`,`type`,`custom_name`,`icon`,`url`,`link_mode`,`is_use`,`is_sys`,`order_num`,`is_deleted`,`status`,`create_time`)
SELECT '000000','workflow','todo','我的待办','solution','/workflow/todo',1,1,1,1,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_quick_search` WHERE `tenant_id`='000000' AND `module`='workflow' AND `type`='todo' AND `is_deleted`=0);

INSERT INTO `blade_portal_quick_search`
  (`tenant_id`,`module`,`type`,`custom_name`,`icon`,`url`,`link_mode`,`is_use`,`is_sys`,`order_num`,`is_deleted`,`status`,`create_time`)
SELECT '000000','notice','notice','通知公告','bell','/portal/noticedetail',1,1,1,2,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_quick_search` WHERE `tenant_id`='000000' AND `module`='notice' AND `type`='notice' AND `is_deleted`=0);

INSERT INTO `blade_portal_quick_search`
  (`tenant_id`,`module`,`type`,`custom_name`,`icon`,`url`,`link_mode`,`is_use`,`is_sys`,`order_num`,`is_deleted`,`status`,`create_time`)
SELECT '000000','doc','doc','文档搜索','file-search','/portal/home',1,1,1,3,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_quick_search` WHERE `tenant_id`='000000' AND `module`='doc' AND `type`='doc' AND `is_deleted`=0);

-- 5) 管理菜单：快捷搜索 / 插件（普通子菜单，两段连写路径）----------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_quick_search', '快捷搜索管理', 'portal_quick_search', '/portal/quicksearch', 'api', 13,
       1, 1, 0, 0,
       '快捷搜索管理（普通子菜单 → 组件 ./pages/Portal/QuickSearch/index.tsx；path 末段须连写 quicksearch）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_quick_search' AND m.`is_deleted` = 0);

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_plugin', '插件管理', 'portal_plugin', '/portal/plugin', 'api', 14,
       1, 1, 0, 0,
       '插件管理（普通子菜单 → 组件 ./pages/Portal/Plugin/index.tsx；path 末段须连写 plugin）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_plugin' AND m.`is_deleted` = 0);

-- 6) 权限按钮 + 权限码 -------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_quick_search_manage' AS `code`, '快捷搜索管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_quick_search_maint',   '快捷搜索维护权限', 2
  UNION ALL SELECT 'portal_plugin_manage',        '插件管理按钮',     3
  UNION ALL SELECT 'portal_plugin_maint',         '插件维护权限',     4
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 7) 授权：仅超级管理员 -------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_quick_search','portal_plugin',
                   'portal_quick_search_manage','portal_plugin_manage',
                   'portal_quick_search_maint','portal_plugin_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 8) 校验输出 ---------------------------------------------------------------------------
SELECT `module`, `type`, `custom_name`, `is_sys` FROM `blade_portal_quick_search` WHERE `is_deleted` = 0 ORDER BY `order_num`;
SELECT COUNT(*) AS `plugin_rows` FROM `blade_portal_plugin` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path`, m.`category` FROM `blade_menu` m
 WHERE m.`code` IN ('portal_quick_search','portal_plugin') AND m.`is_deleted` = 0;
