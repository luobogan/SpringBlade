-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「版式文件 / 元数据 / 电子签章 / GB/T9704 规则库」表（收发文一体化 L2）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_005
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_layout_file、wf_odoc_meta、wf_odoc_seal、wf_odoc_gbt_profile
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_gbt_profile`;
--   DROP TABLE IF EXISTS `wf_odoc_seal`;
--   DROP TABLE IF EXISTS `wf_odoc_meta`;
--   DROP TABLE IF EXISTS `wf_odoc_layout_file`;
--
-- 背景
--   方案 §3.2.4：L2 版式能力的数据底座。wf_odoc_layout_file 与 doc_version 平行，承载
--   PDF/OFD 定稿形态（L1 仅 PDF、L2 叠加 OFD，同一张表）；wf_odoc_meta 写入 OFD 元数据区供
--   档案检索；wf_odoc_seal 记录 CA 签章与验章；wf_odoc_gbt_profile 是 GB/T 9704 版式规则库
--   （一切版式能力的地基，P0 前置项）。

CREATE TABLE IF NOT EXISTS `wf_odoc_layout_file` (
    `id`             BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `doc_id`         BIGINT       NOT NULL COMMENT '正文ID',
    `src_version_id` BIGINT       DEFAULT NULL COMMENT '来源docx版本ID',
    `format`         TINYINT      NOT NULL COMMENT '版式格式 12PDF 13OFD',
    `file_id`        BIGINT       NOT NULL COMMENT '版式文件ID',
    `vendor`         VARCHAR(32)  DEFAULT NULL COMMENT '转换厂商 shuke/yozo/fuxin/fuxin-gsdk',
    `layout_stage`   TINYINT      DEFAULT 3 COMMENT '3版式态 4签发态 5归档态',
    `is_final`       TINYINT      DEFAULT 0 COMMENT '是否定稿(定稿后不可再编辑docx)',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_layout_doc` (`doc_id`, `format`, `layout_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文版式文件';

-- 公文元数据（写入 OFD 元数据区，档案检索用；对应 E9 getMetaData）
CREATE TABLE IF NOT EXISTS `wf_odoc_meta` (
    `id`           BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `doc_id`       BIGINT       NOT NULL,
    `instance_id`  BIGINT       DEFAULT NULL,
    `meta_key`     VARCHAR(64)  NOT NULL COMMENT '如 issuedNum/title/secretLevel/urgency/writtenDate/issuedUnit',
    `meta_value`   VARCHAR(500) DEFAULT NULL,
    `tenant_id`    VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`  BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`  BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`  BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`   INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_meta_doc` (`doc_id`, `meta_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文元数据(OFD结构化)';

-- 电子签章记录
CREATE TABLE IF NOT EXISTS `wf_odoc_seal` (
    `id`             BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `doc_id`         BIGINT       NOT NULL,
    `layout_file_id` BIGINT       NOT NULL COMMENT '被签章版式文件',
    `signer_id`      BIGINT       NOT NULL COMMENT '签章人',
    `seal_type`      TINYINT      DEFAULT 1 COMMENT '1个人签字 2单位公章 3时间戳',
    `page`           INT          DEFAULT 1,
    `offset_x`       DOUBLE       DEFAULT 0,
    `offset_y`       DOUBLE       DEFAULT 0,
    `vendor`         VARCHAR(32)  DEFAULT NULL COMMENT 'CA厂商 qiyuesuo/...',
    `vendor_seal_id` VARCHAR(128) DEFAULT NULL COMMENT '厂商侧签章/合同ID',
    `seal_file_id`   BIGINT       DEFAULT NULL COMMENT '签章后文件',
    `cert_sn`        VARCHAR(128) DEFAULT NULL COMMENT '签章证书序列号',
    `algorithm`      VARCHAR(32)  DEFAULT NULL COMMENT '签名算法 RSA/SM2',
    `tsa_token`      VARCHAR(500) DEFAULT NULL COMMENT 'RFC3161时间戳令牌',
    `status`         TINYINT      DEFAULT 0 COMMENT '0待签 1已签 2已验 3已撤销',
    `sign_time`      DATETIME     DEFAULT NULL,
    `verify_time`    DATETIME     DEFAULT NULL,
    `verify_result`  VARCHAR(500) DEFAULT NULL,
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_seal_doc` (`doc_id`, `layout_file_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子签章记录';

-- GB/T 9704 版式规则库（可按机关/文种定制，一切版式能力的地基）
CREATE TABLE IF NOT EXISTS `wf_odoc_gbt_profile` (
    `id`           BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `profile_name` VARCHAR(128) NOT NULL COMMENT 'profile名称,如 默认/XX市政府/党委机关',
    `org_scope`    VARCHAR(128) DEFAULT NULL COMMENT '适用机关(空=全局)',
    `standard_ver` VARCHAR(32)  DEFAULT 'GB/T 9704-2012',
    `profile_json` JSON         NOT NULL COMMENT 'Gbt9704Profile 序列化',
    `enabled`      TINYINT      DEFAULT 1,
    `tenant_id`    VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`  BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`  BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`  BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`   INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_gbt_org` (`org_scope`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GB/T9704版式规则库';
