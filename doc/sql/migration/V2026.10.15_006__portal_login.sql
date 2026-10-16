-- =============================================================================
-- 迁移脚本 V2026.10.15_006：登录前门户（page_type 区分 + 登录页皮肤模板）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_006
-- 目标库    : blade
-- 影响表    : blade_portal_page（ADD page_type）；新建 blade_portal_login_template；
--             blade_menu（登录皮肤管理页 + 按钮）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（information_schema 守卫加列；CREATE TABLE IF NOT EXISTS；菜单/授权按 code/id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §15.2 A1(loginpage) / §16.1(loginportal/SystemLoginTemplate) / §20.4
--              登录前门户 = 登录页皮肤(背景/Logo/登录框/二维码/版权) + 登录前门户页(page_type=0)
-- 说明      : page_type 0=登录前门户 1=登录后门户（默认 1，存量页不受影响）；
--              登录皮肤支持当前启用(is_current) + 定时切换(open_timer+timer_start/end，拉模式)。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_login_template','portal_login_template_manage');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_login_template','portal_login_template_manage');
--   DROP TABLE IF EXISTS `blade_portal_login_template`;
--   ALTER TABLE `blade_portal_page` DROP COLUMN `page_type`;
-- =============================================================================

USE `blade`;

-- 1) blade_portal_page 增加 page_type（0登录前 1登录后，默认1）--------------------
SET @dbname = DATABASE();
SET @tbl = 'blade_portal_page';
SET @col = 'page_type';
SET @s = IF(
  (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = @tbl AND column_name = @col) = 0,
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
         '` int DEFAULT ''1'' COMMENT ''页面类型：0登录前门户 1登录后门户'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 登录页皮肤模板表 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_login_template` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`         varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `template_name`     varchar(128) NOT NULL                COMMENT '模板名称',
  `template_type`     varchar(64)           DEFAULT 'default' COMMENT '模板类型标识',
  `background_config` text                 DEFAULT NULL   COMMENT '背景配置JSON {bgColor,bgImage}',
  `logo_config`       text                 DEFAULT NULL   COMMENT 'Logo配置JSON {logoUrl,title}',
  `form_config`       text                 DEFAULT NULL   COMMENT '登录框配置JSON',
  `qrcode_config`     text                 DEFAULT NULL   COMMENT '二维码配置JSON',
  `copyright_config`  text                 DEFAULT NULL   COMMENT '版权配置JSON',
  `is_current`        int                   DEFAULT '0'    COMMENT '是否当前启用（租户内唯一）',
  `open_timer`        int                   DEFAULT '0'    COMMENT '是否开启定时切换（0否 1是）',
  `timer_start`       varchar(32)           DEFAULT NULL   COMMENT '定时开始 yyyy-MM-dd HH:mm:ss',
  `timer_end`         varchar(32)           DEFAULT NULL   COMMENT '定时结束 yyyy-MM-dd HH:mm:ss',
  `is_deleted`        int                   DEFAULT '0'    COMMENT '是否删除',
  `status`            int                   DEFAULT '1'    COMMENT '状态',
  `create_user`       bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`       datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`       bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`       datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name` (`tenant_id`, `template_name`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录前门户皮肤模板（对标泛微 SystemLoginTemplate）';

-- 3) 种子：默认登录皮肤（当前启用）---------------------------------------------
INSERT INTO `blade_portal_login_template`
  (`tenant_id`,`template_name`,`template_type`,`background_config`,`logo_config`,`copyright_config`,`is_current`,`is_deleted`,`status`,`create_time`)
SELECT '000000','默认登录皮肤','default',
       '{"bgColor":"#f0f2f5","bgImage":""}',
       '{"logoUrl":"","title":"SpringBlade"}',
       '{"text":"Copyright © SpringBlade"}',
       1,0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_login_template` WHERE `tenant_id`='000000' AND `template_name`='默认登录皮肤');

-- 4) 管理菜单：登录页皮肤管理（普通子菜单，两段连写路径 → ./pages/Portal/LoginTemplate/index.tsx）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_login_template', '登录页皮肤', 'portal_login_template', '/portal/logintemplate', 'api', 8,
       1, 1, 0, 0,
       '登录前门户皮肤管理（普通子菜单 → 组件 ./pages/Portal/LoginTemplate/index.tsx；path 末段须连写 logintemplate）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_login_template' AND m.`is_deleted` = 0);

-- 5) 权限按钮（前端门禁）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       'portal_login_template_manage', '登录页皮肤管理按钮', 'portal_login_template_manage', '', 'api', 1,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal_login_template_manage' AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_login_template','portal_login_template_manage')
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`);

-- 校验输出 ---------------------------------------------------------------------
SELECT COUNT(*) AS `login_template_rows` FROM `blade_portal_login_template` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path` FROM `blade_menu` m
 WHERE m.`code` IN ('portal_login_template','portal_login_template_manage') AND m.`is_deleted` = 0;
