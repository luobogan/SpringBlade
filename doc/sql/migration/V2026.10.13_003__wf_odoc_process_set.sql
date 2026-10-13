-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「公文流程设置」与「公文信息(按实例)」表（收发文一体化）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_003
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_process_set、wf_odoc_request
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_request`;
--   DROP TABLE IF EXISTS `wf_odoc_process_set`;
--
-- 背景
--   方案 §3.2.3：流程侧扩展。wf_odoc_process_set 承载「流程级开关」（offical_type、正文字段、
--   套红/签章/清稿/打印节点、layout_format 三层开关等），是 L1/L2 共存与流程级灰度的核心；
--   wf_odoc_request 冗余公文关键信息（对应 E9 odoc_requestdoc），每次流转提交后重建。

CREATE TABLE IF NOT EXISTS `wf_odoc_process_set` (
    `id`                    BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `def_id`                BIGINT       NOT NULL COMMENT '流程定义ID',
    `offical_type`          TINYINT      DEFAULT 0 COMMENT '0非公文 1发文 2收文 3签报',
    `doc_field_key`         VARCHAR(128) DEFAULT NULL COMMENT '正文字段(表单存doc_id)',
    `doc_title_field_key`   VARCHAR(128) DEFAULT NULL COMMENT '标题字段',
    `taohong_node_key`      VARCHAR(128) DEFAULT NULL COMMENT '套红节点(对应 useTempletNode)',
    `signature_node_keys`   VARCHAR(500) DEFAULT NULL COMMENT '签章节点,逗号分隔',
    `clean_copy_node_keys`  VARCHAR(500) DEFAULT NULL COMMENT '清稿节点',
    `print_node_keys`       VARCHAR(500) DEFAULT NULL COMMENT '打印节点',
    `number_field_key`      VARCHAR(128) DEFAULT NULL COMMENT '文号字段',
    `default_sec_category`  BIGINT       DEFAULT NULL COMMENT '默认正文档目录',
    `is_permission_temp`    TINYINT      DEFAULT 0 COMMENT '是否按权限选套红模板',
    `taohong_mould_id`      BIGINT       DEFAULT NULL COMMENT '默认套红模板',
    `must_taohong`          TINYINT      DEFAULT 1 COMMENT '套红节点提交前是否强制套红',
    `finalize_node_key`     VARCHAR(128) DEFAULT NULL COMMENT '定稿节点',
    `layout_node_key`       VARCHAR(128) DEFAULT NULL COMMENT '转版式节点',
    `sign_node_key`         VARCHAR(128) DEFAULT NULL COMMENT '签章节点',
    `layout_format`         TINYINT      DEFAULT 13 COMMENT '版式格式 12PDF 13OFD 0=不启用',
    `ofd_vendor`            VARCHAR(32)  DEFAULT NULL COMMENT 'OFD厂商(空=取全局配置)',
    `seal_required`         TINYINT      DEFAULT 0 COMMENT '是否必须电子签章',
    `seal_config`           JSON         DEFAULT NULL COMMENT '签章位置配置(页/坐标/章型)',
    `tenant_id`             VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`           BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`           BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`           BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`                INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`            INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_odoc_set_def` (`def_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文流程设置';

-- 公文信息冗余（对应 odoc_requestdoc，每次流转提交后重建）
CREATE TABLE IF NOT EXISTS `wf_odoc_request` (
    `id`                  BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `instance_id`         BIGINT       NOT NULL,
    `def_id`              BIGINT       DEFAULT NULL,
    `doc_id`              BIGINT       DEFAULT NULL,
    `source_instance_id`  BIGINT       DEFAULT NULL COMMENT '转文来源实例ID(收文转发文等,见§3.11)',
    `offical_type`        TINYINT      DEFAULT NULL,
    `issued_num`          VARCHAR(128) DEFAULT NULL COMMENT '发文文号',
    `receive_num`         VARCHAR(128) DEFAULT NULL COMMENT '收文编号',
    `issued_unit`         VARCHAR(255) DEFAULT NULL COMMENT '印发/来文单位',
    `receive_unit`        VARCHAR(255) DEFAULT NULL COMMENT '主送/收文单位',
    `written_date`        DATETIME     DEFAULT NULL COMMENT '成文日期',
    `issued_date`         DATETIME     DEFAULT NULL COMMENT '印发日期',
    `secret_level`        VARCHAR(32)  DEFAULT NULL COMMENT '密级',
    `urgency`             VARCHAR(32)  DEFAULT NULL COMMENT '紧急程度',
    `topic_type`          VARCHAR(64)  DEFAULT NULL COMMENT '主题词',
    `tenant_id`           VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`         BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`         BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`         BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`              INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`          INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_odoc_req_inst` (`instance_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文信息(按实例)';
