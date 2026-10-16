-- -----------------------------------------------------------------------------
-- 迁移脚本：公文交换（内部交换记录 + 外部交换平台设置）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_009
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_exchange_inner（新建）、wf_odoc_exchange_setting（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_exchange_setting`;
--   DROP TABLE IF EXISTS `wf_odoc_exchange_inner`;
--
-- 背景
--   内部交换（对应 E9 exchange_senddocinfo_inner/receive_doc_inner）：把公文推送目标机构，按方向记录。
--   外部交换（对应 E9 odoc_exchange_*）：仅持久化平台连接配置，真正 WS/FTP 收发由 IOdocExchangeClient
--   适配器完成（环境相关，默认实现未接入明确报错，不造假集成）。

CREATE TABLE IF NOT EXISTS `wf_odoc_exchange_inner` (
    `id`                 BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `instance_id`        BIGINT       NOT NULL COMMENT '流程实例ID',
    `offical_type`       TINYINT      DEFAULT NULL COMMENT '公文类型 1发文 2收文 3签报',
    `direction`          TINYINT      DEFAULT 1 COMMENT '方向 1发出 2接收',
    `target_unit`        VARCHAR(255) DEFAULT NULL COMMENT '对方机构/单位',
    `document_identifier`VARCHAR(128) DEFAULT NULL COMMENT '公文标识(交换生成)',
    `issued_num`         VARCHAR(128) DEFAULT NULL COMMENT '发文文号',
    `issued_unit`        VARCHAR(255) DEFAULT NULL COMMENT '印发/来文单位',
    `exchange_status`    TINYINT      DEFAULT 0 COMMENT '交换状态 0待接收 1已接收 2已退回',
    `remark`             VARCHAR(500) DEFAULT NULL COMMENT '备注',
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
    KEY `idx_odoc_exc_inner_inst` (`instance_id`, `is_deleted`),
    KEY `idx_odoc_exc_inner_creator` (`create_user`, `direction`, `is_deleted`),
    KEY `idx_odoc_exc_inner_test` (`is_test`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内部交换记录';

CREATE TABLE IF NOT EXISTS `wf_odoc_exchange_setting` (
    `id`            BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `platform_code` VARCHAR(64)  NOT NULL COMMENT '平台编码(唯一)',
    `platform_name` VARCHAR(128) DEFAULT NULL COMMENT '平台名称',
    `type`          VARCHAR(16)  DEFAULT NULL COMMENT '交换方式 ws/ftp',
    `endpoint`      VARCHAR(255) DEFAULT NULL COMMENT '服务地址(ws endpoint 或 ftp host:port)',
    `app_key`       VARCHAR(128) DEFAULT NULL COMMENT '应用标识 appKey',
    `app_secret`    VARCHAR(255) DEFAULT NULL COMMENT '密钥(加密存储)',
    `enabled`       TINYINT      DEFAULT 0 COMMENT '是否启用 0否 1是',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`   BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_odoc_exc_set_code` (`platform_code`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部交换平台设置';
