-- =============================================================================
-- 迁移脚本 V2026.10.16_004：样式库三件 + 元素模板库（对标 menustylelib / elementstylelib /
--                          headlinestyle / elementtemplate）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_004
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_menu_style / blade_portal_element_style /
--                    blade_portal_element_template；
--             blade_menu（样式库管理页 + 按钮 + 权限码）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §14.2 E（menustylelib 菜单样式库 menuh/menuv）
--             §15.3 B3（elementstylelib 元素样式库，泛微复用 hpMenuStyle menustyletype='element'）
--             §15.3 B4（headlinestyle 头条样式库）——本方案以 style_kind 列区分 element/headline
--             §15.3 B2（elementtemplate 元素模板库 HPELEMENTTEMPLATE：另存为模板/预览/批量删除）
-- 说明      : 泛微把菜单样式与元素样式合放 hpMenuStyle（menustyletype 区分）；本方案按
--             「菜单样式 / 元素样式(含头条) / 元素模板」分三表，语义更清晰，与前端三个页签对应。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_style_lib','portal_style_lib_manage','portal_style_lib_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_style_lib','portal_style_lib_manage','portal_style_lib_maint');
--   DROP TABLE IF EXISTS `blade_portal_element_template`;
--   DROP TABLE IF EXISTS `blade_portal_element_style`;
--   DROP TABLE IF EXISTS `blade_portal_menu_style`;
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 菜单样式库（对标 hpMenuStyle，menustyletype=menuh/menuv §14.2 E）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_menu_style` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `style_name`     varchar(128) NOT NULL                COMMENT '样式名称',
  `style_desc`     varchar(255)          DEFAULT NULL   COMMENT '样式描述',
  `style_type`     varchar(32)  NOT NULL DEFAULT 'menuv' COMMENT '样式类型（menuh横向 menuv纵向）',
  `style_config`   text                  DEFAULT NULL   COMMENT '样式配置JSON（配色/字号/图标位置等）',
  `cite_count`     int          NOT NULL DEFAULT '0'    COMMENT '被引用次数（引用追踪）',
  `remark`         varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`     int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`         int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`    bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`    bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`    datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`    bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`    datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name_type` (`tenant_id`, `style_name`, `style_type`),
  KEY `idx_type` (`style_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户菜单样式库（对标泛微 hpMenuStyle menuh/menuv §14.2）';

-- -----------------------------------------------------------------------------
-- 2) 元素样式库（style_kind 区分 元素样式/头条样式，对标 §15.3 B3/B4）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element_style` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `style_kind`     varchar(32)  NOT NULL DEFAULT 'element' COMMENT '样式分类（element元素样式 headline头条样式）',
  `style_name`     varchar(128) NOT NULL                COMMENT '样式名称',
  `style_desc`     varchar(255)          DEFAULT NULL   COMMENT '样式描述',
  `style_config`   text                  DEFAULT NULL   COMMENT '样式配置JSON（卡片/网格/头条布局参数）',
  `cite_count`     int          NOT NULL DEFAULT '0'    COMMENT '被引用次数（引用追踪）',
  `remark`         varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`     int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`         int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`    bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`    bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`    datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`    bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`    datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_kind_name` (`tenant_id`, `style_kind`, `style_name`),
  KEY `idx_kind` (`style_kind`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户元素样式库+头条样式库（对标泛微 §15.3 B3/B4）';

-- -----------------------------------------------------------------------------
-- 3) 元素模板库（对标 HPELEMENTTEMPLATE §15.3 B2：另存为模板/预览/批量删除）
--    与 blade_portal_element_lib（独立元素库）的区别：lib 是"可复用元素定义"，
--    template 是"某个元素配置的快照模板"，实例化时复制配置生成页面元素。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element_template` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `element_type`   varchar(64)  NOT NULL                COMMENT '元素类型（todo/notice/message/chart/links/rich）',
  `template_name`  varchar(128) NOT NULL                COMMENT '模板名称',
  `config_json`    text                  DEFAULT NULL   COMMENT '元素私有配置JSON（模板快照）',
  `style_json`     text                  DEFAULT NULL   COMMENT '元素样式配置JSON（模板快照）',
  `remark`         varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`     int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`         int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`    bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`    bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`    datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`    bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`    datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name` (`tenant_id`, `template_name`),
  KEY `idx_type` (`element_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户元素模板库（对标泛微 HPELEMENTTEMPLATE §15.3）';

-- -----------------------------------------------------------------------------
-- 4) 种子：默认样式与模板
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_menu_style`
  (`tenant_id`,`style_name`,`style_desc`,`style_type`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','经典纵向','一级图标+文字，二级缩进','menuv',
       '{"colorPrimary":"#1677ff","fontSize":14,"iconPosition":"left","indent":16}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu_style` WHERE `tenant_id`='000000' AND `style_name`='经典纵向' AND `is_deleted`=0);

INSERT INTO `blade_portal_menu_style`
  (`tenant_id`,`style_name`,`style_desc`,`style_type`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','简约横向','顶部横向排列，悬停高亮','menuh',
       '{"colorPrimary":"#1677ff","fontSize":14,"hoverBg":"#f0f5ff"}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_menu_style` WHERE `tenant_id`='000000' AND `style_name`='简约横向' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_style`
  (`tenant_id`,`style_kind`,`style_name`,`style_desc`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','element','标准卡片','白底圆角卡片，标题栏灰底','{"radius":8,"padding":16,"titleBg":"#fafafa"}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_style` WHERE `tenant_id`='000000' AND `style_kind`='element' AND `style_name`='标准卡片' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_style`
  (`tenant_id`,`style_kind`,`style_name`,`style_desc`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','element','网格宫格','无边框网格，适合快捷入口','{"radius":4,"padding":8,"grid":true}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_style` WHERE `tenant_id`='000000' AND `style_kind`='element' AND `style_name`='网格宫格' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_style`
  (`tenant_id`,`style_kind`,`style_name`,`style_desc`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','headline','左侧大图','头条列表左侧配图，标题加粗','{"layout":"left-image","titleWeight":600,"imageWidth":120}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_style` WHERE `tenant_id`='000000' AND `style_kind`='headline' AND `style_name`='左侧大图' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_style`
  (`tenant_id`,`style_kind`,`style_name`,`style_desc`,`style_config`,`is_deleted`,`status`,`create_time`)
SELECT '000000','headline','顶部通栏','头条通栏大图，标题叠加','{"layout":"top-banner","titleOverlay":true}',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_style` WHERE `tenant_id`='000000' AND `style_kind`='headline' AND `style_name`='顶部通栏' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_template`
  (`tenant_id`,`element_type`,`template_name`,`config_json`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','notice','通知公告-紧凑列表','{"perPage":8,"showContent":false}','8条紧凑列表',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_template` WHERE `tenant_id`='000000' AND `template_name`='通知公告-紧凑列表' AND `is_deleted`=0);

INSERT INTO `blade_portal_element_template`
  (`tenant_id`,`element_type`,`template_name`,`config_json`,`remark`,`is_deleted`,`status`,`create_time`)
SELECT '000000','todo','我的待办-标准','{"perPage":5,"dataOrgScope":1}','5条待办',0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_element_template` WHERE `tenant_id`='000000' AND `template_name`='我的待办-标准' AND `is_deleted`=0);

-- -----------------------------------------------------------------------------
-- 5) 管理菜单：样式库（普通子菜单，两段连写路径 → ./pages/Portal/StyleLib/index.tsx）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_style_lib', '门户样式库', 'portal_style_lib', '/portal/stylelib', 'api', 15,
       1, 1, 0, 0,
       '门户样式库（菜单样式/元素样式/头条样式/元素模板 四页签；普通子菜单 → 组件 ./pages/Portal/StyleLib/index.tsx；path 末段须连写 stylelib）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_style_lib' AND m.`is_deleted` = 0);

-- 6) 权限按钮 + 权限码 -------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_style_lib_manage' AS `code`, '门户样式库按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_style_lib_maint',   '门户样式库维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 7) 授权：仅超级管理员 -------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_style_lib','portal_style_lib_manage','portal_style_lib_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 8) 校验输出 ---------------------------------------------------------------------------
SELECT `style_type`, COUNT(*) AS `cnt` FROM `blade_portal_menu_style` WHERE `is_deleted` = 0 GROUP BY `style_type`;
SELECT `style_kind`, COUNT(*) AS `cnt` FROM `blade_portal_element_style` WHERE `is_deleted` = 0 GROUP BY `style_kind`;
SELECT `element_type`, `template_name` FROM `blade_portal_element_template` WHERE `is_deleted` = 0;
