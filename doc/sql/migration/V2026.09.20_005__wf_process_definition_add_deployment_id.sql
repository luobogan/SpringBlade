-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_process_definition 补「正式部署ID」列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.20_005
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_process_definition` DROP COLUMN `deployment_id`;
--
-- 背景（规范 §1.5 风险 3 / §5 P2）
--   deploy() 此前丢弃了 deployProcess() 返回的引擎部署ID，导致"引擎 latest 是否 = 正式部署"
--   只能靠部署时间、BPMN 消毒标记（manualTask）间接判读，无法精确比对。
--   落库后：
--     ① 巡检脚本可直接 JOIN：wf_process_definition.deployment_id == ACT_RE_PROCDEF.DEPLOYMENT_ID_
--        （同 procKey 的 latest 版本）→ 判断是否被测试部署 / 手工部署顶替；
--     ② 发起自检给出 engine_deployment_matched 标志（见 V2026.09.20_006）。
--
--   存量数据兼容：列加好后 deployment_id 全为 NULL（= 未落库 / 未知），
--   自检标志相应为 NULL（**不会误报**）；重新「发布」（deploy）一次即回填。
--   测试部署走独立 key（procKey + "__test"），**不写本列**（本列语义 = 正式部署）。
--   「另存为新版本」时置空：新版本是未部署的草稿。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启 blade-workflow。
-- ⚠️ 严禁使用 blade-service/blade-workflow/src/main/resources/sql/workflow-schema.sql
--    来"同步结构"——该脚本会 DROP TABLE 后重建，清空全部业务数据。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_process_definition';

-- deployment_id --------------------------------------------------------------
SET @v_col = 'deployment_id';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `deployment_id` VARCHAR(64) NULL
         COMMENT ''最近一次正式部署的引擎部署ID（ACT_RE_DEPLOYMENT.ID_）；NULL=尚未正式部署/未落库''
         AFTER `active_version_id`',
    'SELECT ''SKIP: wf_process_definition.deployment_id already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（应返回 1）--------------------------------------------------------------
SELECT COUNT(1) AS deployment_id_should_be_1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND COLUMN_NAME  = 'deployment_id';

-- 落库情况（执行后应全为 NULL；重新发布一条流程后该行会出现部署ID）----------------
SELECT id, name, proc_key, version, status, deployment_id
FROM wf_process_definition
WHERE is_deleted = 0 AND status = 1
ORDER BY id DESC LIMIT 5;
