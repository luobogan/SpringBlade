USE blade;

-- ============================================================
-- V2026.10.02_001__cleanup_draft_definitions.sql
-- ⚠️⚠️ 破坏性脚本（仅限 dev）：硬删除「未部署(草稿)」流程定义及其全部关联数据
--
-- 背景：T-14 前置核查发现 dev 存在 22 个 proc_def_id 为空的定义
--       （其中 8 个 is_deleted=0 在用草稿 + 10 个已软删），
--       连同节点/连线/操作者/字段权限/自定义操作/测试日志等历史垃圾数据，
--       长期积累导致数据混乱。用户已批准：全部硬删除、清空关联，之后重新生成数据。
--
-- 范围：proc_def_id IS NULL OR proc_def_id='' 的定义，及其全部下游：
--       wf_instance(及其 wf_task / wf_approval_log / ACT 行)
--       wf_process_node(及其 wf_node_operator)
--       wf_node_link / wf_node_field_perm / wf_node_detail_perm / wf_node_detail_filter
--       wf_node_timeout / wf_node_default_sign
--       wf_custom_operation(及其 action / right)
--       wf_definition_gray / flow_def_bridge / wf_test_log
--       wf_migration_map / wf_subflow_request
--
-- ⚠️ 不可回滚（硬删除）。执行前已确认：这些定义上的 3 个实例均为
--    status=5(草稿/未提交)、engine_inst_id IS NULL、ACT_HI_PROCINST 0 行
--    —— 即从未真正发起，删除不影响任何运行中的流程。
--
-- 幂等：全部按条件删除，重复执行第二次影响行数为 0。
-- ============================================================

-- ---------- 删除前核对 ----------
SELECT 'BEFORE' AS phase,
       (SELECT COUNT(*) FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '') AS draft_defs,
       (SELECT COUNT(*) FROM wf_process_definition)                                               AS all_defs,
       (SELECT COUNT(*) FROM wf_instance)                                                         AS all_inst,
       (SELECT COUNT(*) FROM wf_process_node)                                                     AS all_nodes;

-- ---------- 1) 实例的下游：审批意见 / 任务 / ACT ----------
DELETE FROM wf_approval_log
WHERE inst_id IN (SELECT id FROM wf_instance
                  WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));

DELETE FROM wf_task
WHERE inst_id IN (SELECT id FROM wf_instance
                  WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));

DELETE FROM ACT_HI_COMMENT
WHERE PROC_INST_ID_ IN (SELECT engine_inst_id FROM wf_instance
                        WHERE engine_inst_id IS NOT NULL
                          AND def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));

DELETE FROM ACT_HI_PROCINST
WHERE BUSINESS_ID_ IN (SELECT id FROM wf_instance
                       WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));

-- ---------- 2) 实例本体 ----------
DELETE FROM wf_instance
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');

-- ---------- 3) 节点的下游：操作者 ----------
DELETE FROM wf_node_operator
WHERE node_id IN (SELECT id FROM wf_process_node
                  WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));

-- ---------- 4) 节点配置类 ----------
DELETE FROM wf_node_field_perm
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_node_detail_perm
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_node_detail_filter
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_node_timeout
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_node_default_sign
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');

-- ---------- 5) 节点 / 连线 ----------
DELETE FROM wf_node_link
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_process_node
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');

-- ---------- 6) 自定义操作及其动作 / 权限 ----------
DELETE FROM wf_custom_operation_action
WHERE op_id IN (SELECT id FROM wf_custom_operation
                WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));
DELETE FROM wf_custom_operation_right
WHERE op_id IN (SELECT id FROM wf_custom_operation
                WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = ''));
DELETE FROM wf_custom_operation
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');

-- ---------- 7) 其它关联 ----------
DELETE FROM wf_definition_gray
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM flow_def_bridge
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_test_log
WHERE def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_migration_map
WHERE new_def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');
DELETE FROM wf_subflow_request
WHERE sub_def_id IN (SELECT id FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '');

-- ---------- 8) 定义本体（规避 MySQL 1093：不能直接子查询目标表，用派生表）----------
DELETE FROM wf_process_definition
WHERE id IN (SELECT id FROM (SELECT id FROM wf_process_definition
                             WHERE proc_def_id IS NULL OR proc_def_id = '') t);

-- ---------- 删除后核对（期望 draft_defs = 0；all_defs / all_inst 仅减少草稿部分）----------
SELECT 'AFTER' AS phase,
       (SELECT COUNT(*) FROM wf_process_definition WHERE proc_def_id IS NULL OR proc_def_id = '') AS draft_defs,
       (SELECT COUNT(*) FROM wf_process_definition)                                               AS all_defs,
       (SELECT COUNT(*) FROM wf_instance)                                                         AS all_inst,
       (SELECT COUNT(*) FROM wf_process_node)                                                     AS all_nodes;

-- ---------- 残留孤儿检查（期望全部为 0）----------
SELECT 'orphan_check' AS chk,
       (SELECT COUNT(*) FROM wf_process_node n LEFT JOIN wf_process_definition d ON n.def_id = d.id WHERE d.id IS NULL)      AS orphan_nodes,
       (SELECT COUNT(*) FROM wf_node_link l LEFT JOIN wf_process_definition d ON l.def_id = d.id WHERE d.id IS NULL)        AS orphan_links,
       (SELECT COUNT(*) FROM wf_instance i LEFT JOIN wf_process_definition d ON i.def_id = d.id WHERE d.id IS NULL)         AS orphan_inst;
