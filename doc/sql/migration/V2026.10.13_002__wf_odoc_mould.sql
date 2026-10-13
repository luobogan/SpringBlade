-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「公文模板 / 书签 / 字段映射 / 正文书签值」表（收发文一体化）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_002
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_mould、wf_odoc_mould_bookmark、wf_odoc_mould_field_map、wf_odoc_doc_bookmark
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_doc_bookmark`;
--   DROP TABLE IF EXISTS `wf_odoc_mould_field_map`;
--   DROP TABLE IF EXISTS `wf_odoc_mould_bookmark`;
--   DROP TABLE IF EXISTS `wf_odoc_mould`;
--
-- 背景
--   方案 §3.2.2：红头模板（含书签的 .docx）及其书签定义、书签→表单字段映射；
--   wf_odoc_doc_bookmark 落套红时的书签实际值，便于审计与复用（对应 E9 MouldBookMark / DocMouldBookMark）。

CREATE TABLE IF NOT EXISTS `wf_odoc_mould` (
    `id`             BIGINT       NOT NULL COMMENT '模板ID(雪花)',
    `mould_name`     VARCHAR(255) NOT NULL COMMENT '模板名',
    `mould_type`     TINYINT      NOT NULL COMMENT '1编辑 2套红 3打印 4版式',
    `file_id`        BIGINT       DEFAULT NULL COMMENT '模板文件ID(.docx,含书签)',
    `gbt_profile_id` BIGINT       DEFAULT NULL COMMENT '绑定的GB/T9704版式profile',
    `doc_type_code`  VARCHAR(32)  DEFAULT NULL COMMENT '文种代码(对应15文种)',
    `def_id`         BIGINT       DEFAULT NULL COMMENT '所属流程定义(空=全局)',
    `system_type`    TINYINT      DEFAULT 1,
    `enabled`        TINYINT      DEFAULT 1 COMMENT '是否启用',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_mould_def_type` (`def_id`, `mould_type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文模板';

-- 模板书签定义
CREATE TABLE IF NOT EXISTS `wf_odoc_mould_bookmark` (
    `id`           BIGINT       NOT NULL COMMENT '书签ID(雪花)',
    `mould_id`     BIGINT       NOT NULL COMMENT '模板ID',
    `name`         VARCHAR(128) NOT NULL COMMENT '书签名/占位符,如 fld_Mark / Content',
    `descript`     VARCHAR(255) DEFAULT NULL COMMENT '描述,如 [发文文号]',
    `element_type` VARCHAR(32)  DEFAULT NULL COMMENT '映射Gbt9704Profile.ElementType',
    `show_order`   INT          DEFAULT 0,
    `is_content`   TINYINT      DEFAULT 0 COMMENT '是否正文占位书签(1=不替换,插入正文)',
    `tenant_id`    VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`  BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`  BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`  BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`   INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_bm_mould` (`mould_id`, `show_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板书签定义';

-- 书签 → 表单字段映射
CREATE TABLE IF NOT EXISTS `wf_odoc_mould_field_map` (
    `id`             BIGINT       NOT NULL COMMENT '映射ID(雪花)',
    `def_id`         BIGINT       NOT NULL COMMENT '流程定义ID',
    `mould_id`       BIGINT       NOT NULL,
    `bookmark_id`    BIGINT       NOT NULL,
    `field_key`      VARCHAR(128) NOT NULL COMMENT '表单字段名(fieldName)',
    `scope`          VARCHAR(32)  DEFAULT 'main' COMMENT 'main / dt{idx}',
    `date_show_type` TINYINT      DEFAULT 0 COMMENT '0原样 1中文日期',
    `select_value`   VARCHAR(64)  DEFAULT '-1' COMMENT '模板选择值(文号型为 -{numberId}-100)',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_map_def_mould` (`def_id`, `mould_id`, `select_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模板书签-表单字段映射';

-- 书签实际值（便于审计/复用，对应 DocMouldBookMark）
CREATE TABLE IF NOT EXISTS `wf_odoc_doc_bookmark` (
    `id`             BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `doc_id`         BIGINT       NOT NULL,
    `mould_id`       BIGINT       NOT NULL,
    `bookmark_id`    BIGINT       NOT NULL,
    `bookmark_value` LONGTEXT     COMMENT '实际值',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_dbm` (`doc_id`, `mould_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='正文书签实际值';
