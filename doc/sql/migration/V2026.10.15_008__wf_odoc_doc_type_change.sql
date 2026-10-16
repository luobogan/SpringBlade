-- -----------------------------------------------------------------------------
-- 迁移脚本：文种转换（配置 + 记录）+ 建文设置补充「变更次数限制」
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_008
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_process_set（加列）、wf_odoc_doc_type_change（新建）、wf_odoc_doc_type_change_revision（新建）
-- 变更类型  : DDL（ALTER + CREATE）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_doc_type_change_revision`;
--   DROP TABLE IF EXISTS `wf_odoc_doc_type_change`;
--   ALTER TABLE `wf_odoc_process_set` DROP COLUMN `doc_type_change_times`, DROP COLUMN `doc_type_change_number`;
--
-- 背景
--   文种转换（对应 E9 OdocTypeChangeWorkflow + Odoc_TypeChangeRevision）：配置=源流程→目标流程映射；
--   记录=某实例的变更留痕；次数限制落在建文设置（docTypeChangeNumber 1不限/2一次/3自定义）。
--   引擎级重路由/起新实例为后续增强（方案 §14.5），当前 submit 记录变更并更新公文镜像 officalType。

ALTER TABLE `wf_odoc_process_set`
    ADD COLUMN `doc_type_change_number` TINYINT DEFAULT NULL COMMENT '文种变更次数限制 1不限制 2仅一次 3自定义',
    ADD COLUMN `doc_type_change_times`  INT    DEFAULT NULL COMMENT '文种变更自定义次数(number=3时有效)';

CREATE TABLE IF NOT EXISTS `wf_odoc_doc_type_change` (
    `id`                 BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `def_id`             BIGINT       NOT NULL COMMENT '源流程定义ID',
    `target_def_id`      BIGINT       NOT NULL COMMENT '目标流程定义ID',
    `target_offical_type`TINYINT      DEFAULT NULL COMMENT '目标公文类型 1发文 2收文 3签报',
    `is_default`         TINYINT      DEFAULT 0 COMMENT '是否默认目标 0否 1是',
    `field_maps`         JSON         DEFAULT NULL COMMENT '赋值设置(源字段→目标字段)',
    `enabled`            TINYINT      DEFAULT 1 COMMENT '是否启用 0否 1是',
    `tenant_id`          VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`        BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`        BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`        BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`             INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`         INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_odoc_dtc_src_tgt` (`def_id`, `target_def_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文种转换配置(源流程→目标流程)';

CREATE TABLE IF NOT EXISTS `wf_odoc_doc_type_change_revision` (
    `id`                 BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `instance_id`        BIGINT       NOT NULL COMMENT '原流程实例ID',
    `def_id`             BIGINT       DEFAULT NULL COMMENT '源流程定义ID',
    `target_def_id`      BIGINT       DEFAULT NULL COMMENT '目标流程定义ID',
    `target_offical_type`TINYINT      DEFAULT NULL COMMENT '目标公文类型 1发文 2收文 3签报',
    `reasons`            VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    `operator_id`        BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `chan_instance_id`   BIGINT       DEFAULT NULL COMMENT '变更后新实例ID(引擎级重路由后续增强)',
    `is_test`            TINYINT      DEFAULT 0 COMMENT '是否测试流程产生的数据(可一键清理)',
    `tenant_id`          VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`        BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`        BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`        BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`             INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`         INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_odoc_dtc_rev_inst` (`instance_id`, `is_deleted`),
    KEY `idx_odoc_dtc_rev_test` (`is_test`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文种转换记录(按实例)';
