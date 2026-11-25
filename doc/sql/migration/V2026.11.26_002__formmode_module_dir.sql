-- 表单建模模块目录树：复用 modetreefield，并新增目录权限表。

ALTER TABLE `modetreefield`
  ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  ADD COLUMN `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  ADD COLUMN `create_user` bigint DEFAULT NULL COMMENT '创建人',
  ADD COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  ADD COLUMN `update_user` bigint DEFAULT NULL COMMENT '更新人',
  ADD COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间';

ALTER TABLE `modeinfo`
  MODIFY COLUMN `modeTreeField` bigint DEFAULT NULL COMMENT '应用分类ID';

CREATE TABLE IF NOT EXISTS `form_dir_auth` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `dir_id` bigint NOT NULL COMMENT '目录ID',
  `subject_type` int NOT NULL COMMENT '授权对象类型：1角色 2部门 3岗位 4人员 5分部 7全员',
  `subject_id` bigint NOT NULL DEFAULT 0 COMMENT '授权对象ID',
  `right_type` int NOT NULL COMMENT '权限类型：1可见 2新增 3编辑 4删除',
  `include_sub` int NOT NULL DEFAULT 0 COMMENT '是否包含子级',
  `status` int NOT NULL DEFAULT 1 COMMENT '业务状态',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_dir_auth` (`tenant_id`,`dir_id`,`subject_type`,`subject_id`,`right_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单建模模块目录权限';
