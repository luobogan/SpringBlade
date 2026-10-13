-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「公文正文文档」与「正文版本」表（收发文一体化 L1 核心）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_001
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_doc（新建）、wf_odoc_doc_version（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_doc_version`;
--   DROP TABLE IF EXISTS `wf_odoc_doc`;
--
-- 背景
--   收发文一体化方案 §3.2.1：正文文档主表 + 版本表，支撑在线编辑（每次保存产新版本）、
--   套红（has_used_templet 标记）、取消套红回滚（依赖版本历史）。正文与流程实例通过
--   instance_id 关联，doc_edition_id 冗余指向当前版本便于取最新。
--   所有实体继承 TenantEntity，故每张表均含完整基础列（tenant_id/create_user/create_dept/
--   create_time/update_user/update_time/status/is_deleted）。

CREATE TABLE IF NOT EXISTS `wf_odoc_doc` (
    `id`               BIGINT       NOT NULL COMMENT '正文ID(雪花)',
    `def_id`           BIGINT       DEFAULT NULL COMMENT '流程定义ID',
    `instance_id`      BIGINT       DEFAULT NULL COMMENT '流程实例ID',
    `title`            VARCHAR(500) DEFAULT NULL COMMENT '正文标题',
    `sec_category`     BIGINT       DEFAULT NULL COMMENT '存放目录ID',
    `doc_status`       TINYINT      DEFAULT 9 COMMENT '文档状态 9草稿 1正式',
    `doc_edition_id`   BIGINT       DEFAULT NULL COMMENT '当前版本ID(冗余,便于取最新)',
    `has_used_templet` TINYINT      DEFAULT 0 COMMENT '是否已套红 0否 1是',
    `edit_mould_id`    BIGINT       DEFAULT NULL COMMENT '当前编辑模板ID',
    `is_test`          TINYINT      DEFAULT 0 COMMENT '是否测试流程产生的数据(可一键清理)',
    `tenant_id`        VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`      BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`      BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`      BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`           INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`       INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_wf_odoc_doc_inst` (`instance_id`, `is_deleted`),
    KEY `idx_wf_odoc_doc_test` (`is_test`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文正文文档';

-- 正文版本表 ★ 取消套红回滚依赖此表
CREATE TABLE IF NOT EXISTS `wf_odoc_doc_version` (
    `id`                BIGINT       NOT NULL COMMENT '版本ID(雪花)',
    `doc_id`            BIGINT       NOT NULL COMMENT '正文ID',
    `version_id`        INT          NOT NULL COMMENT '版本序号(自增,按doc)',
    `file_id`           BIGINT       DEFAULT NULL COMMENT '文件ID(指向文件服务)',
    `file_name`         VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    `doc_file_type`     TINYINT      DEFAULT NULL COMMENT '3doc 7docx 12pdf 13ofd',
    `has_used_templet`  TINYINT      DEFAULT 0 COMMENT '本版本是否已套红',
    `is_ext_file`       TINYINT      DEFAULT 0 COMMENT '是否外部附件(不参与套红)',
    `version_detail`    VARCHAR(500) DEFAULT NULL COMMENT '版本说明(如:套红前取消痕迹版本)',
    `tenant_id`         VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`       BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`       BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`       BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`            INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`        INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_version` (`doc_id`, `version_id`, `is_deleted`),
    KEY `idx_doc_th` (`doc_id`, `has_used_templet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文正文版本';
