-- ============================================================================
-- 历史审批日志回填：wf_approval_log  ->  ACT_HI_COMMENT
-- ============================================================================
-- 用途：当 `blade.workflow.instance-read-source=act` 把 /instance/{id}/logs 的读源
--       切到 ACT_HI_COMMENT 之前，必须把存量 wf_approval_log 回填进 ACT_HI_COMMENT，
--       否则翻源后「双写开启前的历史意见」将不可见。
--
-- 对称契约：MESSAGE_ 必须还原成与 WfWriteHelper.syncCommentToEngine 完全一致的 JSON
--       {"nodeKey":"...","operator":N,"wfTaskId":N,"opinion":"...","ts":N}
--       TYPE_ 保留 logType（对齐 RequestLogType）；读侧 parseActComment 据此还原维度。
--
-- 作用范围：仅回填「实例已双写到 ACT_HI_PROCINST」的记录（JOIN BUSINESS_ID_）。
--       双写开启前、且从未双写过的实例在 ACT 里没有行，会被跳过——这些实例翻源后会
--       丢失历史意见（属已知限制；双写自 P3-4 起默认开启，绝大多数活跃实例已覆盖）。
--
-- 幂等：用 (PROC_INST_ID_, TYPE_, 时间±2s) 容差防重复；另附 dedup 兜底。
-- 运行：**staging 一次性**，翻源前执行；USE blade 规避跨库（jeelowcode 也有 ACT_* 表）。
-- ============================================================================

USE blade;

-- ---------------------------------------------------------------------------
-- 1) 回填（CTE 内用 JSON_OBJECT 生成与写侧同形态 MESSAGE_，自动转义用户文本）
-- ---------------------------------------------------------------------------
INSERT INTO ACT_HI_COMMENT
    (ID_, TYPE_, TIME_, USER_ID_, TASK_ID_, PROC_INST_ID_, ACTION_, MESSAGE_, FULL_MSG_)
WITH src AS (
    SELECT
        l.inst_id,
        l.task_id,
        l.node_key,
        l.operator,
        l.log_type,
        l.opinion,
        l.operate_time,
        t.engine_task_id,
        p.ID_                                                   AS engine_inst_id,
        JSON_OBJECT(
            'nodeKey',  COALESCE(l.node_key, ''),
            'operator', COALESCE(l.operator, 0),
            'wfTaskId', COALESCE(l.task_id, 0),
            'opinion',  COALESCE(l.opinion, ''),
            'ts',       COALESCE(UNIX_TIMESTAMP(l.operate_time) * 1000, 0)
        )                                                       AS message
    FROM wf_approval_log l
    LEFT JOIN wf_task t ON t.id = l.task_id
    JOIN ACT_HI_PROCINST p ON p.BUSINESS_ID_ = l.inst_id
    WHERE p.ID_ IS NOT NULL
)
SELECT
    UUID(),
    src.log_type,
    src.operate_time,
    CAST(COALESCE(src.operator, 0) AS CHAR),
    src.engine_task_id,
    src.engine_inst_id,
    'AddComment',
    CAST(src.message AS CHAR),
    NULL
FROM src
WHERE NOT EXISTS (
    SELECT 1
    FROM ACT_HI_COMMENT c
    WHERE c.PROC_INST_ID_ = src.engine_inst_id
      AND c.TYPE_        = src.log_type
      AND TIMESTAMPDIFF(SECOND, c.TIME_, src.operate_time) BETWEEN -2 AND 2
);

-- ---------------------------------------------------------------------------
-- 2) 兜底去重：若因 ±2s 容差或重复执行产生了同一条业务意见的两条评论，按
--    (PROC_INST_ID_, TYPE_, 秒级时间, nodeKey, operator, opinion) 保留其一。
--    （仅清理本次回填可能引入的、与既有双写评论重叠的重复，不影响真实多节点意见）
-- ---------------------------------------------------------------------------
DELETE c1
FROM ACT_HI_COMMENT c1
JOIN ACT_HI_COMMENT c2
  ON c2.PROC_INST_ID_ = c1.PROC_INST_ID_
 AND c2.TYPE_        = c1.TYPE_
 AND DATE_FORMAT(c2.TIME_, '%Y-%m-%d %H:%i:%s') = DATE_FORMAT(c1.TIME_, '%Y-%m-%d %H:%i:%s')
 AND JSON_EXTRACT(c2.MESSAGE_, '$.nodeKey')  = JSON_EXTRACT(c1.MESSAGE_, '$.nodeKey')
 AND JSON_EXTRACT(c2.MESSAGE_, '$.operator') = JSON_EXTRACT(c1.MESSAGE_, '$.operator')
 AND JSON_EXTRACT(c2.MESSAGE_, '$.opinion')  = JSON_EXTRACT(c1.MESSAGE_, '$.opinion')
 AND c2.ID_ < c1.ID_;

-- ---------------------------------------------------------------------------
-- 3) 校验：回填后每个双写实例的 ACT 评论数应 >= 其 wf_approval_log 条数（覆盖到的）
-- ---------------------------------------------------------------------------
-- SELECT p.BUSINESS_ID_                                          AS wf_inst_id,
--        (SELECT COUNT(*) FROM wf_approval_log l
--             WHERE l.inst_id = p.BUSINESS_ID_)                   AS wf_log_cnt,
--        (SELECT COUNT(*) FROM ACT_HI_COMMENT c
--             WHERE c.PROC_INST_ID_ = p.ID_)                      AS act_comment_cnt
-- FROM ACT_HI_PROCINST p
-- WHERE p.BUSINESS_ID_ IS NOT NULL
--   AND act_comment_cnt < wf_log_cnt;
-- （结果应为空集；非空行代表仍有未覆盖实例，需确认是否属「双写前从未迁移」的实例）
