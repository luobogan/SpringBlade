-- =============================================================================
-- 迁移脚本 V2026.10.15_008：独立元素库 blade_portal_element_lib + 元素 lib_id
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_008
-- 目标库    : blade
-- 影响表    : blade_portal_element（ADD lib_id）；新建 blade_portal_element_lib；
--             blade_menu（元素库页 + 按钮）、blade_role_menu
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（information_schema 守卫加列；CREATE TABLE IF NOT EXISTS；菜单/授权按 code/id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §12.5 P3（hpBaseElement / HPELEMENTTEMPLATE）
--              独立元素库：元素定义入库可复用；页面元素记 lib_id 引用；
--              封存(status=0)后不可新引，既有引用继续有效；被引用时禁止删除。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_element_lib','portal_element_lib_manage');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_element_lib','portal_element_lib_manage');
--   DROP TABLE IF EXISTS `blade_portal_element_lib`;
--   ALTER TABLE `blade_portal_element` DROP COLUMN `lib_id`;
-- =============================================================================

USE `blade`;

-- 1) blade_portal_element 增加 lib_id（来源元素库ID，可空）-----------------------
SET @dbname = DATABASE();
SET @tbl = 'blade_portal_element';
SET @col = 'lib_id';
SET @s = IF(
  (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = @tbl AND column_name = @col) = 0,
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
         '` bigint DEFAULT NULL COMMENT ''来源元素库ID（blade_portal_element_lib.id）'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 独立元素库表 ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element_lib` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `element_type`  varchar(64)  NOT NULL                COMMENT '元素类型',
  `element_name`  varchar(128) NOT NULL                COMMENT '元素名称',
  `config_json`   text                  DEFAULT NULL   COMMENT '元素私有配置JSON',
  `style_json`    text                  DEFAULT NULL   COMMENT '元素样式配置JSON',
  `remark`        varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`    int                   DEFAULT '0'    COMMENT '是否删除',
  `status`        int                   DEFAULT '1'    COMMENT '状态（1正常 0封存 -1已删除）',
  `create_user`   bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`   datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`   bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`   datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`element_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户独立元素库（对标 hpBaseElement）';

-- 3) 种子：常用库元素 -----------------------------------------------------------
INSERT INTO `blade_portal_element_lib`
  (`tenant_id`,`element_type`,`element_name`,`config_json`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','links','常用快捷入口',
       '{"links":[{"name":"新建流程","icon":"file-add","url":"/workflow/create"},{"name":"我的待办","icon":"solution","url":"/workflow/todo"}]}',
       '常用系统入口宫格',0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_lib` WHERE `tenant_id`='000000' AND `element_name`='常用快捷入口' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_lib`
  (`tenant_id`,`element_type`,`element_name`,`config_json`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','rich','公司公告模板',
       '{"html":"<h3>公司公告</h3><p>请在此处编辑公告内容…</p>"}',
       '富文本公告模板',0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_lib` WHERE `tenant_id`='000000' AND `element_name`='公司公告模板' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_lib`
  (`tenant_id`,`element_type`,`element_name`,`config_json`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','notice','通知公告','{"perPage":8}',
       '系统通知公告',0,1,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_lib` WHERE `tenant_id`='000000' AND `element_name`='通知公告' AND `is_deleted`=0);

-- 4) 管理菜单：元素库（普通子菜单，两段连写路径 → ./pages/Portal/ElementLib/index.tsx）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_element_lib', '门户元素库', 'portal_element_lib', '/portal/elementlib', 'api', 10,
       1, 1, 0, 0,
       '门户独立元素库（普通子菜单 → 组件 ./pages/Portal/ElementLib/index.tsx；path 末段须连写 elementlib）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_element_lib' AND m.`is_deleted` = 0);

-- 5) 权限按钮
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       'portal_element_lib_manage', '门户元素库按钮', 'portal_element_lib_manage', '', 'api', 1,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'portal_element_lib_manage' AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_element_lib','portal_element_lib_manage')
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`);

-- 校验输出 ---------------------------------------------------------------------
SELECT COUNT(*) AS `element_lib_rows` FROM `blade_portal_element_lib` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path` FROM `blade_menu` m
 WHERE m.`code` IN ('portal_element_lib','portal_element_lib_manage') AND m.`is_deleted` = 0;
