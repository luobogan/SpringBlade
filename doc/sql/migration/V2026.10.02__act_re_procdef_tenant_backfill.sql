-- =====================================================================
-- V2026.10.02__act_re_procdef_tenant_backfill.sql
-- P0 前置（「以 Flowable 为唯一事实源」改造）：回填 ACT_RE_PROCDEF / ACT_RE_DEPLOYMENT 的 TENANT_ID_
-- 背景：现状 ProcessServiceImpl.deployProcess / deployProcessForTest 的 createDeployment() 未 .tenantId()，
--       导致所有流程定义与部署都落在默认（空）租户；多租户在引擎侧实际未隔离。
--       改造后部署必须带租户（见 ProcessServiceImpl 的 3 参重载），本脚本补齐存量数据的租户列。
-- 范围：ACT_* 与 wf_process_definition 同库（blade_workflow），可用 JOIN 回填；
--       mode_triggerworkflowset（blade 主库）跨库，不在此脚本，属任务 2 应用侧批处理。
-- 幂等：UPDATE 仅作用于 TENANT_ID_ 为空/未回填的行，可重复执行。
-- 执行前请先人工确认下面的【预检】结果，并对账【校验】结果。属于破坏性写入，需单独审批后再执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 【预检 1】确认 proc_key 在 wf_process_definition 中按租户是否唯一
--          若某 proc_key 映射到 >1 个租户（多租户同 key），下方 UPDATE 会跳过该 key（HAVING COUNT(DISTINCT tenant_id)=1），
--          这些 key 需人工确认租户归属后再单独处理。
-- ---------------------------------------------------------------------
SELECT proc_key, COUNT(DISTINCT tenant_id) AS tenant_cnt, COUNT(*) AS rows_cnt
FROM wf_process_definition
WHERE is_deleted = 0
GROUP BY proc_key
HAVING COUNT(DISTINCT tenant_id) > 1;

-- ---------------------------------------------------------------------
-- 【预检 2】统计待回填行数（执行 UPDATE 前应先看一眼量级）
-- ---------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM ACT_RE_PROCDEF WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')        AS procdef_to_backfill,
  (SELECT COUNT(*) FROM ACT_RE_DEPLOYMENT WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')      AS deployment_to_backfill;

-- ---------------------------------------------------------------------
-- 【回填 1】ACT_RE_PROCDEF.TENANT_ID_ ← wf_process_definition.tenant_id
--           仅回填「proc_key 在 wf_process_definition 中租户唯一」的定义，避免多租户同 key 误填。
--           版本多行（同 key 同租户）统一取该租户；is_deleted 排除已删定义。
-- ---------------------------------------------------------------------
UPDATE ACT_RE_PROCDEF pd
JOIN (
  SELECT proc_key, MAX(tenant_id) AS tenant_id
  FROM wf_process_definition
  WHERE is_deleted = 0
  GROUP BY proc_key
  HAVING COUNT(DISTINCT tenant_id) = 1
) d ON d.proc_key = pd.KEY_
SET pd.TENANT_ID_ = d.tenant_id
WHERE pd.TENANT_ID_ IS NULL OR pd.TENANT_ID_ = '';

-- ---------------------------------------------------------------------
-- 【回填 2】ACT_RE_DEPLOYMENT.TENANT_ID_ ← wf_process_definition.tenant_id（按 deployment_id 关联）
--           仅回填「deployment_id 在 wf_process_definition 中租户唯一」的部署；孤儿部署（无 def 引用）保持为空。
-- ---------------------------------------------------------------------
UPDATE ACT_RE_DEPLOYMENT dep
JOIN (
  SELECT deployment_id, MAX(tenant_id) AS tenant_id
  FROM wf_process_definition
  WHERE is_deleted = 0 AND deployment_id IS NOT NULL
  GROUP BY deployment_id
  HAVING COUNT(DISTINCT tenant_id) = 1
) d ON d.deployment_id = dep.ID_
SET dep.TENANT_ID_ = d.tenant_id
WHERE dep.TENANT_ID_ IS NULL OR dep.TENANT_ID_ = '';

-- ---------------------------------------------------------------------
-- 【校验】执行后再次统计，确认待回填行数归零（预检 2 的多租户同 key 行除外，应已在预检 1 中暴露）
-- ---------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM ACT_RE_PROCDEF WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')    AS procdef_still_null,
  (SELECT COUNT(*) FROM ACT_RE_DEPLOYMENT WHERE TENANT_ID_ IS NULL OR TENANT_ID_ = '')  AS deployment_still_null;
