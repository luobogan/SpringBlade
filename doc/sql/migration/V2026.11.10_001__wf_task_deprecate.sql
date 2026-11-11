-- =============================================================================
-- wf_task 表级「废弃标记」（去 wf_ 表 · 批次 1 · 2026-11-10）
--
-- 背景：批次 1 完成后 wf_task 运行时零写入（任务 id 预生成 + 业务列直写 ACT，
--       操作链按 BIZ_TASK_ID_ 反查 ACT），表与 Entity/Mapper/Service 按用户决策
--       「仅打废弃标记、不物理删除」保留，供存量追溯与 legacy 回退。
--
-- 设计：只改表 COMMENT，不改任何列/索引/数据，零结构变更、可重复执行（幂等）。
--       这是本项目首个「表级废弃标记」约定，需在 D 系列决策登记。
--
-- 回滚（如需恢复旧注释，按实际旧值填写）：
--   ALTER TABLE wf_task COMMENT = '流程任务';
-- =============================================================================

USE blade;

ALTER TABLE wf_task
  COMMENT = '【已废弃·P6 2026-11-10】任务台账，运行时零写入；仅保留存量行供追溯与 task-ledger-mode=legacy 回退。运行态以 ACT_RU_TASK/ACT_HI_TASKINST 为唯一事实源，扩展任务见 wf_ext_task。不物理删除。';

-- 只读复核（可选）：
-- SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_task';
