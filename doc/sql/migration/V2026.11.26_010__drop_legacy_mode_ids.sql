-- 不做新旧 modeId 兼容：旧 modeId（表单ID）数据清理，统一使用 modeinfo.id。
-- 先把仍在使用、子数据已经写到新 modeId 的方案主表迁到新 modeId。

UPDATE mode_auth_scheme
SET mode_id = 2106000000000000001
WHERE tenant_id = '000000' AND form_id = 5 AND mode_id = 5 AND id IN (4, 14);

UPDATE mode_auth_scheme
SET mode_id = 2125644676909633537
WHERE tenant_id = '000000' AND form_id = 7 AND mode_id = 7 AND id IN (1, 25, 26);

-- 删除旧 modeId 下的权限数据（form_manage_auth_scope 只删随旧方案一起废弃的）。
DELETE FROM form_manage_auth_scope
WHERE tenant_id = '000000'
  AND scheme_id IN (
    SELECT id FROM mode_auth_scheme
    WHERE tenant_id = '000000' AND mode_id IN (5, 7)
  );

DELETE FROM modefieldauthorize WHERE tenant_id = '000000' AND modeid IN (5, 7);

DELETE FROM mode_field_auth_subject
WHERE tenant_id = '000000'
  AND group_id IN (
    SELECT id FROM mode_field_auth_group
    WHERE tenant_id = '000000' AND modeid IN (5, 7)
  );

DELETE FROM mode_field_auth_group WHERE tenant_id = '000000' AND modeid IN (5, 7);
DELETE FROM moderightinfo WHERE modeid IN (5, 7);
DELETE FROM modedatasharerule WHERE modeid IN (5, 7);
DELETE FROM mode_auth_scheme WHERE tenant_id = '000000' AND mode_id IN (5, 7);
