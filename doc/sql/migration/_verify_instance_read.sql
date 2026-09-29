-- 验证脚本：连通性 + 实例读源切换对账（只读，不改动数据）
-- 运行：mysql -h 127.0.0.1 -P 3306 -u root -p123456 blade < _verify_instance_read.sql

SELECT '=== 1. 连通性 ===' AS step;
SELECT DATABASE() AS cur_db, NOW() AS now;

SELECT '=== 2. 表存在性 ===' AS step;
SELECT
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='blade' AND table_name='wf_instance') AS wf_instance_exists,
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='blade' AND table_name='ACT_HI_PROCINST') AS act_proc_exists;

SELECT '=== 3. 数据规模 ===' AS step;
SELECT (SELECT COUNT(*) FROM wf_instance) AS wf_inst_cnt,
       (SELECT COUNT(*) FROM ACT_HI_PROCINST) AS act_proc_cnt;

SELECT '=== 4. ACT_HI_PROCINST 业务列是否已加（本改造依赖） ===' AS step;
SELECT COLUMN_NAME
FROM information_schema.columns
WHERE table_schema='blade' AND table_name='ACT_HI_PROCINST'
  AND COLUMN_NAME IN ('BUSINESS_ID_','STARTER_','CURRENT_NODE_KEY_','URGENCY_','BUSINESS_STATUS_','DEF_ID_','DATA_ID_','FORM_ID_','TITLE_','IS_TEST_','BUSINESS_ROW_READY_','REQUEST_ID_BOUND_','ENGINE_DEPLOY_MATCHED_','PARENT_ID_','TEST_DEPLOYMENT_ID_','PENDING_STATUS_');

SELECT '=== 5. 双写/回填就绪度（ACT 业务列非空占比） ===' AS step;
SELECT
  COUNT(*) AS total,
  SUM(BUSINESS_STATUS_ IS NOT NULL) AS backfilled,
  SUM(BUSINESS_ID_ IS NOT NULL) AS business_id_filled,
  SUM(STARTER_ IS NOT NULL) AS starter_filled,
  SUM(TENANT_ID_ IS NULL OR TENANT_ID_='') AS tenant_missing
FROM ACT_HI_PROCINST;

SELECT '=== 6. 对齐校验：wf_instance 与 ACT_HI_PROCINST 通过 engine_inst_id 关联完整性 ===' AS step;
SELECT
  (SELECT COUNT(*) FROM wf_instance w WHERE w.engine_inst_id IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM ACT_HI_PROCINST t WHERE t.PROC_INST_ID_ = w.engine_inst_id)) AS wf_orphan_no_act,
  (SELECT COUNT(*) FROM ACT_HI_PROCINST t WHERE NOT EXISTS (SELECT 1 FROM wf_instance w WHERE w.engine_inst_id = t.PROC_INST_ID_)) AS act_orphan_no_wf;
