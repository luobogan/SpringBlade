-- 表单建模列表的独立数据范围配置。
-- 与第四步动态列表数据范围 modedatasharerule 完全独立。

CREATE TABLE IF NOT EXISTS `form_manage_auth_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `scheme_id` bigint NOT NULL COMMENT '授权方案ID',
  `form_id` bigint NOT NULL COMMENT '表单ID',
  `subject_type` int NOT NULL COMMENT '授权对象类型',
  `subject_id` bigint NOT NULL COMMENT '授权对象ID',
  `scope_type` int NOT NULL DEFAULT 1 COMMENT '1全部表单 2本人创建 3指定表单',
  `scope_value` text COMMENT '指定表单ID，逗号分隔',
  `status` int NOT NULL DEFAULT 1 COMMENT '业务状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_fmas_scheme` (`tenant_id`,`scheme_id`),
  KEY `idx_fmas_subject` (`tenant_id`,`subject_type`,`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单建模列表数据范围';
