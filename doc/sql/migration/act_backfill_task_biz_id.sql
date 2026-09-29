-- =============================================================================
-- 回填 ACT_RU_TASK / ACT_HI_TASKINST 的 BIZ_TASK_ID_（= wf_task.id）
-- 作用：待办/已办翻源到 ACT_* 后，列表必须继续吐【blade 业务任务ID】——
--       操作接口（approve / 转办 / 退回 / 查看）一律走
--       requireTodoTask(taskId) → taskMapper.selectById(taskId) 按 wf_task.id 查，
--       若列表吐引擎任务 ID_ 会导致「点同意查不到任务」。故本列是翻源的硬依赖。
--
-- ⚠️ 前提与范围：
--   1. 依赖 act_add_task_biz_columns.sql 已执行（BIZ_TASK_ID_ 列已存在）。
--   2. 本脚本 USE blade；只读 wf_task，只 UPDATE ACT_* 的 BIZ_TASK_ID_。
--   3. 仅回填【1:1】任务（同一 engine_task_id 只对应一条 wf_task）。
--      N:1（会签/或签自研模式，同一引擎任务多人）不回填——ACT 单行无法表达多人，
--      由 WfTaskActWriter 的 N:1 守卫跳过；待这些实例自然办结即可。
--   4. 幂等：仅当 BIZ_TASK_ID_ IS NULL 时写入，可重复执行。
-- =============================================================================

USE blade;

-- 已办/历史（ACT_HI_TASKINST）：翻源后「已办」列表读此表
UPDATE ACT_HI_TASKINST h
JOIN wf_task t ON t.engine_task_id = h.ID_
SET h.BIZ_TASK_ID_ = t.id
WHERE h.BIZ_TASK_ID_ IS NULL
  AND t.engine_task_id IS NOT NULL
  AND t.engine_task_id <> ''
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );

-- 待办（ACT_RU_TASK）
UPDATE ACT_RU_TASK r
JOIN wf_task t ON t.engine_task_id = r.ID_
SET r.BIZ_TASK_ID_ = t.id
WHERE r.BIZ_TASK_ID_ IS NULL
  AND t.engine_task_id IS NOT NULL
  AND t.engine_task_id <> ''
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );

-- =============================================================================
-- 校验：两表已回填条数；以及「1:1 但仍未回填」的残留（应为 0，否则需排查）
-- =============================================================================
SELECT 'ACT_HI_TASKINST 已回填 BIZ_TASK_ID_' AS item, COUNT(*) AS val
FROM ACT_HI_TASKINST WHERE BIZ_TASK_ID_ IS NOT NULL;

SELECT 'ACT_RU_TASK 已回填 BIZ_TASK_ID_' AS item, COUNT(*) AS val
FROM ACT_RU_TASK WHERE BIZ_TASK_ID_ IS NOT NULL;

SELECT '残留：1:1 但 BIZ_TASK_ID_ 仍为空' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE h.BIZ_TASK_ID_ IS NULL
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );
