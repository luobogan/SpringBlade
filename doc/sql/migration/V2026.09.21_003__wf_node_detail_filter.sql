-- -----------------------------------------------------------------------------
-- 迁移脚本：新建「节点级明细表字段筛选」表 wf_node_detail_filter
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_003
-- 目标库    : blade_workflow
-- 影响表    : wf_node_detail_filter（新建）
-- 变更类型  : 建表（DDL）
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否（新建空表）
-- 回滚脚本  : DROP TABLE IF EXISTS `wf_node_detail_filter`;
--
-- 背景（对齐 ecology「明细表数据根据操作者筛选显示」）：
--   节点可配置按明细字段值过滤明细行——仅当某行满足全部筛选规则时才对该操作者
--   「显示 / 打印」出来。支持两套口径（显示 / 打印）与四种比较方式（等于 / 不等于 /
--   包含 / 不包含）。多条规则间为「且」关系。
-- =============================================================================

USE `blade_workflow`;

CREATE TABLE IF NOT EXISTS `wf_node_detail_filter` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `tenant_id`     VARCHAR(50)  NOT NULL DEFAULT '000000',
  `def_id`        BIGINT       NOT NULL,
  `node_key`      VARCHAR(100) NOT NULL,
  `dt_index`      INT          NOT NULL COMMENT '明细表序号',
  `mode_type`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=显示时过滤 2=打印时过滤',
  `field_name`    VARCHAR(100) NOT NULL COMMENT '比较字段（明细列的 fieldName）',
  `compare_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '1等于 2不等于 3包含 4不包含',
  `compare_value` VARCHAR(500) NULL     COMMENT '比较值（多值用逗号分隔）',
  `is_required`   TINYINT      NOT NULL DEFAULT 0 COMMENT '过滤后要求至少一条 1=是 0=否',
  `create_time`   DATETIME     DEFAULT NULL,
  `create_user`   BIGINT       DEFAULT NULL,
  `create_dept`   BIGINT       DEFAULT NULL COMMENT '创建部门',
  `update_time`   DATETIME     DEFAULT NULL,
  `update_user`   BIGINT       DEFAULT NULL,
  `status`        INT          DEFAULT 1 COMMENT '业务状态[1:正常]',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除[0:未删除,1:删除]',
  PRIMARY KEY (`id`),
  KEY `idx_wf_detail_filter_def_node` (`def_id`, `node_key`, `mode_type`, `dt_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='节点级明细表字段筛选（显示/打印两套 + 4 种比较）';

-- 校验 ------------------------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter') AS table_should_be_1;
