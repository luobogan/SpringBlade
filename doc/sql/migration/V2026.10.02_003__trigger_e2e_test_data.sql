-- =============================================================================
-- 迁移脚本：formmode 触发器全链路测试数据（dev）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.02_003
-- 目标库    : blade
-- 作用      : 1) 新建一个测试表单模块 modeinfo（billid=5 → 复用物理表 formtable_main_5，
--                该表已有可填列 field_1/field_2/field_3/field_4）
--             2) 为该模块配置一条「新建即触发」的审批触发器，绑定已发布流程
--                测试-918（wf_process_definition.id=2100918142244032513，
--                proc_key=flow_mu6qi6bgutt2，tenant 000000）
-- 幂等      : 按 id NOT EXISTS 判重，可重复执行
-- 回滚      : DELETE FROM mode_triggerworkflowset WHERE id=2106000000000000101;
--             DELETE FROM modeinfo WHERE id=2106000000000000001;
-- =============================================================================

-- 1) 测试表单模块（billid=5 → formtable_main_5）
INSERT INTO modeinfo (id, modename, modedescription, billid, modetype, status, dsporder,
                      creater, createdate, createtime, tenant_id, is_deleted)
SELECT 2106000000000000001, '触发器测试表单', 'dev 全链路测试：保存自动发起审批（billid=5 复用 formtable_main_5）',
       5, 0, 1, 0,
       1, '2026-10-02', '10:00:00', '000000', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM modeinfo WHERE id = 2106000000000000001);

-- 2) 触发器：新建(0)即触发，绑定 测试-918（旧路径 workflowid + 新键 workflow_key 双写）
INSERT INTO mode_triggerworkflowset (id, modeid, triggeropt, workflowid, workflowname,
                                     workflow_key, status, showcondition, dsporder, tenant_id)
SELECT 2106000000000000101, 2106000000000000001, 0, 2100918142244032513, '测试-918',
       'flow_mu6qi6bgutt2', 1, NULL, 0, '000000'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM mode_triggerworkflowset WHERE id = 2106000000000000101);

-- 3) 校验
SELECT 'modeinfo' AS t, id, modename, billid, status, tenant_id FROM modeinfo WHERE id = 2106000000000000001;
SELECT 'trigger' AS t, id, modeid, triggeropt, workflowid, workflowname, workflow_key, status, tenant_id
FROM mode_triggerworkflowset WHERE id = 2106000000000000101;
