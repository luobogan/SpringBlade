-- =============================================================================
-- 去 wf_* 表改造 · P3-2 实例级回填（迁移切割时一次性执行）
-- 作用：把存量 wf_instance 的业务维度回填进 ACT_HI_PROCINST 的原生业务列，
--       使运行期台账可直接从 ACT_* 检索，无需回写 wf_instance。
--
-- ⚠️ 执行前提（务必先读）：
--   1. USE blade；jeelowcode 也有 ACT_* 表，严禁跨库。
--   2. 必须在「wf_instance 尚未退役、且与 ACT_HI_PROCINST 通过 engine_inst_id 对齐」的切割窗口执行。
--   3. 幂等：仅回填 ACT_HI_PROCINST.BUSINESS_STATUS_ IS NULL 的实例，重复执行安全。
--   4. BUSINESS_STATUS_ 编码必须与运行期实时写入保持一致——
--      此处沿用 wf_instance.status → 业务状态码：
--        0运行中→'RUNNING'  1通过→'APPROVED'  2不通过→'REJECTED'
--        3撤销→'CANCELED'  4暂停→'SUSPENDED'  5草稿→'DRAFT'
--      如运行期改了编码，请同步修改下方 CASE。
--   5. TENANT_ID_ 已是 Flowable 标准列；此处仅在 ACT_HI_PROCINST.TENANT_ID_ 为空时
--      用 wf_instance.tenant_id 补填（多租户逻辑隔离防漏），不影响已带租户的实例。
-- =============================================================================

USE blade;

-- ───────────────────────────── 回填业务列 ─────────────────────────────
UPDATE ACT_HI_PROCINST t
JOIN wf_instance w ON w.engine_inst_id = t.PROC_INST_ID_
SET
  t.DEF_ID_         = w.def_id,
  t.FORM_ID_        = w.form_id,
  t.DATA_ID_        = w.data_id,
  t.TITLE_          = w.title,
  t.IS_TEST_        = IFNULL(w.is_test, 0),
  t.BUSINESS_STATUS_= CASE w.status
                        WHEN 0 THEN 'RUNNING'
                        WHEN 1 THEN 'APPROVED'
                        WHEN 2 THEN 'REJECTED'
                        WHEN 3 THEN 'CANCELED'
                        WHEN 4 THEN 'SUSPENDED'
                        WHEN 5 THEN 'DRAFT'
                        ELSE 'RUNNING'
                      END
WHERE t.BUSINESS_STATUS_ IS NULL
  AND w.engine_inst_id IS NOT NULL;

-- ───────────────────────────── 补填租户（仅空值） ─────────────────────────────
UPDATE ACT_HI_PROCINST t
JOIN wf_instance w ON w.engine_inst_id = t.PROC_INST_ID_
SET t.TENANT_ID_ = w.tenant_id
WHERE (t.TENANT_ID_ IS NULL OR t.TENANT_ID_ = '')
  AND w.tenant_id IS NOT NULL
  AND w.engine_inst_id IS NOT NULL;

-- ───────────────────────────── 回填 wf_instance 全字段对齐列 ─────────────────────────────
-- 把 BUSINESS_ID_(雪花)/STARTER_/CURRENT_NODE_KEY_/URGENCY_/L3 三标志/PARENT_ID_/TEST_DEPLOYMENT_ID_/PENDING_STATUS_
-- 从 wf_instance 回填进 ACT_HI_PROCINST。幂等：仅回填 BUSINESS_ID_ 仍为 NULL 的实例。
UPDATE ACT_HI_PROCINST t
JOIN wf_instance w ON w.engine_inst_id = t.PROC_INST_ID_
SET
  t.BUSINESS_ID_            = w.id,
  t.STARTER_                = w.starter,
  t.CURRENT_NODE_KEY_       = w.current_node_key,
  t.URGENCY_                = IFNULL(w.urgency, 0),
  t.BUSINESS_ROW_READY_     = w.business_row_ready,
  t.REQUEST_ID_BOUND_       = w.request_id_bound,
  t.ENGINE_DEPLOY_MATCHED_  = w.engine_deployment_matched,
  t.PARENT_ID_              = w.parent_id,
  t.TEST_DEPLOYMENT_ID_     = w.test_deployment_id,
  t.PENDING_STATUS_         = w.pending_status
WHERE t.BUSINESS_ID_ IS NULL
  AND w.engine_inst_id IS NOT NULL;

-- ───────────────────────────── 回填统计（人工核对用） ─────────────────────────────
SELECT
  COUNT(*)                                   AS total_proc_inst,
  SUM(t.BUSINESS_STATUS_ IS NOT NULL)         AS backfilled,
  SUM(t.BUSINESS_STATUS_ IS NULL)             AS still_null,
  SUM(t.TENANT_ID_ IS NULL OR t.TENANT_ID_='') AS tenant_missing
FROM ACT_HI_PROCINST t;
