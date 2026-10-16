-- =============================================================================
-- 迁移脚本 V2026.10.14_003：门户默认首页（按机构）表 blade_portal_default
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_003
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_default
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §13.8
--              泛微 defaultportal（按机构指定登录后默认首页）
-- 说明      : dept_id 对应 blade_dept.id。解析时取用户部门祖先链（blade_dept.ancestors）
--             命中的"最具体"配置：配在父部门，其子孙部门用户同样生效。
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_default`;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_default` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `dept_id`      bigint       NOT NULL                COMMENT '机构/部门ID（blade_dept.id，租户内唯一）',
  `page_id`      bigint       NOT NULL                COMMENT '默认门户页面ID（blade_portal_page.id）',
  `remark`       varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`   int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用）',
  `create_user`  bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept` (`dept_id`),
  KEY `idx_page_id` (`page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户默认首页（按机构，对标泛微 defaultportal）';

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `default_rows` FROM `blade_portal_default`;
