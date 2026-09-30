-- =============================================================================
-- 存量任务业务列回填：wf_task → ACT_RU_TASK / ACT_HI_TASKINST
-- 作用：把双写开启【之前】产生的存量任务，按 wf_task 把 6 类业务列 + BIZ_TASK_ID_/BIZ_ASSIGNEE_
--       补写到 ACT_*，使校验项 ⑤「双写未覆盖」归零、存量 1:1 任务立即可被读源=act 读到。
--
-- 回填字段映射：
--   BUSINESS_STATUS_  ← CASE status: 0→TODO 2→DONE 4→FINISHED 6→AUTO_SUBMIT
--                                    7→COADJUTANT 8→CIRCULATE 11→READ
--   IS_TEST_          ← is_test
--   ORIGINAL_USER_    ← original_user（转 varchar）
--   SIGN_ORDER_       ← sign_order
--   VIEW_TIME_        ← view_time
--   TIMEOUT_HANDLED_  ← timeout_handled
--   BIZ_TASK_ID_      ← id
--   BIZ_ASSIGNEE_     ← assignee
--
-- ⚠️ 前提与范围：
--   1. 依赖 act_add_task_biz_columns.sql 已执行（8 个业务列已存在）。
--   2. 本脚本 USE blade；只读 wf_task，只 UPDATE ACT_*。
--   3. 【仅回填 1:1】同一 engine_task_id 只对应一条 wf_task 的任务。
--      N:1（自研会签，同一引擎任务多人）不回填——ACT 单行无法表达多人，
--      由 WfTaskActWriter 的 N:1 守卫跳过；这些实例应自然办结（见上线检查清单 ①）。
--   4. 幂等：仅当目标列为空时写入，可重复执行。
--   5. ⚠️ 判空必须按【各自列】，不能只看其中一列（否则先回填过部分列的行会被整体跳过）。
-- =============================================================================

USE blade;

-- ── 已办/历史：ACT_HI_TASKINST ───────────────────────────────────────────────
UPDATE ACT_HI_TASKINST h
JOIN wf_task t ON t.engine_task_id = h.ID_
SET h.BUSINESS_STATUS_ = CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE NULL END,
    h.IS_TEST_         = COALESCE(t.is_test, 0),
    h.ORIGINAL_USER_   = CAST(t.original_user AS CHAR),
    h.SIGN_ORDER_      = t.sign_order,
    h.VIEW_TIME_       = t.view_time,
    h.TIMEOUT_HANDLED_ = COALESCE(t.timeout_handled, 0),
    h.BIZ_TASK_ID_     = t.id,
    h.BIZ_ASSIGNEE_    = t.assignee
WHERE (h.BUSINESS_STATUS_ IS NULL OR h.BIZ_TASK_ID_ IS NULL OR h.BIZ_ASSIGNEE_ IS NULL)
  AND t.engine_task_id IS NOT NULL
  AND t.engine_task_id <> ''
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );

-- ── 待办：ACT_RU_TASK ───────────────────────────────────────────────────────
UPDATE ACT_RU_TASK r
JOIN wf_task t ON t.engine_task_id = r.ID_
SET r.BUSINESS_STATUS_ = CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE NULL END,
    r.IS_TEST_         = COALESCE(t.is_test, 0),
    r.ORIGINAL_USER_   = CAST(t.original_user AS CHAR),
    r.SIGN_ORDER_      = t.sign_order,
    r.VIEW_TIME_       = t.view_time,
    r.TIMEOUT_HANDLED_ = COALESCE(t.timeout_handled, 0),
    r.BIZ_TASK_ID_     = t.id,
    r.BIZ_ASSIGNEE_    = t.assignee
WHERE (r.BUSINESS_STATUS_ IS NULL OR r.BIZ_TASK_ID_ IS NULL OR r.BIZ_ASSIGNEE_ IS NULL)
  AND t.engine_task_id IS NOT NULL
  AND t.engine_task_id <> ''
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );

-- =============================================================================
-- 校验
-- =============================================================================
-- ⑤ 双写未覆盖：1:1 但 ACT_HI_TASKINST 业务列仍为空（应为 0）
SELECT '⑤ 双写未覆盖（回填后）' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NULL;

-- ⑥ 双写漂移：ACT 子状态 ≠ wf_task.status 映射（应保持 0）
SELECT '⑥ 双写漂移' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- 翻源覆盖率：wf_task 列表行 vs ACT 可读行
SELECT 'wf_task 当前列表行' AS item, COUNT(*) AS val
FROM wf_task WHERE is_test = 0 AND assignee IS NOT NULL AND status IN (0,2,4,6,7,8,11);

SELECT 'ACT 已办可读行' AS item, COUNT(*) AS val
FROM ACT_HI_TASKINST
WHERE COALESCE(IS_TEST_,0) = 0 AND COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) IS NOT NULL
  AND END_TIME_ IS NOT NULL;

SELECT 'ACT 待办可读行' AS item, COUNT(*) AS val
FROM ACT_RU_TASK
WHERE COALESCE(IS_TEST_,0) = 0 AND COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) IS NOT NULL;
