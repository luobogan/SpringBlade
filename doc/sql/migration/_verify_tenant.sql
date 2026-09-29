-- 核查正式实例 ACT 行的租户填充情况
SELECT '=== 正式 ACT 行租户填充 ===' AS step;
SELECT
  COUNT(*) AS formal_act_rows,
  SUM(TENANT_ID_ IS NULL OR TENANT_ID_='') AS tenant_null,
  GROUP_CONCAT(DISTINCT TENANT_ID_) AS distinct_tenants
FROM ACT_HI_PROCINST t
WHERE EXISTS (SELECT 1 FROM wf_instance w WHERE w.is_test=0 AND w.engine_inst_id=t.PROC_INST_ID_);

SELECT '=== wf_instance 租户分布（对照） ===' AS step;
SELECT IFNULL(tenant_id,'(null)') AS tenant, COUNT(*) AS cnt FROM wf_instance WHERE is_test=0 GROUP BY tenant_id;
