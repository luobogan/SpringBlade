-- -----------------------------------------------------------------------------
-- 迁移脚本：流程轨迹「已查看」判定 —— wf_task 增加首次查看时间
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.20_002
-- 目标库    : blade_workflow
-- 影响表    : wf_task（新增 view_time）
-- 变更类型  : DDL
-- 是否幂等  : 是（用 information_schema 判存在再 ALTER）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_task` DROP COLUMN `view_time`;
--
-- 背景
--   流程图（流程轨迹）悬浮节点要按「已操作 / 已查看 / 未操作」分组列出操作者（对齐 ecology
--   流程图节点悬浮「操作者」面板）。其中：
--     · 已操作 —— wf_task.status 已办(2)/办结(4)/自动提交(6)/协办(7)，或该节点有审批日志；
--     · 未操作 —— wf_task.status=0（待办）且从未打开过；
--     · 已查看 —— wf_task.status=0（待办）且已打开过（view_time 非空），即「看了没办」。
--   原先没有记录「打开待办」这一动作的字段，故补 view_time；由办理页打开时
--   POST /api/blade-workflow/task/{id}/view 写入（只记首次，不覆盖）。

-- wf_task.view_time
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_task' AND column_name = 'view_time');
SET @s1 = IF(@c1 = 0,
  "ALTER TABLE wf_task ADD COLUMN view_time datetime NULL COMMENT '首次查看时间（流程轨迹「已查看」判定）' AFTER operate_time",
  'SELECT 1');
PREPARE stmt FROM @s1; EXECUTE stmt; DEALLOCATE PREPARE stmt;
