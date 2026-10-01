USE blade;

-- ============================================================
-- V2026.10.01_003__act_backfill_task_tenant.sql
-- T-13 多租户隔离前置：回填 ACT_RU_TASK / ACT_HI_TASKINST 的 TENANT_ID_
--
-- 背景（2026-10-01 T-13 实测）：
--   去 wf_ 表后任务读源已切 ACT_*，但引擎侧 TENANT_ID_ 从未赋值 ——
--   ACT_RU_TASK 205 行 TENANT_ID_ 全空，而 wf_task.tenant_id 恒为 000000；
--   ACT_HI_PROCINST.TENANT_ID_ 已在 T-7 回填（111 行 000000 + 1 孤儿为空）。
--   结果：ACT 任务读源【没有可过滤的租户列】，多租户场景下会串租户。
--   当前 dev 工作流数据恰为单租户 000000，靠 assignee 隐式隔离才未暴露。
--
-- 推导优先级：
--   ① PROC_INST_ID_  → ACT_HI_PROCINST.TENANT_ID_（主路径，RU 205/205、HI 291/294 命中）
--   ② BIZ_TASK_ID_   → wf_task.tenant_id          （兜底，覆盖无实例的历史任务）
--   ③ 常量 '000000'                                （最终兜底，保证不留空）
--
-- 幂等：仅填充【为空】的行，已有值不覆盖；可重复执行。
-- 回滚：本脚本只补空值、不改已有值；如需还原，将 TENANT_ID_ 置回 NULL 即可，无结构变更。
-- 依赖：无（TENANT_ID_ 为 Flowable 原生列，两表均已建索引）。
-- ============================================================

-- ---------- 迁移前校验 ----------
SELECT 'BEFORE' AS phase,
       (SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')    AS ru_empty,
       (SELECT COUNT(*) FROM ACT_RU_TASK)                                                 AS ru_total,
       (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '') AS hi_empty,
       (SELECT COUNT(*) FROM ACT_HI_TASKINST)                                             AS hi_total;

-- ---------- 回填 ACT_RU_TASK ----------
UPDATE ACT_RU_TASK t
    LEFT JOIN ACT_HI_PROCINST p ON t.PROC_INST_ID_ = p.PROC_INST_ID_
    LEFT JOIN wf_task w         ON t.BIZ_TASK_ID_ = w.id
SET t.TENANT_ID_ = COALESCE(NULLIF(p.TENANT_ID_, ''), NULLIF(w.tenant_id, ''), '000000')
WHERE (t.TENANT_ID_ IS NULL OR t.TENANT_ID_ = '');

-- ---------- 回填 ACT_HI_TASKINST ----------
UPDATE ACT_HI_TASKINST t
    LEFT JOIN ACT_HI_PROCINST p ON t.PROC_INST_ID_ = p.PROC_INST_ID_
    LEFT JOIN wf_task w         ON t.BIZ_TASK_ID_ = w.id
SET t.TENANT_ID_ = COALESCE(NULLIF(p.TENANT_ID_, ''), NULLIF(w.tenant_id, ''), '000000')
WHERE (t.TENANT_ID_ IS NULL OR t.TENANT_ID_ = '');

-- ---------- 迁移后校验（期望 ru_empty = 0、hi_empty = 0）----------
SELECT 'AFTER' AS phase,
       (SELECT COUNT(*) FROM ACT_RU_TASK WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')    AS ru_empty,
       (SELECT COUNT(*) FROM ACT_RU_TASK)                                                 AS ru_total,
       (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '') AS hi_empty,
       (SELECT COUNT(*) FROM ACT_HI_TASKINST)                                             AS hi_total;

-- ---------- 回填后租户分布 ----------
SELECT 'ACT_RU_TASK' AS tbl, COALESCE(NULLIF(TENANT_ID_, ''), '(空)') AS tenant, COUNT(*) AS c
FROM ACT_RU_TASK GROUP BY TENANT_ID_
UNION ALL
SELECT 'ACT_HI_TASKINST' AS tbl, COALESCE(NULLIF(TENANT_ID_, ''), '(空)') AS tenant, COUNT(*) AS c
FROM ACT_HI_TASKINST GROUP BY TENANT_ID_;
