-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_instance 新增 pending_status（期望终态 intent 列）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.27_001
-- 目标库    : blade_workflow
-- 影响表    : wf_instance（新增列）
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（information_schema 守卫 + PREPARE 动态 SQL，重复执行不会报错）
-- 是否丢数据: 否（仅新增可空列，不动既有数据）
-- 回滚脚本  : ALTER TABLE `wf_instance` DROP COLUMN `pending_status`;
--
-- 背景：方案C 阶段3 把「终止/撤回/撤销」的 wf_instance.status 改为由
--   PROCESS_CANCELLED 引擎事件反写；但该事件只有「取消」一种语义，无法区分
--   「不通过(2)」与「撤销(3)」。故新增本列承载「期望终态(intent)」：
--   业务在调用引擎终结（deleteProcessInstance）**之前**写入 2 或 3，
--   事件侧（WfStateProjector#writeCancel）按 processInstanceId 查到该行后读取本列
--   派生正确终态，应用后清空。
--
--   这样监听仍只依赖 wf_* Mapper、不注入引擎 Service、不回查 ACT_RU_*
--   （治理文档 §4.2 / §4.5），同时让「不通过(2)」可被正确派生。
--
-- 取值约定：NULL = 无 intent（按默认撤销 3 处理）；1 通过 / 2 不通过 / 3 撤销。
-- =============================================================================

USE `blade_workflow`;

-- 1) 新增 pending_status 列（幂等：已存在则跳过）--------------------------------
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'wf_instance'
        AND COLUMN_NAME = 'pending_status'
    ),
    'SELECT ''pending_status 已存在，跳过'' AS info',
    'ALTER TABLE `wf_instance` ADD COLUMN `pending_status` TINYINT NULL DEFAULT NULL COMMENT ''期望终态(intent)：由业务在引擎终结前写入，供 PROCESS_CANCELLED 事件派生（1通过/2不通过/3撤销）；应用后清空'''
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 校验 ----------------------------------------------------------------------
SELECT
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_DEFAULT,
  COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wf_instance'
  AND COLUMN_NAME = 'pending_status';
