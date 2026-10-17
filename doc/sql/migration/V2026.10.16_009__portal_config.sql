-- =============================================================================
-- 迁移脚本 V2026.10.16_009：门户配置项管理（portalconfig）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_009
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_config；blade_menu（配置项管理页菜单 1 + 权限按钮 2）、
--             blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §16.6（portalconfig 配置项）
--              泛微 portalconfig 表（ccode/vvalue/name/note）+ PortalConfigUtil
--              （get/set/refresh + StaticObj 进程内缓存）；管理端列出配置项并可编辑取值。
-- 本地化说明:
--              1) 租户维度（tenant_id + ccode 唯一），对标泛微全局单例；
--              2) 进程内缓存用 ConcurrentHashMap + TTL（不引第三方缓存库），
--                 set 后 refresh 即时失效；
--              3) 种子配置项均有真实代码消费点（非死配置）：
--                 - isOpenMenuCache        菜单树缓存开关（PortalMenuServiceImpl，默认0关）
--                 - canPageMaintainerSettingInViewpage  页面维护者前台编辑总开关
--                                              （Home 编辑布局按钮门禁，默认1开）
--                 - portalStatEnabled      访问统计(PV/UV)开关（recordView，默认1开）
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_config','portal_config_manage','portal_config_maint');
--   DELETE FROM `blade_menu`
--     WHERE `code` IN ('portal_config','portal_config_manage','portal_config_maint');
--   DROP TABLE IF EXISTS `blade_portal_config`;
-- =============================================================================

USE `blade`;

-- 1) 门户配置项表（对标 portalconfig）----------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_config` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `ccode`       varchar(128) NOT NULL                COMMENT '配置项编码（租户内唯一）',
  `vvalue`      varchar(512) NOT NULL                COMMENT '配置项取值',
  `name`        varchar(255)          DEFAULT NULL   COMMENT '配置项名称（展示）',
  `note`        varchar(512)          DEFAULT NULL   COMMENT '配置项说明（展示）',
  `is_deleted`  int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_ccode` (`tenant_id`, `ccode`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户配置项（对标泛微 portalconfig §16.6）';

-- 2) 种子配置项（均有真实消费点）--------------------------------------------------
INSERT INTO `blade_portal_config`
  (`tenant_id`,`ccode`,`vvalue`,`name`,`note`,`is_deleted`,`status`,`create_time`)
SELECT '000000','isOpenMenuCache','0','菜单缓存开关',
       '是否开启门户菜单树进程内缓存（0关 1开）；开启后菜单授权变更需等待缓存 TTL 或重新保存菜单才生效',
       0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_config` WHERE `tenant_id`='000000' AND `ccode`='isOpenMenuCache');

INSERT INTO `blade_portal_config`
  (`tenant_id`,`ccode`,`vvalue`,`name`,`note`,`is_deleted`,`status`,`create_time`)
SELECT '000000','canPageMaintainerSettingInViewpage','1','页面维护者前台编辑总开关',
       '是否允许页面维护者在门户查看页直接进入「编辑布局」（0禁止 1允许）；按钮权限 portal_page_manage 仍前置校验',
       0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_config` WHERE `tenant_id`='000000' AND `ccode`='canPageMaintainerSettingInViewpage');

INSERT INTO `blade_portal_config`
  (`tenant_id`,`ccode`,`vvalue`,`name`,`note`,`is_deleted`,`status`,`create_time`)
SELECT '000000','portalStatEnabled','1','访问统计开关',
       '是否记录门户访问统计（PV/UV/热度，0关 1开）；关闭后不再写入 blade_portal_page_stat/visitor',
       0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_config` WHERE `tenant_id`='000000' AND `ccode`='portalStatEnabled');

-- 3) 管理页菜单（普通子菜单，两段连写路径 → ./pages/Portal/Config/index.tsx）--------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_config', '门户配置项', 'portal_config', '/portal/config', 'api', 18,
       1, 1, 0, 0,
       '门户配置项管理（普通子菜单 → 组件 ./pages/Portal/Config/index.tsx）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_config' AND m.`is_deleted` = 0);

-- 4) 权限按钮（category=2）：前端按钮门禁 + 后端权限码 ---------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_config_manage' AS `code`, '门户配置项管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_config_maint',   '门户配置项维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 5) 授权：仅超级管理员 -------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_config','portal_config_manage','portal_config_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 6) 校验输出 ---------------------------------------------------------------------
SELECT `ccode`, `vvalue`, `name` FROM `blade_portal_config` WHERE `is_deleted` = 0 ORDER BY `id`;
