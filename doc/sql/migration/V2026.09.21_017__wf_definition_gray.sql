-- =============================================================================
-- 迁移 017：流程定义灰度规则表
--
-- 背景（见 doc/md/流程测试与生产上线隔离方案.md §4.3「灰度实现（最小可用设计）」）
--   新版本上线不能一刀切全量：先对白名单/小比例生效，观察无异常再逐步放大，
--   出问题把规则置为停用即可**秒级切回**旧版本（在途实例不受影响 ——
--   Flowable 实例原生绑定创建时的 ACT_RE_PROCDEF.ID_）。
--
-- 规则载体（落库而非 Nacos：便于审计「谁在何时对哪个流程开了多少灰度」）
--   def_id            流程定义ID（版本组锚点）
--   base_proc_def_id  正式版本（灰度兜底，停用/未命中时用它）
--   gray_proc_def_id  灰度版本（命中时用它）
--   strategy          whitelist=白名单 / ratio=比例 / dept=部门 / role=角色
--   ratio             ratio 策略下的百分比（1-100）
--   scope_value       白名单用户（或部门/角色）ID，逗号分隔
--   status            1启用 0停用（置 0 = 立即全量回退）
--
-- 幂等：CREATE TABLE IF NOT EXISTS + 尾部校验
-- =============================================================================

CREATE TABLE IF NOT EXISTS `wf_definition_gray` (
  `id`               BIGINT       NOT NULL COMMENT '主键',
  `tenant_id`        VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `def_id`           BIGINT       NOT NULL COMMENT '流程定义ID（版本组锚点）',
  `base_proc_def_id` VARCHAR(64)  NOT NULL COMMENT '正式版本 processDefinitionId（灰度兜底）',
  `gray_proc_def_id` VARCHAR(64)  NOT NULL COMMENT '灰度版本 processDefinitionId',
  `strategy`         VARCHAR(16)  NOT NULL DEFAULT 'whitelist' COMMENT 'whitelist=白名单 / ratio=比例 / dept=部门 / role=角色',
  `ratio`            INT          NULL     COMMENT 'ratio 策略下的百分比（1-100）',
  `scope_value`      VARCHAR(512) NULL     COMMENT '白名单用户/部门/角色ID，逗号分隔',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用（置0=立即回退正式版本）',
  `start_time`       DATETIME     NULL     COMMENT '灰度开始时间',
  `create_user`      BIGINT       NULL COMMENT '创建人',
  `create_dept`      BIGINT       NULL COMMENT '创建部门',
  `create_time`      DATETIME     NULL COMMENT '创建时间',
  `update_user`      BIGINT       NULL COMMENT '修改人',
  `update_time`      DATETIME     NULL COMMENT '修改时间',
  `is_deleted`       INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_gray_def` (`def_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义灰度规则';

-- -----------------------------------------------------------------------------
-- 校验（期望：表存在且结构完整）
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS has_table
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blade_workflow' AND TABLE_NAME = 'wf_definition_gray';

SELECT COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blade_workflow' AND TABLE_NAME = 'wf_definition_gray'
  AND COLUMN_NAME IN ('def_id','base_proc_def_id','gray_proc_def_id','strategy','ratio','scope_value','status');

-- =============================================================================
-- 回滚脚本（如需撤销，取消注释执行）
--   DROP TABLE IF EXISTS blade_workflow.wf_definition_gray;
-- =============================================================================
