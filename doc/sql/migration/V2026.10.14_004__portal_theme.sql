-- =============================================================================
-- 迁移脚本 V2026.10.14_004：门户主题体系（落地 blade_portal_page.theme 预留字段）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_004
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_theme / blade_portal_user_theme
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子数据按 theme_key 判重）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §12.5(P2) / §13.7
--              泛微：themestyle（主题库）/ themecenter（主题个性化中心）
--                    blade_portal_page.theme 此前仅预留未用，本脚本落地为可选主题库
-- 说明      : theme_config 存 JSON：{primaryColor,bgColor,logoUrl,...}；用户可覆盖租户默认主题
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_user_theme`; DROP TABLE IF EXISTS `blade_portal_theme`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_theme` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `theme_name`   varchar(128) NOT NULL                COMMENT '主题名称',
  `theme_key`    varchar(64)  NOT NULL                COMMENT '主题标识（租户内唯一，如 default/blue）',
  `theme_config` text                 DEFAULT NULL   COMMENT '主题配置JSON：{primaryColor,bgColor,logoUrl,...}',
  `is_default`   int                   DEFAULT '0'    COMMENT '是否租户默认主题（0否 1是）',
  `is_deleted`   int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `theme_key`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户主题库（对标泛微 themestyle）';

CREATE TABLE IF NOT EXISTS `blade_portal_user_theme` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `user_id`      bigint      NOT NULL                COMMENT '用户ID',
  `theme_id`     bigint      NOT NULL                COMMENT '选用的主题ID（blade_portal_theme.id）',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_theme_id` (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户主题偏好（对标泛微 themecenter）';

-- 种子：default 主题（与 blade_portal_page.theme 默认 'default' 对齐）
INSERT INTO `blade_portal_theme`
  (`tenant_id`, `theme_name`, `theme_key`, `theme_config`, `is_default`, `is_deleted`, `status`, `create_time`)
SELECT '000000', '默认主题', 'default', '{"primaryColor":"#1677ff","bgColor":"#f0f2f5","logoUrl":""}', 1, 0, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_theme` WHERE `tenant_id` = '000000' AND `theme_key` = 'default');

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `theme_rows` FROM `blade_portal_theme`;
SELECT COUNT(*) AS `user_theme_rows` FROM `blade_portal_user_theme`;
