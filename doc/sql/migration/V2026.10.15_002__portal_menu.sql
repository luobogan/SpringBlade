-- =============================================================================
-- 迁移脚本 V2026.10.15_002：门户菜单（portalmenu）—— 建表 + 种子 + 管理页菜单
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_002
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_menu / blade_portal_menu_share；
--             blade_menu（管理页菜单 1 + 权限按钮 2）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §14（门户菜单 portalmenu）
--              门户自有导航菜单（top 顶部 / toolbar 工具栏 / quick 快捷入口），
--              父子层级 + 使用限制共享（对标 commonshareinfo/elementshareinfo），
--              与系统菜单（blade_menu/blade-system 维护）相互独立。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_menu_list','portal_menu_manage','portal_menu_maint');
--   DELETE FROM `blade_menu`
--     WHERE `code` IN ('portal_menu_list','portal_menu_manage','portal_menu_maint');
--   DELETE FROM `blade_portal_menu_share`;
--   DELETE FROM `blade_portal_menu`;
--   DROP TABLE IF EXISTS `blade_portal_menu_share`;
--   DROP TABLE IF EXISTS `blade_portal_menu`;
-- =============================================================================

USE `blade`;

-- 1) 门户菜单表 -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_menu` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `parent_id`    bigint       NOT NULL DEFAULT '0'     COMMENT '父菜单ID（0为顶级）',
  `menu_name`    varchar(128) NOT NULL                COMMENT '菜单名称',
  `icon`         varchar(255)          DEFAULT NULL   COMMENT '图标（antd 图标名或图片URL）',
  `route_url`    varchar(512)          DEFAULT NULL   COMMENT '链接地址（站内路由或外部URL）',
  `menu_type`    varchar(32)  NOT NULL                COMMENT '菜单类型：top顶部/toolbar工具栏/quick快捷入口',
  `link_mode`    int                   DEFAULT '1'    COMMENT '链接模式：1站内路由 2外部链接',
  `sort_order`   int                   DEFAULT '0'    COMMENT '同级排序号（越小越靠前）',
  `visible`      int                   DEFAULT '1'    COMMENT '是否可见（0隐藏 1可见）',
  `is_system`    int                   DEFAULT '0'    COMMENT '是否系统内置（1内置不可删）',
  `config_json`  text                 DEFAULT NULL   COMMENT '扩展配置JSON（预留）',
  `is_deleted`   int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`menu_type`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户菜单（对标泛微 portalmenu）';

-- 2) 门户菜单授权表（使用限制：1全员/2部门/3个人）--------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_menu_share` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)          DEFAULT '000000' COMMENT '租户ID',
  `menu_id`     bigint      NOT NULL                COMMENT '菜单ID（blade_portal_menu.id）',
  `share_type`  int         NOT NULL                COMMENT '共享范围（1全员 2部门 3个人）',
  `share_id`    bigint               DEFAULT '0'    COMMENT '共享对象ID（部门ID或用户ID；全员为0）',
  `is_deleted`  int                  DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                  DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_share` (`share_type`, `share_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户菜单授权（对标 commonshareinfo）';

-- 3) 种子：顶部导航菜单（全员可见，默认不设授权即全员可见）------------------------
INSERT INTO `blade_portal_menu`
  (`tenant_id`,`parent_id`,`menu_name`,`icon`,`route_url`,`menu_type`,`link_mode`,`sort_order`,`visible`,`is_system`,`is_deleted`,`status`,`create_time`)
SELECT '000000',0,'工作台','home','/welcome','top',1,1,1,0,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `menu_type`='top' AND `menu_name`='工作台' AND `is_deleted`=0);

INSERT INTO `blade_portal_menu`
  (`tenant_id`,`parent_id`,`menu_name`,`icon`,`route_url`,`menu_type`,`link_mode`,`sort_order`,`visible`,`is_system`,`is_deleted`,`status`,`create_time`)
SELECT '000000',0,'消息中心','message','/message','top',1,2,1,0,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `menu_type`='top' AND `menu_name`='消息中心' AND `is_deleted`=0);

INSERT INTO `blade_portal_menu`
  (`tenant_id`,`parent_id`,`menu_name`,`icon`,`route_url`,`menu_type`,`link_mode`,`sort_order`,`visible`,`is_system`,`is_deleted`,`status`,`create_time`)
SELECT '000000',0,'我的门户','appstore','/portal/home','top',1,3,1,0,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu` WHERE `menu_type`='top' AND `menu_name`='我的门户' AND `is_deleted`=0);

-- 4) 管理页菜单（普通子菜单，两段连写路径 → ./pages/Portal/MenuManage/index.tsx）----
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_menu_list', '门户菜单管理', 'portal_menu_list', '/portal/menumanage', 'api', 6,
       1, 1, 0, 0,
       '门户菜单管理（普通子菜单 → 组件 ./pages/Portal/MenuManage/index.tsx；path 末段须连写 menumanage）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_menu_list' AND m.`is_deleted` = 0);

-- 5) 权限按钮（category=2）：前端按钮门禁 + 后端权限码 ---------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_menu_manage' AS `code`, '门户菜单管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_menu_maint',   '门户菜单维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员（菜单维护属管理端能力，普通用户无需）--------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_menu_list','portal_menu_manage','portal_menu_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 7) 校验输出 ---------------------------------------------------------------------
SELECT `menu_type`, COUNT(*) AS `cnt` FROM `blade_portal_menu` WHERE `is_deleted` = 0 GROUP BY `menu_type`;
SELECT m.`code`, m.`name`, m.`path`, m.`category`
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_menu_list','portal_menu_manage','portal_menu_maint') AND m.`is_deleted` = 0;
