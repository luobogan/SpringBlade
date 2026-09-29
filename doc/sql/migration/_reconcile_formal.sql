-- 生产侧逐字段对账：正式(is_test=0) wf_instance 与回填后的 ACT_HI_PROCINST 是否一致
-- 运行：mysql -h 127.0.0.1 -P 3306 -u root -p123456 blade < _reconcile_formal.sql

SELECT '=== 正式实例 wf↔ACT 覆盖度 ===' AS step;
SELECT
  (SELECT COUNT(*) FROM wf_instance WHERE is_test=0) AS formal_wf_total,
  (SELECT COUNT(*) FROM wf_instance w
     WHERE w.is_test=0 AND w.engine_inst_id IS NOT NULL
       AND EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_=w.engine_inst_id)) AS formal_wf_matched_to_act,
  (SELECT COUNT(*) FROM wf_instance w
     WHERE w.is_test=0 AND w.engine_inst_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_=w.engine_inst_id)) AS formal_wf_orphan;

SELECT '=== 逐字段差异计数（仅正式+已对齐） ===' AS step;
SELECT
  SUM(t.BUSINESS_ID_ IS NULL OR t.BUSINESS_ID_ <> w.id)                                                   AS diff_business_id,
  SUM(t.STARTER_    IS NULL OR t.STARTER_    <> w.starter)                                                 AS diff_starter,
  SUM(t.DEF_ID_     IS NULL OR t.DEF_ID_     <> w.def_id)                                                  AS diff_def_id,
  SUM(t.TITLE_      IS NULL OR t.TITLE_      <> w.title)                                                   AS diff_title,
  SUM(t.CURRENT_NODE_KEY_ IS NULL OR t.CURRENT_NODE_KEY_ <> w.current_node_key)                            AS diff_node_key,
  SUM(t.IS_TEST_    IS NULL OR t.IS_TEST_    <> IFNULL(w.is_test,0))                                       AS diff_is_test,
  SUM(t.BUSINESS_STATUS_ IS NULL OR t.BUSINESS_STATUS_ <>
        CASE w.status WHEN 0 THEN 'RUNNING' WHEN 1 THEN 'APPROVED' WHEN 2 THEN 'REJECTED'
                     WHEN 3 THEN 'CANCELED' WHEN 4 THEN 'SUSPENDED' WHEN 5 THEN 'DRAFT' ELSE 'RUNNING' END) AS diff_status
FROM wf_instance w
JOIN ACT_HI_PROCINST t ON t.PROC_INST_ID_ = w.engine_inst_id
WHERE w.is_test = 0;

SELECT '=== 抽样：前 10 条正式实例 wf↔ACT 关键字段并排 ===' AS step;
SELECT w.id AS wf_id, w.status AS wf_status, w.starter AS wf_starter, w.def_id AS wf_def,
       w.title AS wf_title, w.current_node_key AS wf_node,
       t.BUSINESS_ID_ AS act_bid, t.BUSINESS_STATUS_ AS act_status, t.STARTER_ AS act_starter,
       t.DEF_ID_ AS act_def, t.TITLE_ AS act_title, t.CURRENT_NODE_KEY_ AS act_node
FROM wf_instance w
JOIN ACT_HI_PROCINST t ON t.PROC_INST_ID_ = w.engine_inst_id
WHERE w.is_test = 0
ORDER BY w.id DESC
LIMIT 10;
