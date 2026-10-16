-- =============================================================================
-- 迁移脚本 V2026.10.15_012：门户页面独立访客(UV)按日去重 blade_portal_page_visitor
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_012
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_page_visitor
-- 变更类型  : DDL
-- 说明      : 在 PV（blade_portal_page_stat）之外，按「页面+日期+租户+用户」唯一去重记录独立访客，
--              用于 UV 统计（PV=访问次数，UV=独立访客数）。INSERT IGNORE 保证同一用户同日多次
--              访问只计一次。登录前门户无登录用户，不作 UV 记录。
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_page_visitor`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_page_visitor` (
  `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `page_id`    bigint       NOT NULL                COMMENT '门户页面ID',
  `stat_date`  varchar(10)  NOT NULL                COMMENT '统计日期 yyyy-MM-dd',
  `tenant_id`  varchar(32)  DEFAULT '000000'        COMMENT '租户ID',
  `user_id`    bigint       NOT NULL                COMMENT '用户ID',
  `is_deleted` int          NOT NULL DEFAULT '0'    COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_date_tenant_user` (`page_id`, `stat_date`, `tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户页面独立访客（按日/用户去重）';

-- 校验输出 -------------------------------------------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT
  FROM information_schema.columns
 WHERE table_schema = 'blade' AND table_name = 'blade_portal_page_visitor'
 ORDER BY ORDINAL_POSITION;
