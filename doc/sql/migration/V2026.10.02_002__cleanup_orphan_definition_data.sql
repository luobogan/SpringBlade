USE blade;

-- ============================================================
-- V2026.10.02_002__cleanup_orphan_definition_data.sql
-- ⚠️ 破坏性脚本（仅限 dev）：清理「父定义已不存在」的孤儿数据
--
-- 背景：接 V2026.10.02_001（硬删草稿定义）后复查发现历史遗留孤儿 ——
--   此前删除过流程定义但未级联清理其节点/连线/权限/自定义操作，
--   残留 wf_process_node 212、wf_node_link 267、wf_node_field_perm 326、
--   wf_custom_operation 326、wf_approval_log 1。
--   已确认成因全部是 def_id 指向【已不存在的定义】（def_id IS NULL 的行数为 0），
--   非合法全局值，可安全清理。
--
-- 幂等：全部按条件删除，重复执行第二次影响行数为 0。
-- ⚠️ 不可回滚。
-- ============================================================

-- ---------- 删除前核对 ----------
SELECT 'BEFORE' AS phase,
       (SELECT COUNT(*) FROM wf_process_node n LEFT JOIN wf_process_definition d ON n.def_id = d.id WHERE d.id IS NULL) AS orphan_nodes,
       (SELECT COUNT(*) FROM wf_node_link l LEFT JOIN wf_process_definition d ON l.def_id = d.id WHERE d.id IS NULL)    AS orphan_links,
       (SELECT COUNT(*) FROM wf_node_field_perm p LEFT JOIN wf_process_definition d ON p.def_id = d.id WHERE d.id IS NULL) AS orphan_perm,
       (SELECT COUNT(*) FROM wf_custom_operation o LEFT JOIN wf_process_definition d ON o.def_id = d.id WHERE d.id IS NULL) AS orphan_ops;

-- ---------- 1) 自定义操作的动作 / 权限（先于操作本体，避免产生新孤儿）----------
DELETE FROM wf_custom_operation_action
WHERE op_id IN (SELECT o.id FROM wf_custom_operation o
                LEFT JOIN wf_process_definition d ON o.def_id = d.id
                WHERE d.id IS NULL);

DELETE FROM wf_custom_operation_right
WHERE op_id IN (SELECT o.id FROM wf_custom_operation o
                LEFT JOIN wf_process_definition d ON o.def_id = d.id
                WHERE d.id IS NULL);

DELETE FROM wf_custom_operation
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);

-- ---------- 2) 字段权限 ----------
DELETE FROM wf_node_field_perm
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);

-- ---------- 3) 节点操作者（先于节点本体）----------
DELETE FROM wf_node_operator
WHERE node_id IN (SELECT n.id FROM wf_process_node n
                  LEFT JOIN wf_process_definition d ON n.def_id = d.id
                  WHERE d.id IS NULL);

-- ---------- 4) 连线 / 节点 ----------
DELETE FROM wf_node_link
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);

DELETE FROM wf_process_node
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);

-- ---------- 5) 其它孤儿 ----------
DELETE FROM wf_node_detail_perm
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);
DELETE FROM wf_node_detail_filter
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);
DELETE FROM wf_node_timeout
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);
DELETE FROM wf_node_default_sign
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);
DELETE FROM wf_test_log
WHERE def_id NOT IN (SELECT id FROM wf_process_definition);
DELETE FROM wf_approval_log
WHERE inst_id NOT IN (SELECT id FROM wf_instance);

-- ---------- 删除后核对（期望全部为 0）----------
SELECT 'AFTER' AS phase,
       (SELECT COUNT(*) FROM wf_process_node n LEFT JOIN wf_process_definition d ON n.def_id = d.id WHERE d.id IS NULL) AS orphan_nodes,
       (SELECT COUNT(*) FROM wf_node_link l LEFT JOIN wf_process_definition d ON l.def_id = d.id WHERE d.id IS NULL)    AS orphan_links,
       (SELECT COUNT(*) FROM wf_node_field_perm p LEFT JOIN wf_process_definition d ON p.def_id = d.id WHERE d.id IS NULL) AS orphan_perm,
       (SELECT COUNT(*) FROM wf_custom_operation o LEFT JOIN wf_process_definition d ON o.def_id = d.id WHERE d.id IS NULL) AS orphan_ops,
       (SELECT COUNT(*) FROM wf_approval_log g LEFT JOIN wf_instance i ON g.inst_id = i.id WHERE i.id IS NULL)          AS orphan_logs;

-- ---------- 现存数据总览 ----------
SELECT 'SUMMARY' AS phase,
       (SELECT COUNT(*) FROM wf_process_definition) AS defs,
       (SELECT COUNT(*) FROM wf_process_node)       AS nodes,
       (SELECT COUNT(*) FROM wf_node_link)          AS links,
       (SELECT COUNT(*) FROM wf_instance)           AS insts;
