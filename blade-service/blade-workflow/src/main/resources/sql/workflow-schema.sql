-- =============================================================================
-- 审批流程模块（blade-workflow）建表脚本
--
-- 依据：《审批流程方案-现状分析与技术选型.md》v1.0 第 5 章「数据模型设计」
-- 表名前缀：wf_*（已拍板；不使用 workflow_*，避免与 ecology 迁移源表 workflow_base 等撞名）
-- 部署库：blade_workflow（与 Flowable 引擎 ACT_* 表同库，但 wf_* 不依赖 ACT_*，仅弱关联）
--
-- 设计约定：
--   1. 主键 BIGINT UNSIGNED AUTO_INCREMENT（窄、单调，避免随机值做聚簇主键）
--   2. 统一 utf8mb4 / utf8mb4_0900_ai_ci；时间用 DATETIME 而非 TIMESTAMP
--   3. 复合索引遵循最左前缀：等值列在前，范围/排序列在后
--   4. 审计字段对齐 CLAUDE.md §7 与 org.springblade.core.mp.base.TenantEntity
--      （id / tenant_id / create_user / create_dept / create_time / update_user /
--        update_time / status / is_deleted），实体统一继承 TenantEntity，
--      因此每张表都必须是完整的审计列集合，否则 MyBatis-Plus 会因列缺失报错。
--   5. 大 JSON（布局快照、表单数据）独立列/独立表，避免撑大主表热点行
--   6. 无外键约束（跨库/跨模块引用靠逻辑关联，与 ecology 一致）
--   7. 脚本可重复执行（先 DROP 再 CREATE）
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `blade_workflow`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `blade_workflow`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `wf_migration_map`;
DROP TABLE IF EXISTS `wf_form_snapshot`;
DROP TABLE IF EXISTS `wf_approval_log`;
DROP TABLE IF EXISTS `wf_task`;
DROP TABLE IF EXISTS `wf_instance`;
DROP TABLE IF EXISTS `wf_node_detail_perm`;
DROP TABLE IF EXISTS `wf_node_field_perm`;
DROP TABLE IF EXISTS `wf_node_link`;
DROP TABLE IF EXISTS `wf_node_operator`;
DROP TABLE IF EXISTS `wf_process_node`;
DROP TABLE IF EXISTS `wf_process_definition`;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 5.1 流程定义域
-- =============================================================================

-- 流程定义
CREATE TABLE `wf_process_definition` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `proc_key`      VARCHAR(64)  NOT NULL                COMMENT '引擎流程Key（= BPMN process id）',
    `form_id`       BIGINT       NOT NULL                COMMENT '关联 workflow_bill.id（表单）',
    `name`          VARCHAR(200) NOT NULL                COMMENT '流程名称',
    `bpmn_xml`      MEDIUMTEXT   NULL                    COMMENT 'BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）',
    `version`       INT          NOT NULL DEFAULT 1      COMMENT '版本号',
    `is_free`       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否自由流程',
    `type`          VARCHAR(64)  NULL                    COMMENT '路径类型（对齐 ecology path_type 字典 code）',
    `form_type`     TINYINT      NULL                    COMMENT '对应表单类型：0自定义表单 1系统表单',
    `description`   VARCHAR(500) NULL                    COMMENT '路径描述',
    `sort_order`    INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 0      COMMENT '0草稿 1已发布 2停用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除:1已删 0未删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_proc_key_version` (`proc_key`, `version`),
    KEY `idx_form_status` (`form_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义';

-- 流程节点（对齐 ecology workflow_flownode / workflow_nodebase）
CREATE TABLE `wf_process_node` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `def_id`        BIGINT UNSIGNED NOT NULL                COMMENT '流程定义ID',
    `node_key`      VARCHAR(64)  NOT NULL                COMMENT '引擎节点ID',
    `node_name`     VARCHAR(200) NOT NULL                COMMENT '节点名称',
    `node_type`     TINYINT      NOT NULL                COMMENT '0创建 1审批 2提交 3归档 5等待 6自动处理（对齐 ecology NodeType）',
    `sign_order`    TINYINT      NOT NULL DEFAULT 0      COMMENT '0或签 1会签 2依次 3抄送不需提交 4抄送需提交（对齐 SignOrder）',
    `merge_type`    TINYINT      NOT NULL DEFAULT 0      COMMENT '0普通 1分叉起点 2分叉中间 3按分支数合并 4指定分支合并 5比例合并（对齐 nodeattribute）',
    `pass_num`      INT          NOT NULL DEFAULT 0      COMMENT '合并阈值：分支数或百分比（对齐 passnum）',
    `allow_reject`  TINYINT      NOT NULL DEFAULT 1      COMMENT '允许退回',
    `allow_forward` TINYINT      NOT NULL DEFAULT 0      COMMENT '允许转发/转办',
    `auto_approve`  TINYINT      NOT NULL DEFAULT 0      COMMENT '自动批准',
    `sort_order`    INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `ext_json`      JSON         NULL                    COMMENT '扩展属性（超时、提醒、签章等）',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_def_node` (`def_id`, `node_key`),
    KEY `idx_def_sort` (`def_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点';

-- 节点操作者（对齐 ecology workflow_groupdetail）
CREATE TABLE `wf_node_operator` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `node_id`       BIGINT UNSIGNED NOT NULL                COMMENT '流程节点ID',
    `group_no`      INT          NOT NULL DEFAULT 1      COMMENT '操作组序号',
    `op_type`       INT          NOT NULL                COMMENT '操作者类型（对齐 OperatorDBType：3人员 1部门 30分部 2角色 58岗位 4所有人 / 17创建人本人 18创建人上级 19本部门 / 40本人 41上级 / 5字段-人员 6字段-人员上级 42字段-部门 43字段-角色 / 99矩阵 97接口 98SQL）',
    `obj_id`        VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '对象ID或表单字段名',
    `level_min`     INT          NULL COMMENT '安全级别下限（对齐 LEVEL_N）',
    `level_max`     INT          NULL COMMENT '安全级别上限（对齐 LEVEL2_N）',
    `bhxj`          TINYINT      NOT NULL DEFAULT 0      COMMENT '0本部 1含下级 2含上级 3逐级向上（对齐 BHXJ）',
    `sign_order`    TINYINT      NOT NULL DEFAULT 0      COMMENT '会签关系（对齐 SIGNORDER）',
    `batch_no`      INT          NOT NULL DEFAULT 0      COMMENT '批次（依次审批顺序，对齐 ORDERS）',
    `condition_json` JSON        NULL                    COMMENT '操作者生效条件',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_node_group` (`node_id`, `group_no`),
    KEY `idx_node_type` (`node_id`, `op_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点操作者';

-- 出口（连线），对齐 ecology workflow_nodelink
CREATE TABLE `wf_node_link` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `def_id`        BIGINT UNSIGNED NOT NULL                COMMENT '流程定义ID',
    `from_node_key` VARCHAR(64)  NOT NULL                COMMENT '源节点',
    `to_node_key`   VARCHAR(64)  NOT NULL                COMMENT '目标节点',
    `is_reject`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否退回线（对齐 ISREJECT）',
    `is_must_pass`  TINYINT      NOT NULL DEFAULT 0      COMMENT '分叉必经分支（对齐 ISMUSTPASS）',
    `condition_expr` TEXT         NULL                    COMMENT '条件表达式',
    `condition_cn`  VARCHAR(1000) NOT NULL DEFAULT ''    COMMENT '条件中文描述',
    `sort_order`    INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_def_from` (`def_id`, `from_node_key`),
    KEY `idx_def_reject` (`def_id`, `is_reject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程出口（连线）';

-- =============================================================================
-- 5.2 权限域（与 Excel 布局直接耦合）
-- =============================================================================

-- 节点级字段权限（对齐 ecology workflow_nodeform，按本项目 scope 体系落地）
-- scope 与 data-excelp-scope 对齐：main | dt{idx} | dt{idx}_r{row}（行级；回退：行级 → 明细表级 → 主表级）
-- perm 对齐 ecology fieldattr：0隐藏 1只读 2可编辑 3必填
CREATE TABLE `wf_node_field_perm` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `def_id`        BIGINT UNSIGNED NOT NULL                COMMENT '流程定义ID',
    `node_key`      VARCHAR(64)  NOT NULL                COMMENT '节点Key',
    `scope`         VARCHAR(32)  NOT NULL                COMMENT 'main | dt{idx} | dt{idx}_r{row}（与 data-excelp-scope 对齐）',
    `field_name`    VARCHAR(128) NOT NULL                COMMENT '字段名（与 data-excelp-field 对齐）',
    `perm`          TINYINT      NOT NULL DEFAULT 2      COMMENT '0隐藏 1只读 2可编辑 3必填（对齐 ecology fieldattr）',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_scope_field` (`def_id`, `node_key`, `scope`, `field_name`),
    KEY `idx_def_node` (`def_id`, `node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点级字段权限矩阵';

-- 节点级明细表权限（对齐 ecology workflow_nodeformgroup 的 10 位 detailgroupattr）
CREATE TABLE `wf_node_detail_perm` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `def_id`        BIGINT UNSIGNED NOT NULL                COMMENT '流程定义ID',
    `node_key`      VARCHAR(64)  NOT NULL                COMMENT '节点Key',
    `dt_index`      INT          NOT NULL                COMMENT '明细表序号',
    `can_add`       TINYINT      NOT NULL DEFAULT 1      COMMENT '可新增行',
    `can_edit`      TINYINT      NOT NULL DEFAULT 1      COMMENT '可编辑行',
    `can_delete`    TINYINT      NOT NULL DEFAULT 1      COMMENT '可删除行',
    `hide_empty`    TINYINT      NOT NULL DEFAULT 0      COMMENT '隐藏空行',
    `default_rows`  INT          NOT NULL DEFAULT 1      COMMENT '默认行数',
    `required`      TINYINT      NOT NULL DEFAULT 0      COMMENT '必须至少一条',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1      COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_dt` (`def_id`, `node_key`, `dt_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点级明细表权限';

-- =============================================================================
-- 5.3 运行实例域
-- =============================================================================

-- 流程实例
CREATE TABLE `wf_instance` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `engine_inst_id`    VARCHAR(64)  NOT NULL DEFAULT ''    COMMENT '引擎实例ID（Flowable PROC_INST_ID_），弱关联，不依赖 ACT_* 表',
    `def_id`            BIGINT UNSIGNED NOT NULL            COMMENT '流程定义ID',
    `form_id`           BIGINT       NOT NULL                COMMENT '表单ID（workflow_bill.id）',
    `data_id`           BIGINT       NOT NULL                COMMENT '业务数据ID（formtable_main_{id}.id）',
    `title`             VARCHAR(400) NOT NULL DEFAULT ''    COMMENT '流程标题',
    `biz_key`           VARCHAR(128) NOT NULL                COMMENT '业务主键 formId:dataId，便于反查',
    `current_node_key`  VARCHAR(64)  NOT NULL DEFAULT ''    COMMENT '当前节点',
    `starter`           BIGINT       NOT NULL                COMMENT '发起人',
    `start_time`        DATETIME     NOT NULL                COMMENT '发起时间',
    `end_time`          DATETIME     NULL                    COMMENT '结束时间',
    `urgency`           TINYINT      NOT NULL DEFAULT 0      COMMENT '紧急程度 0/1/2（对齐 requestlevel）',
    `parent_id`         BIGINT UNSIGNED NULL                 COMMENT '父流程实例（子流程）',
    `tenant_id`         VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`       BIGINT       NULL COMMENT '创建人',
    `create_dept`       BIGINT       NULL COMMENT '创建部门',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`       BIGINT       NULL COMMENT '修改人',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`            INT          NOT NULL DEFAULT 0      COMMENT '0运行中 1通过 2不通过 3撤销 4暂停（对齐 ecology currentstatus 语义）',
    `is_deleted`        INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_engine_inst` (`engine_inst_id`),
    UNIQUE KEY `uk_biz_key` (`biz_key`),
    KEY `idx_status_start` (`status`, `start_time`),
    KEY `idx_starter_status` (`starter`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例';

-- 任务（待办/已办）
-- status 对齐 ecology workflow_currentoperator.isremark：
-- 0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅
CREATE TABLE `wf_task` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inst_id`           BIGINT UNSIGNED NOT NULL            COMMENT '流程实例ID',
    `engine_task_id`    VARCHAR(64)  NOT NULL DEFAULT ''    COMMENT '引擎任务ID（Flowable），弱关联',
    `node_key`          VARCHAR(64)  NOT NULL                COMMENT '节点Key',
    `assignee`          BIGINT       NOT NULL                COMMENT '办理人',
    `original_user`     BIGINT       NULL                    COMMENT '代理人代办时的原处理人',
    `sign_order`        TINYINT      NOT NULL DEFAULT 0      COMMENT '会签关系',
    `receive_time`      DATETIME     NULL                    COMMENT '接收时间',
    `operate_time`      DATETIME     NULL                    COMMENT '处理时间',
    `due_time`          DATETIME     NULL                    COMMENT '截止时间',
    `tenant_id`         VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`       BIGINT       NULL COMMENT '创建人',
    `create_dept`       BIGINT       NULL COMMENT '创建部门',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`       BIGINT       NULL COMMENT '修改人',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`            INT          NOT NULL DEFAULT 0      COMMENT '0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅（对齐 isremark）',
    `is_deleted`        INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_assignee_status` (`assignee`, `status`),
    KEY `idx_inst_node` (`inst_id`, `node_key`),
    KEY `idx_engine_task` (`engine_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务（待办/已办）';

-- 审批/流转记录（大表，按月 RANGE 分区）
-- log_type 对齐 ecology RequestLogType：
-- 0批准 2提交 3退回 7转发 9批注 h转办 s督办 t抄送 y批示；正常流转仅认定 SUBMIT(2) 与 APPROVE(0)
-- 注意：分区列 operate_time 必须纳入主键；p_max 为兜底分区，运维需提前补充后续月份分区
CREATE TABLE `wf_approval_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inst_id`       BIGINT UNSIGNED NOT NULL                COMMENT '流程实例ID',
    `task_id`       BIGINT UNSIGNED NULL                    COMMENT '任务ID',
    `node_key`      VARCHAR(64)  NOT NULL DEFAULT ''      COMMENT '节点Key',
    `operator`      BIGINT       NOT NULL                  COMMENT '操作人',
    `log_type`      VARCHAR(2)   NOT NULL                  COMMENT '0批准 2提交 3退回 7转发 9批注 h转办 s督办 t抄送 y批示（对齐 RequestLogType）',
    `opinion`       VARCHAR(2000) NOT NULL DEFAULT ''      COMMENT '审批意见',
    `operate_time`  DATETIME     NOT NULL                  COMMENT '操作时间（分区列）',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1        COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    PRIMARY KEY (`id`, `operate_time`),
    KEY `idx_inst_time` (`inst_id`, `operate_time`),
    KEY `idx_operator_time` (`operator`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流转记录'
PARTITION BY RANGE COLUMNS(`operate_time`) (
    PARTITION `p202609` VALUES LESS THAN ('2026-10-01') ENGINE = InnoDB,
    PARTITION `p202610` VALUES LESS THAN ('2026-11-01') ENGINE = InnoDB,
    PARTITION `p202611` VALUES LESS THAN ('2026-12-01') ENGINE = InnoDB,
    PARTITION `p202612` VALUES LESS THAN ('2027-01-01') ENGINE = InnoDB,
    PARTITION `p_max`   VALUES LESS THAN (MAXVALUE)     ENGINE = InnoDB
);

-- =============================================================================
-- 5.4 表单集成域
-- =============================================================================

-- 表单数据快照（发起/每次提交留痕，与布局解耦）
-- data_json key 沿用 {sheetId}__{row}__{col} / dt{idx}__r{n}__...
CREATE TABLE `wf_form_snapshot` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `inst_id`       BIGINT UNSIGNED NOT NULL                COMMENT '流程实例ID',
    `node_key`      VARCHAR(64)  NOT NULL DEFAULT ''      COMMENT '节点Key',
    `layout_id`     BIGINT       NULL                      COMMENT '当时的 form_layout.id（布局快照）',
    `data_json`     JSON         NOT NULL                  COMMENT '主表+明细全量值',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 1        COMMENT '状态:1正常 0禁用',
    `is_deleted`    INT          NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_inst_node` (`inst_id`, `node_key`),
    KEY `idx_inst_time` (`inst_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单数据快照';

-- =============================================================================
-- 5.5 迁移域
-- =============================================================================

-- ecology 存量映射（迁移与回滚的核心凭据）
CREATE TABLE `wf_migration_map` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ec_wf_id`      BIGINT       NOT NULL                  COMMENT 'ecology workflow_base.id',
    `ec_request_id` BIGINT       NULL                      COMMENT 'ecology workflow_requestbase.requestid（在途实例）',
    `ec_node_id`    BIGINT       NULL                      COMMENT 'ecology 节点ID',
    `new_def_id`    BIGINT UNSIGNED NULL                   COMMENT '新流程定义ID',
    `new_inst_id`   BIGINT UNSIGNED NULL                   COMMENT '新流程实例ID',
    `new_node_key`  VARCHAR(64)  NULL                      COMMENT '新节点Key',
    `migrate_time`  DATETIME     NOT NULL                  COMMENT '迁移时间',
    `err_msg`       VARCHAR(1000) NOT NULL DEFAULT ''      COMMENT '错误信息',
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`   BIGINT       NULL COMMENT '创建人',
    `create_dept`   BIGINT       NULL COMMENT '创建部门',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`   BIGINT       NULL COMMENT '修改人',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`        INT          NOT NULL DEFAULT 0        COMMENT '0待迁移 1成功 2失败',
    `is_deleted`    INT          NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ec_wf` (`ec_wf_id`, `ec_node_id`),
    KEY `idx_ec_request` (`ec_request_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ecology 存量迁移映射';

-- =============================================================================
-- 流程（路径）类型：浏览框 wftype 数据源（对齐 ecology workflow_type）
-- =============================================================================
CREATE TABLE IF NOT EXISTS `wf_workflow_type` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type_name`   VARCHAR(100) NOT NULL                COMMENT '类型名称',
    `type_desc`   VARCHAR(500) NULL                   COMMENT '类型描述',
    `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user` BIGINT       NULL                   COMMENT '创建人',
    `create_dept` BIGINT       NULL                   COMMENT '创建部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT       NULL                   COMMENT '修改人',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      INT          NOT NULL DEFAULT 1      COMMENT '0停用 1启用',
    `is_deleted`  INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除:1已删 0未删',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程（路径）类型（浏览框 wftype 数据源）';

INSERT IGNORE INTO `wf_workflow_type` (`id`, `type_name`, `type_desc`, `sort_order`) VALUES
    (1, '行政审批', '行政审批类流程', 1),
    (2, '人事流程', '招聘、入转调离、考勤等', 2),
    (3, '财务流程', '报销、预算、付款等', 3),
    (4, 'IT流程',  '系统权限、资源申请、运维', 4),
    (5, '业务流程', '各业务运营审批', 5);
