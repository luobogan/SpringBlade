-- =============================================================================
-- 迁移脚本 V2026.10.15_001：用户个人默认门户表 blade_portal_user_default
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_001
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_user_default
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §18.2 / §22
--              泛微 hpcurrentuse（用户上次访问/个人默认首页）
-- 说明      : 用户自选"我的默认门户"。landing 优先级：
--              portalCode 精确 > 个人默认(本表) > 机构默认(blade_portal_default)
--              > 全局默认(blade_portal_page.is_default) > 首个命中授权页。
--              注意：个人默认页同样必须命中 blade_portal_page_share 授权才可见（§22 授权收紧）。
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_user_default`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_user_default` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `user_id`      bigint       NOT NULL                COMMENT '用户ID（租户内唯一，一个用户一条）',
  `page_id`      bigint       NOT NULL                COMMENT '个人默认门户页面ID（blade_portal_page.id）',
  `is_deleted`   int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_page_id` (`page_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户个人默认门户（对标泛微 hpcurrentuse）';

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `user_default_rows` FROM `blade_portal_user_default`;
