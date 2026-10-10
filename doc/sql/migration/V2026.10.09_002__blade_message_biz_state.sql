-- V2026.10.09_002__blade_message_biz_state.sql
-- 流程消息集成二期（消息中心设计文档 §10.8 T9，对齐 ecology updateBizState）：
--   blade_message 增加 biz_state 列：通知业务状态。
--     NULL = 待处理/进行中（TASK_CREATED 待办通知的初始态）
--     1    = 已处理（审批同意后由 blade-workflow 回写，WF_TASK 通知）
--     2    = 已办结（PROCESS_COMPLETED 办结通知自带，WF_INSTANCE 通知）
--   与 idx_blade_message_biz_ref 配合完成"按业务引用回写历史通知状态"。
-- 幂等：列已存在则跳过，可重复执行。

SET @db = DATABASE();

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'blade_message'
      AND COLUMN_NAME = 'biz_state'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE blade_message ADD COLUMN biz_state INT NULL COMMENT ''通知业务状态 NULL=待处理 1=已处理 2=已办结'' AFTER category',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
