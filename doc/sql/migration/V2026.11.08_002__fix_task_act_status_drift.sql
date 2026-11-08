-- =============================================================================
-- 存量修正：wf_task → ACT_* 双写漂移（读源=act 的状态列漂移）
--
-- 【背景】
--   ACT_*.BUSINESS_STATUS_ 是 wf_task.status 的投影（由 WfTaskActWriter 双写）。
--   2026-11-08 定位到缺陷：WfStateProjector（引擎事件驱动的状态反写，方案C 阶段3）
--   只 update 了 wf_task，**从未调 taskActWriter.sync() 回写 ACT**，导致漂移。
--   实测漂移 68 条，单一形态：wf_task.status=4(办结) vs ACT.BUSINESS_STATUS_='TODO'。
--   后果：读源=act 的「已办」列表把「办结」显示成「待办」；按「办结」筛选漏掉这批行。
--   代码侧已修复（onTaskCompleted / onTaskDeleted / closeResidualTodos 三处补 sync），
--   本脚本负责修正**存量**遗留行（代码修复只防新增，不会自动收敛历史数据）。
--
-- 【安全性】
--   1. 幂等：只改「与 wf_task 不一致」的行，重复执行无副作用；
--   2. 只改 ACT_HI_TASKINST.BUSINESS_STATUS_ 一列，不动其它业务列
--      （⑤ 双写未覆盖实测为 0，无需回填其它列）；
--   3. 建议先在事务内预览影响行数，确认后再提交：
--        START TRANSACTION;
--        UPDATE ... ; SELECT ROW_COUNT();
--        -- 满意则 COMMIT; 否则 ROLLBACK;
--   4. 含前后置校验：修前有明细、修后漂移应为 0。
--
-- 【执行前必读】
--   本脚本会写 ACT_HI_TASKINST。dev 库为共享环境，执行前请确认无人在联调。
-- =============================================================================

USE blade;

-- ---------- 前置校验：修前漂移形态分布 ----------
SELECT '【修前】漂移形态（wf_status vs ACT 子状态 / 条数）' AS phase, t.status AS wf_status,
       h.BUSINESS_STATUS_ AS act_status, COUNT(*) AS cnt
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END
GROUP BY t.status, h.BUSINESS_STATUS_
ORDER BY cnt DESC;

SELECT '【修前】待修正行数' AS phase, COUNT(*) AS cnt
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- ---------- 修正：把 ACT 侧子状态对齐 wf_task.status ----------
UPDATE ACT_HI_TASKINST h
JOIN wf_task t ON h.ID_ = t.engine_task_id
SET h.BUSINESS_STATUS_ = CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE h.BUSINESS_STATUS_ END
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- ---------- 后置校验：漂移应为 0 ----------
SELECT '【修后】剩余漂移（应为 0）' AS phase, COUNT(*) AS cnt
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- ---------- 后置校验：读源=act 的「已办」按 ACT 子状态分布（确认「办结」已归位） ----------
SELECT '【修后】已办行按 ACT 子状态分布' AS phase, h.BUSINESS_STATUS_ AS act_status, COUNT(*) AS cnt
FROM ACT_HI_TASKINST h
WHERE h.END_TIME_ IS NOT NULL
  AND (h.IS_TEST_ = 0 OR h.IS_TEST_ IS NULL)
GROUP BY h.BUSINESS_STATUS_
ORDER BY cnt DESC;
