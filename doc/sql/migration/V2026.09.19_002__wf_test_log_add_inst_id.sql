-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_test_log 增加 inst_id（交互式测试关联测试态实例）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.19_002
-- 目标库    : blade_workflow（wf_test_log 所在库）
-- 影响表    : wf_test_log（新增列）
-- 变更类型  : DDL
-- 是否幂等  : 是（先查 information_schema，列已存在则跳过）
-- 是否丢数据: 否（仅新增可空列）
-- 回滚脚本  :
--   ALTER TABLE `wf_test_log` DROP COLUMN `inst_id`;
--
-- 背景
--   新增交互式「流程测试」（对齐 ecology 流程测试页）：
--   「开始测试」发起测试态实例后，可选择「开始自动测试（可暂停）」逐节点推进，
--   或手动填写表单后「提交」单步办理，直至归档。
--   为让一次交互式测试的全过程在「测试历史」中可见并能去重，
--   需要把测试态实例ID（wf_instance.id，is_test=1）记录到 wf_test_log.inst_id。
--   一次性测试（POST /test/run）不写该列（保持为 NULL）。
--
-- 注意
--   本项目无 Flyway/Liquibase，schema 变更长期靠手工执行，
--   请在目标库 blade_workflow 上直接执行本文件。

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_test_log'
      AND COLUMN_NAME = 'inst_id'
);

SET @ddl := IF(@col_exists > 0,
    'SELECT ''wf_test_log.inst_id 已存在，跳过'' AS result',
    'ALTER TABLE `wf_test_log` ADD COLUMN `inst_id` BIGINT NULL COMMENT ''测试态实例ID（交互式测试；一次性测试为 NULL）'' AFTER `result_json`');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
