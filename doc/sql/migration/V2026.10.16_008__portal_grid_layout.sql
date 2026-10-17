-- =============================================================================
-- 迁移脚本 V2026.10.16_008：特色门户（门户网格布局 tsportal）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_008
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_grid_layout / blade_portal_grid_layout_share；
--             blade_menu（特色门户页菜单 1 + 权限按钮 2）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §16.2（tsportal 特色门户）
--              泛微前端标签为「特色门户」，后端真实类是 PortalGridLayoutAction，
--              底层表 hp_grid_layout(+hp_grid_layout_element)：按 layout_type 分
--              「登录前门户(login) / 登录后门户(theme)」，支持启用(layout_use)、
--              设为默认入口(layout_default_entrance)、共享、预览。
--              本地化：元素级子表 hp_grid_layout_element 合并为 layout_config JSON 列
--              （元素清单内嵌，查询一次到位）；layout_use / layout_default_entrance
--              保留同名同语义。
-- 命名澄清  : 代码里没有 startdate/enddate 的「定时门户」逻辑（定时切换只属于
--              登录皮肤 SystemLoginTemplate，见 V2026.10.15_006），本模块同样不含定时。
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_ts_portal','portal_ts_portal_manage','portal_ts_portal_maint');
--   DELETE FROM `blade_menu`
--     WHERE `code` IN ('portal_ts_portal','portal_ts_portal_manage','portal_ts_portal_maint');
--   DELETE FROM `blade_portal_grid_layout_share`;
--   DELETE FROM `blade_portal_grid_layout`;
--   DROP TABLE IF EXISTS `blade_portal_grid_layout_share`;
--   DROP TABLE IF EXISTS `blade_portal_grid_layout`;
-- =============================================================================

USE `blade`;

-- 1) 门户网格布局表（对标 hp_grid_layout）------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_grid_layout` (
  `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`              varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `layout_type`            varchar(32)  NOT NULL                COMMENT '布局类型：login登录前门户/theme登录后门户（对标 layout_type）',
  `layout_name`            varchar(128) NOT NULL                COMMENT '布局名称',
  `layout_title`           varchar(255)          DEFAULT NULL   COMMENT '布局标题（展示用）',
  `layout_use`             int                   DEFAULT '0'    COMMENT '是否启用（0否 1是，对标 layout_use）',
  `layout_default_entrance` int                 DEFAULT '0'    COMMENT '是否默认入口（同类型内唯一，对标 layout_default_entrance）',
  `layout_config`          text                 DEFAULT NULL   COMMENT '网格布局配置JSON（五区元素清单，本地化合并 hp_grid_layout_element）',
  `layout_background`      varchar(512)          DEFAULT NULL   COMMENT '背景图/背景色配置',
  `layout_version`         int                   DEFAULT '1'    COMMENT '配置版本号（对标 layout_version）',
  `remark`                 varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`             int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除，软删）',
  `status`                 int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`            bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`            bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`            datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`            bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`            datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`layout_type`),
  KEY `idx_use` (`layout_use`),
  KEY `idx_default` (`layout_default_entrance`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='特色门户-门户网格布局（对标泛微 hp_grid_layout §16.2）';

-- 2) 门户网格布局授权表（使用限制：1全员/2部门/3个人，对标 commonshareinfo）----------
CREATE TABLE IF NOT EXISTS `blade_portal_grid_layout_share` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)          DEFAULT '000000' COMMENT '租户ID',
  `layout_id`   bigint      NOT NULL                COMMENT '布局ID（blade_portal_grid_layout.id）',
  `share_type`  int         NOT NULL                COMMENT '共享范围（1全员 2部门 3个人）',
  `share_id`    bigint               DEFAULT '0'    COMMENT '共享对象ID（部门ID或用户ID；全员为0）',
  `is_deleted`  int                  DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                  DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_layout_id` (`layout_id`),
  KEY `idx_share` (`share_type`, `share_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='特色门户布局授权（对标 commonshareinfo）';

-- 3) 种子：登录后门户默认网格布局（theme 类型，启用 + 默认入口）--------------------
INSERT INTO `blade_portal_grid_layout`
  (`tenant_id`,`layout_type`,`layout_name`,`layout_title`,`layout_use`,`layout_default_entrance`,
   `layout_background`,`layout_version`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','theme','标准三栏','登录后标准门户','1','1','#F5F7FA',1,'默认入口：登录后门户标准网格布局',0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_grid_layout`
                   WHERE `tenant_id`='000000' AND `layout_type`='theme' AND `layout_name`='标准三栏' AND `is_deleted`=0);

-- 4) 管理页菜单（普通子菜单，两段连写路径 → ./pages/Portal/TsPortal/index.tsx）------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_ts_portal', '特色门户', 'portal_ts_portal', '/portal/tsportal', 'api', 17,
       1, 1, 0, 0,
       '特色门户/门户网格布局（普通子菜单 → 组件 ./pages/Portal/TsPortal/index.tsx；path 末段须连写 tsportal）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_ts_portal' AND m.`is_deleted` = 0);

-- 5) 权限按钮（category=2）：前端按钮门禁 + 后端权限码 ---------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_ts_portal_manage' AS `code`, '特色门户管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_ts_portal_maint',   '特色门户维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员 -------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_ts_portal','portal_ts_portal_manage','portal_ts_portal_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 7) 校验输出 ---------------------------------------------------------------------
SELECT `layout_type`, `layout_name`, `layout_use`, `layout_default_entrance`
  FROM `blade_portal_grid_layout` WHERE `is_deleted` = 0;
