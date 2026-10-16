-- =============================================================================
-- 迁移脚本 V2026.10.16_005：素材库 + 新闻模板（对标 materiallib / newstemplate）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_005
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_material / blade_portal_news_template；
--             blade_menu（素材库管理页 + 按钮 + 权限码）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §15.4 C1（newstemplate 新闻模板库）
--             §15.4 C2（materiallib 图片素材库）
--             泛微 materiallib 纯文件系统无关系表；SpringBlade 落地为「文件存资源服务 +
--             元数据落表」，与 blade-resource/OSS 体系对齐（本地化偏差，见设计说明）。
--             泛微 newstemplate 落 PAGENEWSTEMPLATE(+LAYOUT) 两表；本方案合并为一张
--             （layout_html 直接存版式 HTML，inner/outer 由 template_type 区分）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_material','portal_material_manage','portal_material_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_material','portal_material_manage','portal_material_maint');
--   DROP TABLE IF EXISTS `blade_portal_news_template`;
--   DROP TABLE IF EXISTS `blade_portal_material`;
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 素材库元数据表（对标 materiallib §15.4 C2；文件本体走资源服务，本表记元数据）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_material` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `dir_name`      varchar(128) NOT NULL DEFAULT '默认目录' COMMENT '所属目录名（对标 getDirs 目录维度）',
  `material_name` varchar(128) NOT NULL                COMMENT '素材名称',
  `material_type` varchar(32)  NOT NULL DEFAULT 'image' COMMENT '素材类型（image图片/file文件）',
  `file_url`      varchar(512) NOT NULL                COMMENT '文件访问地址（资源服务/OSS 路径）',
  `file_size`     bigint                 DEFAULT NULL   COMMENT '文件大小（字节）',
  `mime_type`     varchar(128)          DEFAULT NULL   COMMENT 'MIME类型',
  `remark`        varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`    int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`        int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`   bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`   bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`   datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`   bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`   datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_dir` (`dir_name`),
  KEY `idx_type` (`material_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户素材库元数据（对标泛微 materiallib §15.4）';

-- -----------------------------------------------------------------------------
-- 2) 新闻模板库（对标 PAGENEWSTEMPLATE §15.4 C1；inner/outer 两类排版模板）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_news_template` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `template_name`  varchar(128) NOT NULL                COMMENT '模板名称',
  `template_type`  varchar(32)  NOT NULL DEFAULT 'inner' COMMENT '模板类型（inner内部新闻 outer外部新闻）',
  `layout_html`    text                  DEFAULT NULL   COMMENT '版式HTML（标题/摘要/时间占位符替换）',
  `is_default`     int          NOT NULL DEFAULT '0'    COMMENT '是否默认模板（同类型内唯一）',
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
  KEY `idx_type` (`template_type`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户新闻模板库（对标泛微 PAGENEWSTEMPLATE §15.4）';

-- -----------------------------------------------------------------------------
-- 3) 种子：默认新闻模板（内部/外部各一）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_news_template`
  (`tenant_id`,`template_name`,`template_type`,`layout_html`,`is_default`,`is_deleted`,`status`,`create_time`)
SELECT '000000','内部新闻-标准','inner',
       '<div class="news-item"><h4>{title}</h4><p class="summary">{summary}</p><span class="time">{time}</span></div>',
       1,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_news_template` WHERE `tenant_id`='000000' AND `template_name`='内部新闻-标准' AND `is_deleted`=0);

INSERT INTO `blade_portal_news_template`
  (`tenant_id`,`template_name`,`template_type`,`layout_html`,`is_default`,`is_deleted`,`status`,`create_time`)
SELECT '000000','外部新闻-图文','outer',
       '<div class="news-item with-img"><img src="{cover}" alt="{title}"/><div><h4>{title}</h4><p class="summary">{summary}</p><span class="time">{time}</span></div></div>',
       1,0,1,NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_news_template` WHERE `tenant_id`='000000' AND `template_name`='外部新闻-图文' AND `is_deleted`=0);

-- -----------------------------------------------------------------------------
-- 4) 管理菜单：素材库（普通子菜单，两段连写路径 → ./pages/Portal/Material/index.tsx）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_material', '门户素材库', 'portal_material', '/portal/material', 'api', 16,
       1, 1, 0, 0,
       '门户素材库（图片素材/新闻模板 两页签；普通子菜单 → 组件 ./pages/Portal/Material/index.tsx；path 末段须连写 material）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_material' AND m.`is_deleted` = 0);

-- 5) 权限按钮 + 权限码 -------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_material_manage' AS `code`, '门户素材库按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_material_maint',   '门户素材库维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员 -------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_material','portal_material_manage','portal_material_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 7) 校验输出 ---------------------------------------------------------------------------
SELECT `template_type`, `template_name`, `is_default` FROM `blade_portal_news_template` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path`, m.`category` FROM `blade_menu` m
 WHERE m.`code` IN ('portal_material','portal_material_manage','portal_material_maint') AND m.`is_deleted` = 0;
