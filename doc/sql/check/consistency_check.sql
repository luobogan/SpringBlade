-- ============================================================================
-- 测试流程 ↔ 正式流程 一致性巡检（一条命令跑完全部 8 项）
-- ============================================================================
-- 依据：《测试流程与正式流程一致性规范》§2-L4
--
-- 执行（PowerShell 下不能用 < 重定向，用 source）：
--   mysql -uroot -p*** -t -e "source d:/workproject/springbladeandreact/springBlade/doc/sql/check/consistency_check.sql" blade
--
-- 检查项：
--   ① 正式实例的业务数据行缺失（data_id 在业务表找不到）
--   ② request_id 未回填 / 与实例 id 不一致
--   ③ 活跃版本的 BPMN / 节点配置为空（粗粒度结构校验；精确的「BPMN 元素 ↔ wf_process_node」
--      比对在发布门禁里做，见 WfDefinitionServiceImpl#validateForDeploy）
--   ④ 存在未清理的测试实例（is_test=1）：正式发起会被守卫拦住，且可能残留测试部署
--   ⑤ 字段定义 / 布局字段名未落到业务表列（会导致「填了不落库」，写入端会静默忽略）
--   ⑥ 引擎中各 procKey 的最新部署信息（人工判读：是否被测试部署顶替 / 是否含 manualTask 消毒痕迹）
--   ⑦ 引擎 latest 部署 == wf_process_definition.deployment_id（精确比对，需先执行 V2026.09.20_005）
--   ⑧ 实例 L3 自检异常（发起时自检：业务行仅占位 / request_id 未回填 / 引擎 latest 被顶替）
--      （需先执行 V2026.09.20_006；存量实例三列为 NULL = 未自检，按"未知"处理不误报）
--
-- 说明：业务表名一律取 workflow_bill.table_name（迁移表单表名 ≠ formtable_main_{formId}），
--       故 ①② 走游标 + 预处理语句逐表比对；⑤-2 用过程式循环抽布局字段名（不依赖 JSON 通配符）。
-- 输出：明细（按检查项排序）+ 末尾 ERROR/WARN 计数（ERROR > 0 需处理）
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS t_cc;
CREATE TEMPORARY TABLE t_cc (
    no     INT,
    item   VARCHAR(64),
    level  VARCHAR(8),
    detail VARCHAR(600)
);

DROP TEMPORARY TABLE IF EXISTS t_lf;
CREATE TEMPORARY TABLE t_lf (
    form_id    BIGINT,
    node_key   VARCHAR(64),
    field_name VARCHAR(128)
);

-- ---------------------------------------------------------------------------
-- 过程 1：逐表单巡检 ①②
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_cc;
DELIMITER $$
CREATE PROCEDURE sp_cc()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_form_id BIGINT;
    DECLARE v_table VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT id, table_name FROM blade.workflow_bill WHERE is_deleted = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    tbl_loop: LOOP
        FETCH cur INTO v_form_id, v_table;
        IF done = 1 THEN
            LEAVE tbl_loop;
        END IF;

        IF v_table IS NULL OR v_table = '' THEN
            INSERT INTO t_cc
            VALUES (1, '业务表名为空', 'WARN', CONCAT('formId=', v_form_id, '（workflow_bill.table_name 未配置）'));
        ELSE
            -- ① 正式实例在业务表里找不到对应行
            SET @sql = CONCAT(
                'INSERT INTO t_cc SELECT 1,''实例业务行缺失'',''ERROR'',',
                'CONCAT(''formId=', v_form_id, ' 实例'',i.id,'' data_id='',i.data_id,'' bizKey='',i.biz_key)',
                ' FROM blade_workflow.wf_instance i LEFT JOIN blade.`', v_table, '` b ON b.id = i.data_id',
                ' WHERE i.is_deleted = 0 AND i.is_test = 0 AND i.form_id = ', v_form_id, ' AND b.id IS NULL');
            PREPARE s1 FROM @sql; EXECUTE s1; DEALLOCATE PREPARE s1;

            -- ② request_id 未回填 / 与实例 id 不一致
            SET @sql = CONCAT(
                'INSERT INTO t_cc SELECT 2,''request_id 未回填/不一致'',''ERROR'',',
                'CONCAT(''formId=', v_form_id, ' 实例'',i.id,'' request_id='',IFNULL(b.request_id,''NULL''))',
                ' FROM blade_workflow.wf_instance i JOIN blade.`', v_table, '` b ON b.id = i.data_id',
                ' WHERE i.is_deleted = 0 AND i.is_test = 0 AND i.form_id = ', v_form_id,
                ' AND (b.request_id IS NULL OR b.request_id <> i.id)');
            PREPARE s2 FROM @sql; EXECUTE s2; DEALLOCATE PREPARE s2;
        END IF;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 过程 2：抽取所有启用布局里的字段名（fieldMeta.fieldName）→ t_lf
--   索引式访问（JSON_KEYS + JSON_LENGTH + $[n]），避免依赖 JSON 通配符语义
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_dump_layout_fields;
DELIMITER $$
CREATE PROCEDURE sp_dump_layout_fields()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_form BIGINT;
    DECLARE v_node VARCHAR(64);
    DECLARE v_lj LONGTEXT;
    DECLARE i INT; DECLARE n INT;
    DECLARE j INT; DECLARE m INT;
    DECLARE a INT; DECLARE b INT;
    DECLARE sid VARCHAR(64); DECLARE rw VARCHAR(16); DECLARE cl VARCHAR(16); DECLARE fn VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT form_id, node_key, layout_json FROM blade.form_layout WHERE status = 1;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    l1: LOOP
        FETCH cur INTO v_form, v_node, v_lj;
        IF done = 1 THEN
            LEAVE l1;
        END IF;
        IF v_lj IS NULL OR v_lj = '' OR JSON_VALID(v_lj) = 0 THEN
            ITERATE l1;
        END IF;

        SET n = IFNULL(JSON_LENGTH(JSON_KEYS(JSON_EXTRACT(v_lj, '$.sheets'))), 0);
        SET i = 0;
        WHILE i < n DO
            SET sid = JSON_UNQUOTE(JSON_EXTRACT(JSON_KEYS(JSON_EXTRACT(v_lj, '$.sheets')), CONCAT('$[', i, ']')));
            SET m = IFNULL(JSON_LENGTH(JSON_KEYS(JSON_EXTRACT(v_lj, CONCAT('$.sheets."', sid, '".cellData')))), 0);
            SET j = 0;
            WHILE j < m DO
                SET rw = JSON_UNQUOTE(JSON_EXTRACT(
                            JSON_KEYS(JSON_EXTRACT(v_lj, CONCAT('$.sheets."', sid, '".cellData'))),
                            CONCAT('$[', j, ']')));
                SET a = IFNULL(JSON_LENGTH(JSON_KEYS(
                            JSON_EXTRACT(v_lj, CONCAT('$.sheets."', sid, '".cellData."', rw, '"')))), 0);
                SET b = 0;
                WHILE b < a DO
                    SET cl = JSON_UNQUOTE(JSON_EXTRACT(
                                JSON_KEYS(JSON_EXTRACT(v_lj, CONCAT('$.sheets."', sid, '".cellData."', rw, '"'))),
                                CONCAT('$[', b, ']')));
                    SET fn = JSON_UNQUOTE(JSON_EXTRACT(v_lj,
                                CONCAT('$.sheets."', sid, '".cellData."', rw, '"."', cl, '".fieldMeta.fieldName')));
                    IF fn IS NOT NULL AND fn <> '' AND fn <> 'null' THEN
                        INSERT INTO t_lf VALUES (v_form, v_node, fn);
                    END IF;
                    SET b = b + 1;
                END WHILE;
                SET j = j + 1;
            END WHILE;
            SET i = i + 1;
        END WHILE;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL sp_cc();
CALL sp_dump_layout_fields();
DROP PROCEDURE IF EXISTS sp_cc;
DROP PROCEDURE IF EXISTS sp_dump_layout_fields;

-- ③ 活跃版本结构（粗粒度：BPMN 为空 / 存活节点为 0 ⇒ 引擎能跑但语义层没配置）
INSERT INTO t_cc
SELECT 3, '活跃版本结构异常', 'ERROR',
       CONCAT('defId=', d.id, ' ', IFNULL(d.name, ''), ' bpmn长度=',
              IFNULL(LENGTH(d.bpmn_xml), 0), 'B 存活节点数=',
              (SELECT COUNT(*) FROM blade_workflow.wf_process_node n
                WHERE n.def_id = d.id AND n.is_deleted = 0))
FROM blade_workflow.wf_process_definition d
WHERE d.is_deleted = 0 AND d.status = 1
  AND (d.bpmn_xml IS NULL OR d.bpmn_xml = ''
       OR (SELECT COUNT(*) FROM blade_workflow.wf_process_node n
            WHERE n.def_id = d.id AND n.is_deleted = 0) = 0);

-- ④ 未清理的测试实例（正式发起会被守卫拦住，需先在测试页「清理测试数据」）
INSERT INTO t_cc
SELECT 4, '未清理的测试实例', 'WARN',
       CONCAT('count=', COUNT(*), ' 最新实例=', IFNULL(MAX(id), '-'), ' 涉及流程数=', COUNT(DISTINCT def_id))
FROM blade_workflow.wf_instance
WHERE is_test = 1 AND is_deleted = 0
HAVING COUNT(*) > 0;

-- ⑤-1 字段定义（workflow_billfield.fieldname）未落到业务表列 → 单元格写入会被静默忽略
INSERT INTO t_cc
SELECT 5, '字段定义未落到业务表列', 'ERROR',
       CONCAT('formId=', b.id, ' 表=', b.table_name, ' 缺列=', f.fieldname)
FROM blade.workflow_bill b
JOIN blade.workflow_billfield f ON f.billid = b.id
LEFT JOIN information_schema.COLUMNS c
       ON c.TABLE_SCHEMA = 'blade' AND c.TABLE_NAME = b.table_name AND c.COLUMN_NAME = f.fieldname
WHERE b.is_deleted = 0 AND c.COLUMN_NAME IS NULL
GROUP BY b.id, b.table_name, f.fieldname;

-- ⑤-2 布局字段名（不含明细表子画布）未落到业务表列（主表或任一明细表）
INSERT INTO t_cc
SELECT DISTINCT 5, '布局字段名未落到业务表列', 'ERROR',
       CONCAT('formId=', lf.form_id, ' 节点=', IFNULL(lf.node_key, '(表单级)'), ' 缺列=', lf.field_name)
FROM t_lf lf
JOIN blade.workflow_bill b ON b.id = lf.form_id AND b.is_deleted = 0
LEFT JOIN information_schema.COLUMNS col
       ON col.TABLE_SCHEMA = 'blade'
      AND (col.TABLE_NAME = b.table_name OR col.TABLE_NAME LIKE CONCAT(b.table_name, '\\_dt%'))
      AND col.COLUMN_NAME = lf.field_name
WHERE col.COLUMN_NAME IS NULL;

-- ⑥ 引擎中各 procKey 的最新部署（人工判读：若「消毒标记=有」或时间晚于正式发布 → 被测试部署顶替）
INSERT INTO t_cc
SELECT 6, '引擎最新部署', 'INFO',
       CONCAT('key=', pd.KEY_, ' ver=', pd.VERSION_, ' 部署时间=', d.DEPLOY_TIME_,
              ' bpmn=', IFNULL(LENGTH(b.BYTES_), 0), 'B 消毒标记=',
              IF(LOCATE('manualTask', CONVERT(b.BYTES_ USING utf8mb4)) > 0, '有', '无'))
FROM blade_workflow.ACT_RE_PROCDEF pd
JOIN blade_workflow.ACT_RE_DEPLOYMENT d ON d.ID_ = pd.DEPLOYMENT_ID_
LEFT JOIN blade_workflow.ACT_GE_BYTEARRAY b
       ON b.DEPLOYMENT_ID_ = d.ID_ AND b.NAME_ LIKE '%.bpmn20.xml'
WHERE pd.VERSION_ = (SELECT MAX(p2.VERSION_) FROM blade_workflow.ACT_RE_PROCDEF p2 WHERE p2.KEY_ = pd.KEY_)
  AND pd.KEY_ IN (SELECT DISTINCT proc_key FROM blade_workflow.wf_process_definition WHERE is_deleted = 0);

-- ⑦ 引擎 latest 部署 == 定义记录的 deployment_id（精确比对，替代此前的"人工判读"）
--    只比对「已发布 + 已落 deployment_id」的定义；deployment_id 为 NULL = 未落库（存量），跳过不误报。
INSERT INTO t_cc
SELECT 7, '引擎 latest ≠ 正式部署', 'ERROR',
       CONCAT('defId=', d.id, ' ', IFNULL(d.name, ''), ' procKey=', d.proc_key,
              ' 定义部署=', d.deployment_id,
              ' 引擎latest=', IFNULL(pd.DEPLOYMENT_ID_, 'NULL'))
FROM blade_workflow.wf_process_definition d
LEFT JOIN blade_workflow.ACT_RE_PROCDEF pd
       ON pd.KEY_ = d.proc_key
      AND pd.VERSION_ = (SELECT MAX(p2.VERSION_) FROM blade_workflow.ACT_RE_PROCDEF p2 WHERE p2.KEY_ = d.proc_key)
WHERE d.is_deleted = 0 AND d.status = 1 AND d.deployment_id IS NOT NULL
  AND (pd.DEPLOYMENT_ID_ IS NULL OR pd.DEPLOYMENT_ID_ <> d.deployment_id);

-- ⑧ 实例 L3 自检异常（发起时自检写库，见 WfInstanceServiceImpl#start）
--    business_row_ready=0：业务表查不到这张单（跨服务建行失败，用了占位 dataId）
--    request_id_bound=0  ：业务行 request_id 未回填（流程↔单据闭环断）
--    engine_deployment_matched=0：正式发起时引擎 latest 不是本定义的正式部署（被顶替）
INSERT INTO t_cc
SELECT 8,
       CASE
           WHEN i.business_row_ready = 0 THEN 'L3自检：业务行仅占位'
           WHEN i.request_id_bound = 0 THEN 'L3自检：request_id 未回填'
           ELSE 'L3自检：引擎 latest 被顶替'
       END,
       CASE
           WHEN i.business_row_ready = 0 THEN 'ERROR'
           WHEN i.request_id_bound = 0 THEN 'ERROR'
           ELSE 'ERROR'
       END,
       CONCAT('实例=', i.id, ' defId=', i.def_id, ' dataId=', i.data_id,
              ' 业务行=', IFNULL(i.business_row_ready, 'NULL'),
              ' request_id=', IFNULL(i.request_id_bound, 'NULL'),
              ' 引擎部署=', IFNULL(i.engine_deployment_matched, 'NULL'))
FROM blade_workflow.wf_instance i
WHERE i.is_deleted = 0 AND i.is_test = 0
  AND (i.business_row_ready = 0 OR i.request_id_bound = 0 OR i.engine_deployment_matched = 0);

-- ============================ 结果 ============================
SELECT no AS 序号, level AS 级别, item AS 检查项, detail AS 详情
FROM t_cc ORDER BY no, level, detail;

SELECT COUNT(*) AS ERROR_总数,
       SUM(level = 'WARN') AS WARN_总数
FROM t_cc WHERE level IN ('ERROR', 'WARN');
