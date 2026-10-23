-- V2026.10.22_001__blade_doc_tables.sql
-- 内容引擎（DocEngine）→ SpringBlade 落地 · 基础设施阶段 I-2
--   新建 blade-doc 微服务 9 张表（org.springblade.doc.entity.*，均继承 TenantEntity）。
--   对齐《泛微E9-内容引擎-SpringBlade落地规范》§1 表/实体映射。
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行；库无关（作用于当前 DATABASE()，dev 为共享 blade 库）。
-- 公共列口径：对齐 blade 库既有表（tenant_id + create_user/create_dept/create_time/update_user/update_time/status/is_deleted）。

CREATE TABLE IF NOT EXISTS `doc_sec_category` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `name` VARCHAR(255) NOT NULL COMMENT '目录名',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '上级目录ID(0=主目录)',
  `manage_role_id` BIGINT NULL COMMENT '管理角色ID',
  `sub_company_id` BIGINT NULL COMMENT '所属分部(DataScope隔离维度)',
  `is_open_water` INT NULL DEFAULT 0 COMMENT '是否开启水印',
  `coder` VARCHAR(64) NULL COMMENT '编号前缀',
  `max_upload_file_size` INT NULL DEFAULT 5 COMMENT '单文件上传大小限制(MB)',
  `nodownload` INT NULL DEFAULT 0 COMMENT '是否禁止下载',
  `edition_is_open` INT NULL DEFAULT 0 COMMENT '是否开启版本管理',
  `edition_prefix` VARCHAR(64) NULL COMMENT '版本前缀',
  `reader_can_view_history_edition` INT NULL DEFAULT 0 COMMENT '读者是否可看历史版本',
  `is_open_approve_wf` INT NULL DEFAULT 0 COMMENT '是否开启审批 0否 1审批流 2批准流',
  `approve_workflow_id` BIGINT NULL COMMENT '批准工作流ID',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_sec_category_parent` (`parent_id`),
  KEY `idx_doc_sec_category_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档子目录';

CREATE TABLE IF NOT EXISTS `doc_detail` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `sec_category_id` BIGINT NULL COMMENT '所属目录',
  `doc_subject` VARCHAR(500) NULL COMMENT '文档标题',
  `doc_status` INT NULL COMMENT '文档状态 0草稿/1生效/2生效/3审批中/4退回/5归档/6待发布/7失效/8作废/9流程草稿',
  `doc_edition_id` BIGINT NULL COMMENT '版本族ID',
  `doc_edition` INT NULL COMMENT '版次号',
  `is_history` INT NULL DEFAULT 0 COMMENT '是否历史版本',
  `main_file_id` BIGINT NULL COMMENT '正文文件ID',
  `content_id` BIGINT NULL COMMENT '正文内容ID',
  `sub_company_id` BIGINT NULL COMMENT '所属分部(DataScope隔离维度)',
  `is_top` INT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_valid` INT NULL DEFAULT 1 COMMENT '是否有效',
  `selected_pub_mould_id` BIGINT NULL COMMENT '发布选用的显示模板ID',
  `check_out_status` INT NULL DEFAULT 0 COMMENT '签出状态 0未签出 1已签出',
  `check_out_user_id` BIGINT NULL COMMENT '签出人',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_detail_category` (`sec_category_id`),
  KEY `idx_doc_detail_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档主表';

CREATE TABLE IF NOT EXISTS `doc_detail_content` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `content` LONGTEXT NULL COMMENT '正文HTML内容',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_detail_content_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档正文内容';

CREATE TABLE IF NOT EXISTS `doc_image_file` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `version_id` BIGINT NULL COMMENT '文件版本ID',
  `file_id` BIGINT NULL COMMENT '真实文件ID(imagefile)',
  `file_name` VARCHAR(500) NULL COMMENT '文件名',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `doc_file_type` INT NULL COMMENT '文件类型 1正文图片 2附件 3-11 office等',
  `is_ext_file` INT NULL DEFAULT 0 COMMENT '是否外部文件',
  `has_used_templet` INT NULL DEFAULT 0 COMMENT '是否使用模板',
  `version_detail` VARCHAR(500) NULL COMMENT '版本说明',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_image_file_doc` (`doc_id`),
  KEY `idx_doc_image_file_file` (`file_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档文件关联';

CREATE TABLE IF NOT EXISTS `doc_share` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `share_type` INT NULL COMMENT '共享对象类型 1人员2分部3部门4角色5所有人6群组...',
  `target_id` BIGINT NULL COMMENT '共享对象ID',
  `share_level` INT NULL COMMENT '共享级别 1查看 2编辑 3完全控制',
  `is_sec_default_share` INT NULL DEFAULT 0 COMMENT '是否继承自目录默认共享',
  `share_source` VARCHAR(255) NULL COMMENT '共享来源',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_share_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档共享';

CREATE TABLE IF NOT EXISTS `doc_dummy_detail` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `catalog_id` BIGINT NULL COMMENT '虚拟目录ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_dummy_catalog` (`catalog_id`),
  KEY `idx_doc_dummy_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档虚拟目录挂载';

CREATE TABLE IF NOT EXISTS `doc_detail_log` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `operate_type` INT NULL COMMENT '操作类型 0阅读1新增2改13发布14版本18签出20签入21打印22下载23共享',
  `operate_user` BIGINT NULL COMMENT '操作人',
  `operate_desc` VARCHAR(500) NULL COMMENT '操作描述',
  `client_address` VARCHAR(128) NULL COMMENT '客户端地址',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_detail_log_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档操作日志';

CREATE TABLE IF NOT EXISTS `download_log` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `file_id` BIGINT NULL COMMENT '文件ID',
  `operate_user` BIGINT NULL COMMENT '下载人',
  `client_address` VARCHAR(128) NULL COMMENT '客户端地址',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_download_log_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '下载日志';

CREATE TABLE IF NOT EXISTS `doc_recycle` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `delete_user` BIGINT NULL COMMENT '删除人',
  `delete_time` DATETIME NULL COMMENT '删除时间',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_recycle_doc` (`doc_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档回收站';
