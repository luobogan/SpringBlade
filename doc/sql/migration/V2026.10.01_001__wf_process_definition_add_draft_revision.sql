-- =============================================================
-- D15 / R11：设计期草稿并发编辑防护 —— wf_process_definition 加草稿修订号
-- 出处：《去wf_表-D系列决策结论.md》D15（2026-10-01 拍板：草稿态 + 显式部署；
--       保存带乐观锁版本号防并发冲突）、分析文档 §17.3 R11。
--
-- 背景：多人并发编辑同一流程定义（画布自动保存 saveBpmn 高频落盘），
--       后保存者会静默覆盖先保存者的修改。本列为「草稿修订号」：
--       每次 saveBpmn 成功后原子 +1；客户端保存时携带 baseRevision
--       （读到的修订号），服务端条件 UPDATE（WHERE draft_revision = base）
--       不命中即判定冲突、整体回滚，前端提示刷新。
--
-- ⚠️ 语义区分：version 列是「版本组语义」（同 procKey 递增的版本号），
--    与并发修订无关，不可复用。
-- ⚠️ 必须显式 USE blade：jeelowcode 库同样存在同名表结构风险。
-- =============================================================
USE blade;

-- 1) 加列（NOT NULL + DEFAULT 0：存量行直接视为修订 0，无需回填）
ALTER TABLE wf_process_definition
  ADD COLUMN draft_revision BIGINT NOT NULL DEFAULT 0
  COMMENT '草稿修订号（D15/R11）：saveBpmn 原子递增；客户端保存携带 baseRevision 做乐观并发校验';

-- 2) 校验
SELECT COUNT(*) AS total,
       SUM(CASE WHEN draft_revision = 0 THEN 1 ELSE 0 END) AS at_zero
  FROM wf_process_definition;
