-- =============================================================================
-- 迁移脚本 002：wf_process_definition 唯一键纳入 is_deleted
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.24_002
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 改唯一索引（DDL）
-- 是否幂等  : 是（已是 3 列则跳过）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_process_definition`
--     DROP INDEX `uk_proc_key_version`,
--     ADD UNIQUE KEY `uk_proc_key_version` (`proc_key`, `version`);
--
-- 背景
--   removeDefinition 走 MyBatis-Plus 逻辑删除（is_deleted 置 1，物理行仍在），
--   而原唯一键 uk_proc_key_version(proc_key, version) 是库表层约束、不看 is_deleted。
--   于是「删掉一个定义后又用相同 proc_key+version 重新导入」会撞 Duplicate entry，
--   导入 500。把 is_deleted 纳入唯一键后：
--     - 活跃行（is_deleted=0）仍互斥，不会真的重复；
--     - 一条软删行（is_deleted=1）可与活跃行共存，重新导入不再冲突。
--
--   已知边界：若对同一 proc_key+version 连续软删两次（中间不重新导入），
--   会出现两条 (proc_key, version, is_deleted=1)，此时仍会撞键。常规「删→重导」流程
--   不会触发；若需彻底免疫，请改用方案 1（importNewDefinition 插入前物理清旧记录）。
--
-- ⚠️ 执行顺序：在目标库 blade_workflow 执行本脚本，无需重启服务（仅索引变更）。
--   执行方式（服务器本机，root@localhost 可连）：
--     mysql -u root -p123456 blade_workflow < V2026.09.24_002__wf_process_definition_uk_include_is_deleted.sql
-- =============================================================================

USE `blade_workflow`;

-- 预检：是否存在 (proc_key, version, is_deleted) 重复行，若有则加唯一键会失败-------------
SELECT proc_key, version, is_deleted, COUNT(*) AS dup_cnt
FROM wf_process_definition
GROUP BY proc_key, version, is_deleted
HAVING COUNT(*) > 1;

-- 读取当前 uk_proc_key_version 的列组成（用于幂等判定）--------------------------------
SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) INTO @v_cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND INDEX_NAME   = 'uk_proc_key_version';

-- 旧 2 列 -> 删旧加新；已是 3 列 -> 跳过；索引不存在 -> 直接加新（均幂等）----------------
SET @v_sql = CASE
    WHEN @v_cols = 'proc_key,version' THEN
        'ALTER TABLE `wf_process_definition` '
        'DROP INDEX `uk_proc_key_version`, '
        'ADD UNIQUE KEY `uk_proc_key_version` (`proc_key`, `version`, `is_deleted`)'
    WHEN @v_cols = 'proc_key,version,is_deleted' THEN
        'SELECT ''SKIP: uk_proc_key_version already (proc_key,version,is_deleted)'' AS migration_info'
    ELSE
        'ALTER TABLE `wf_process_definition` '
        'ADD UNIQUE KEY `uk_proc_key_version` (`proc_key`, `version`, `is_deleted`)'
END;

PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 校验：应返回 uk_proc_key_version 的三列 --------------------------------------------
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND INDEX_NAME   = 'uk_proc_key_version'
ORDER BY SEQ_IN_INDEX;
