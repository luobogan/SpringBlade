-- =============================================================================
-- 迁移脚本 V2026.10.15_004：用户个人门户布局表 blade_portal_user_layout
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_004
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_user_layout
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §18.2 / §23
--              泛微 hpcurrentuse / 用户专属 hplayout（终端用户个性化排列）
-- 说明      : 按 (user_id, page_id) 一行，layout_json 存该用户对门户页元素的排列覆盖
--              [{elementId,rowNo,colSpan,sortOrder,hidden}]。为空=沿用创建者默认布局。
--              /my-portal 渲染时应用覆盖（hidden=1 跳过，其余覆盖行/栅格/排序后重排）。
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_user_layout`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_user_layout` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `user_id`      bigint       NOT NULL                COMMENT '用户ID',
  `page_id`      bigint       NOT NULL                COMMENT '门户页面ID（blade_portal_page.id）',
  `layout_json`  text                  DEFAULT NULL   COMMENT '布局JSON：[{elementId,rowNo,colSpan,sortOrder,hidden}]',
  `is_deleted`   int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_page` (`user_id`, `page_id`),
  KEY `idx_page_id` (`page_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户个人门户布局（对标泛微 hpcurrentuse）';

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `user_layout_rows` FROM `blade_portal_user_layout`;
