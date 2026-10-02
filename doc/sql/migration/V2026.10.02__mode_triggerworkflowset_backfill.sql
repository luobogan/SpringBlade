-- =============================================================================
-- 迁移脚本③：回填 mode_triggerworkflowset.workflow_key / tenant_id
-- 依赖：V2026.10.02__mode_triggerworkflowset_add_workflow_key.sql（已加列）
-- 逻辑：经已有外键 workflowid 关联 wf_process_definition，补齐绑定键与租户，
--       使新开关「以 Flowable 为权威源」开启后可直接按 (workflow_key, tenant_id) 解析。
-- 幂等：仅回填空值；本环境触发器表当前为空，执行影响 0 行，待配置触发器后生效。
-- =============================================================================
UPDATE mode_triggerworkflowset t
JOIN wf_process_definition d ON d.id = t.workflowid
SET t.workflow_key = d.proc_key,
    t.tenant_id    = d.tenant_id
WHERE t.workflowid IS NOT NULL
  AND (t.workflow_key IS NULL OR t.workflow_key = '' OR t.tenant_id IS NULL OR t.tenant_id = '');

SELECT 'trigger_total'     AS metric, COUNT(*) AS cnt FROM mode_triggerworkflowset
UNION ALL
SELECT 'trigger_with_key',  COUNT(*) FROM mode_triggerworkflowset WHERE workflow_key IS NOT NULL AND workflow_key <> ''
UNION ALL
SELECT 'trigger_with_tenant', COUNT(*) FROM mode_triggerworkflowset WHERE tenant_id IS NOT NULL AND tenant_id <> ''
UNION ALL
SELECT 'trigger_null_wfid', COUNT(*) FROM mode_triggerworkflowset WHERE workflowid IS NULL;
