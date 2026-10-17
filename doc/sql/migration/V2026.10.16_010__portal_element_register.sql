-- =============================================================================
-- 迁移脚本 V2026.10.16_010：元素注册（elementregister）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_010
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_element_register；blade_menu（元素注册页菜单 1 +
--             权限按钮 2）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §15.3 B5/B6
--             （elementregister 元素注册 + developguide 开发指南）
--             泛微：上传 .zip 元素开发包注册自定义元素（扩展名/唯一名/范围校验），
--             注册后元素定义进 hpBaseElement(elementtype=3 自定义)，运行时经
--             hpBaseElementCustom 的 viewcompname/settingcompname 交 SPA 渲染。
-- 本地化说明（编译型前端的现实约束）:
--             1) SpringBlade 前端为编译期 React，无法像泛微 JSP/SPA 那样运行时注入
--                组件，故本模块定位为「元素开发包注册中心」：上传 zip → 解压读取
--                manifest.json → 校验（命名规范/租户唯一/不与内置 6 类冲突）→ 登记；
--             2) /element-types 聚合返回「内置 provider 类型 ∪ 已注册(启用)自定义类型」，
--                设计器面板仍按 后端已注册 ∩ 前端 ElementRegistry 交集呈现——
--                即自定义元素须前端发版部署对应渲染器后方可设计/渲染，未部署则仅登记
--                （UI 明示该语义，不虚假承诺运行时注入）；
--             3) zip 存档于应用目录 portal/element-register/<租户>/，DB 记路径与大小。
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_element_register','portal_element_register_manage','portal_element_register_maint');
--   DELETE FROM `blade_menu`
--     WHERE `code` IN ('portal_element_register','portal_element_register_manage','portal_element_register_maint');
--   DROP TABLE IF EXISTS `blade_portal_element_register`;
-- =============================================================================

USE `blade`;

-- 1) 元素注册表 -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element_register` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `element_type`  varchar(64)  NOT NULL                COMMENT '元素类型编码（小写字母开头，租户唯一；不与内置6类冲突）',
  `element_name`  varchar(128) NOT NULL                COMMENT '元素名称',
  `version`       varchar(32)           DEFAULT NULL   COMMENT '元素包版本',
  `manifest_json` text                 DEFAULT NULL   COMMENT 'manifest.json 原文',
  `package_path`  varchar(512)          DEFAULT NULL   COMMENT 'zip 包存储路径（应用目录 portal/element-register/<租户>/）',
  `package_size`  bigint                DEFAULT NULL   COMMENT '包大小（字节）',
  `is_deleted`    int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`        int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`   bigint                DEFAULT NULL   COMMENT '创建人（注册人）',
  `create_dept`   bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`   datetime              DEFAULT NULL   COMMENT '注册时间',
  `update_user`   bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`   datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_type` (`tenant_id`, `element_type`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元素注册（对标泛微 elementregister §15.3 B5）';

-- 2) 管理页菜单（普通子菜单，两段连写路径 → ./pages/Portal/ElementRegister/index.tsx）--
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_element_register', '元素注册', 'portal_element_register', '/portal/elementregister', 'api', 19,
       1, 1, 0, 0,
       '元素注册（普通子菜单 → 组件 ./pages/Portal/ElementRegister/index.tsx）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_element_register' AND m.`is_deleted` = 0);

-- 3) 权限按钮（category=2）：前端按钮门禁 + 后端权限码 ---------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_element_register_manage' AS `code`, '元素注册管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_element_register_maint',   '元素注册维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 4) 授权：仅超级管理员 -------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_element_register','portal_element_register_manage','portal_element_register_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 5) 校验输出 ---------------------------------------------------------------------
SELECT `element_type`, `element_name`, `version` FROM `blade_portal_element_register` WHERE `is_deleted` = 0;
