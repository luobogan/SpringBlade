USE blade;

-- ============================================================
-- V2026.10.02_003__backfill_approval_comment.sql
-- ⚠️ 破坏性/一次性：把 wf_approval_log 中【未被 ACT_HI_COMMENT 覆盖】的行回填进 ACT_HI_COMMENT，
--    使 ACT 成为完整的审批日志源，从而让 T-14 的 wf-table-retirement 翻转（禁用 mergeMissingFromWf）安全。
--
-- 背景：WfApprovalLogActReader.readFromAct 读源=act 时，先解析 ACT_HI_COMMENT.MESSAGE_（JSON），
--   再经 mergeMissingFromWf 按 (logType+nodeKey+operator+opinion+时间±2s) 补回 wf_approval_log 缺失行
--   （引擎归档/挂起时段 AddCommentCmd 静默失败所致）。dev 实测 wf_approval_log 589 行、ACT 仅 239 行，
--   缺口主要为新发起实例的评论（syncCommentToEngine 未可靠捕获）。翻转 wf-table-retirement=true 会禁用
--   merge，导致这些日志不可见。本脚本把缺口补齐，使 merge 成为 no-op，翻转后日志零丢失。
--
-- 编码契约（与 WfWriteHelper.syncCommentToEngine / WfApprovalLogActReader.parseActComment 对称）：
--   MESSAGE_ = JSON_OBJECT('wfTaskId','nodeKey','operator','opinion')，TYPE_=log_type，TIME_=operate_time。
--
-- 幂等：回填行 ID_ 带 'wf_bf_' 前缀（= 'wf_bf_' + wf.id），唯一且可重跑；并加 NOT EXISTS 覆盖判断避免重复。
-- ⚠️ 不可回滚（dev 数据，可重新生成）。
-- ============================================================

-- ---------- 回填前核对 ----------
SELECT 'BEFORE' AS phase,
       (SELECT COUNT(*) FROM wf_approval_log WHERE is_deleted = 0) AS wf_logs,
       (SELECT COUNT(*) FROM ACT_HI_COMMENT)                       AS act_comments;

-- ---------- 回填（仅覆盖不到的 wf 行，且实例已部署到引擎）----------
INSERT INTO ACT_HI_COMMENT (ID_, TYPE_, TIME_, USER_ID_, TASK_ID_, PROC_INST_ID_, ACTION_, MESSAGE_, FULL_MSG_, TENANT_ID_)
SELECT
    CONCAT('wf_bf_', w.id)                                              AS ID_,
    w.log_type                                                         AS TYPE_,
    w.operate_time                                                     AS TIME_,
    w.operator                                                         AS USER_ID_,
    w.task_id                                                          AS TASK_ID_,
    i.engine_inst_id                                                   AS PROC_INST_ID_,
    NULL                                                               AS ACTION_,
    JSON_OBJECT(
        'wfTaskId', w.task_id,
        'nodeKey', COALESCE(w.node_key, ''),
        'operator', w.operator,
        'opinion',  COALESCE(w.opinion, '')
    )                                                                  AS MESSAGE_,
    NULL                                                               AS FULL_MSG_,
    w.tenant_id                                                        AS TENANT_ID_
FROM wf_approval_log w
JOIN wf_instance i
    ON i.id = w.inst_id
   AND i.engine_inst_id IS NOT NULL
   AND i.is_deleted = 0
WHERE w.is_deleted = 0
  -- 仅回填：ACT 中不存在时间±2s 内的同型同内容评论（避免与既有 ACT 评论重复）
  AND NOT EXISTS (
        SELECT 1 FROM ACT_HI_COMMENT c
        WHERE c.PROC_INST_ID_ = i.engine_inst_id
          AND c.TYPE_ = w.log_type
          AND c.TIME_ BETWEEN w.operate_time - INTERVAL 2 SECOND AND w.operate_time + INTERVAL 2 SECOND
          AND c.MESSAGE_ LIKE CONCAT('%', REPLACE(REPLACE(COALESCE(w.node_key, ''), '\\', '\\\\'), '"', '\\"'), '%')
          AND c.MESSAGE_ LIKE CONCAT('%', REPLACE(REPLACE(COALESCE(w.opinion, ''), '\\', '\\\\'), '"', '\\"'), '%')
          AND (w.operator IS NULL OR c.MESSAGE_ LIKE CONCAT('%', w.operator, '%'))
    )
  -- 幂等：同名 ID_ 不重复插入
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_COMMENT c2 WHERE c2.ID_ = CONCAT('wf_bf_', w.id));

-- ---------- 回填后核对（期望 wf_logs 与 act_comments 接近，merge 变 no-op）----------
SELECT 'AFTER' AS phase,
       (SELECT COUNT(*) FROM wf_approval_log WHERE is_deleted = 0) AS wf_logs,
       (SELECT COUNT(*) FROM ACT_HI_COMMENT)                       AS act_comments,
       (SELECT COUNT(*) FROM ACT_HI_COMMENT WHERE ID_ LIKE 'wf_bf_%') AS backfilled;
