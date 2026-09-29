-- 诊断：135 条 wf_instance 孤儿（engine_inst_id 非空但 ACT_HI_PROCINST 无对应）的组成
-- 运行：mysql -h 127.0.0.1 -P 3306 -u root -p123456 blade < _verify_orphans.sql

SELECT '=== A. wf_instance 总体状态分布 ===' AS step;
SELECT status, COUNT(*) AS cnt FROM wf_instance GROUP BY status ORDER BY status;

SELECT '=== B. wf_instance 是否测试分布 ===' AS step;
SELECT IFNULL(is_test,0) AS is_test, COUNT(*) AS cnt FROM wf_instance GROUP BY is_test;

SELECT '=== C. 孤儿(engine_inst_id 非空且 ACT 无对应) 的状态分布 ===' AS step;
SELECT w.status, COUNT(*) AS orphan_cnt
FROM wf_instance w
WHERE w.engine_inst_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id)
GROUP BY w.status ORDER BY w.status;

SELECT '=== D. 孤儿(engine_inst_id 非空且 ACT 无对应) 的 is_test 分布 ===' AS step;
SELECT IFNULL(w.is_test,0) AS is_test, COUNT(*) AS orphan_cnt
FROM wf_instance w
WHERE w.engine_inst_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id)
GROUP BY w.is_test;

SELECT '=== E. 孤儿中 engine_inst_id 当前仍运行在 ACT_RU_EXECUTION 的比例 ===' AS step;
SELECT
  COUNT(*) AS orphan_total,
  SUM(w.engine_inst_id IN (SELECT ID_ FROM ACT_RU_EXECUTION WHERE TYPE_='processInstance' OR TYPE_ IS NULL)) AS still_running_in_ru
FROM wf_instance w
WHERE w.engine_inst_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id);

SELECT '=== F. 孤儿的时间跨度 ===' AS step;
SELECT MIN(w.start_time) AS min_start, MAX(w.start_time) AS max_start,
       SUM(w.start_time < '2026-01-01') AS before_2026
FROM wf_instance w
WHERE w.engine_inst_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id);

SELECT '=== G. 已对齐(35) 的状态分布（这些是可平滑切读源的） ===' AS step;
SELECT w.status, COUNT(*) AS aligned_cnt
FROM wf_instance w
WHERE EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id)
GROUP BY w.status ORDER BY w.status;
