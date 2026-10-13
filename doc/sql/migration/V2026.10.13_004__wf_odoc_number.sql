-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「文号规则 / 使用记录 / 保留号 / 打印呈批设置」表（收发文一体化）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_004
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_number、wf_odoc_number_request、wf_odoc_number_reserved、wf_odoc_print_set
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_print_set`;
--   DROP TABLE IF EXISTS `wf_odoc_number_reserved`;
--   DROP TABLE IF EXISTS `wf_odoc_number_request`;
--   DROP TABLE IF EXISTS `wf_odoc_number`;
--
-- 背景
--   方案 §3.2.6：文号并发取号依赖 wf_odoc_number（规则）+ wf_odoc_number_request（占用记录，
--   唯一索引 instance_id+number_id 防重复取号）+ wf_odoc_number_reserved（保留号）；
--   wf_odoc_print_set 按节点配置打印/呈批模板（对应 workflow_printset）。

CREATE TABLE IF NOT EXISTS `wf_odoc_number` (
    `id`          BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `type_name`   VARCHAR(128) NOT NULL COMMENT '文号名称,如 普发文件',
    `prefix`      VARCHAR(64)  DEFAULT NULL COMMENT '机关代字,如 X政发',
    `year_pre`    VARCHAR(16)  DEFAULT NULL COMMENT '年份前缀,如 [',
    `year_end`    VARCHAR(16)  DEFAULT NULL COMMENT '年份后缀,如 ]',
    `show_year`   TINYINT      DEFAULT 1 COMMENT '是否显示年份',
    `serial_pre`  VARCHAR(16)  DEFAULT NULL COMMENT '流水前缀,如 第',
    `serial`      INT          DEFAULT 0 COMMENT '当前流水号',
    `digit`       INT          DEFAULT 4 COMMENT '流水位数',
    `fill_zero`   TINYINT      DEFAULT 1 COMMENT '流水是否补零',
    `def_ids`     VARCHAR(500) DEFAULT NULL COMMENT '绑定流程定义,逗号分隔',
    `mould_ids`   VARCHAR(500) DEFAULT NULL COMMENT '绑定套红模板(文号型 select_value=-{id}-100)',
    `is_open`     TINYINT      DEFAULT 1 COMMENT '是否启用',
    `is_used`     TINYINT      DEFAULT 0,
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept` BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`  INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_num_def` (`def_ids`, `is_open`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文号规则';

-- 文号使用记录（对应 odoc_numberRequest）
CREATE TABLE IF NOT EXISTS `wf_odoc_number_request` (
    `id`          BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `def_id`      BIGINT       DEFAULT NULL,
    `instance_id` BIGINT       NOT NULL,
    `number_id`   BIGINT       NOT NULL COMMENT '文号规则ID',
    `number_name` VARCHAR(128) DEFAULT NULL COMMENT '生成的文号',
    `years`       VARCHAR(16)  DEFAULT NULL COMMENT '年份(跨年重置判断)',
    `serial`      INT          DEFAULT NULL COMMENT '占用流水',
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept` BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`  INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_numreq_inst` (`instance_id`, `number_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文号使用记录';

-- 文号保留号（对应 odoc_numberReserved，可选后期）
CREATE TABLE IF NOT EXISTS `wf_odoc_number_reserved` (
    `id`          BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `number_id`   BIGINT       NOT NULL COMMENT '文号规则ID',
    `serial`      INT          NOT NULL COMMENT '保留流水',
    `reserved_by` BIGINT       DEFAULT NULL,
    `expire_time` DATETIME     DEFAULT NULL COMMENT '过期自动释放',
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept` BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`  INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_numres` (`number_id`, `serial`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文号保留号';

-- 打印/呈批设置（对应 workflow_printset，按节点）
CREATE TABLE IF NOT EXISTS `wf_odoc_print_set` (
    `id`                  BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `def_id`              BIGINT       NOT NULL,
    `node_keys`           VARCHAR(500) DEFAULT NULL COMMENT '生效节点',
    `mould_id`            BIGINT       DEFAULT NULL COMMENT '打印呈批模板(mould_type=3)',
    `print_flow_comment`  TINYINT      DEFAULT 0 COMMENT '2始终打印流转意见 0始终不打印 其它放入模板但不打印',
    `comment_sign_default` VARCHAR(255) DEFAULT NULL COMMENT '签字意见默认值',
    `tenant_id`           VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`         BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`         BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`         BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`              INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`          INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_print_def` (`def_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打印/呈批设置';
