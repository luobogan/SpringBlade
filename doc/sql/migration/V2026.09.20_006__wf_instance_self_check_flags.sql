-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_instance 补「L3 运行时自检」三个标志列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.20_006
-- 目标库    : blade_workflow
-- 影响表    : wf_instance
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_instance`
--                 DROP COLUMN `business_row_ready`,
--                 DROP COLUMN `request_id_bound`,
--                 DROP COLUMN `engine_deployment_matched`;
--
-- 背景（规范 §2-L3「运行时自检」）
--   发起时的三类隐患此前只在后端日志里 warn，前端与巡检都拿不到：
--     ① 业务数据行没建成（表单直发时跨服务建行失败 → 用占位 dataId，业务表里查不到这张单）
--     ② 业务行 request_id 没回填（流程 ↔ 单据 闭环断了）
--     ③ 引擎 latest 部署 ≠ 本定义 deployment_id（正式版本被测试/手工部署顶替）
--   落库后随 InstanceVO 返回前端（发起成功页可直接提示），巡检脚本也能一条 SQL 查全。
--
--   取值语义（三列一致）：1 = 正常 / 0 = 异常 / NULL = 未知或不自检
--     business_row_ready       1 业务表有对应行；0 仅占位（业务行创建失败）
--     request_id_bound         1 已回填；0 未回填；NULL 无需回填（单据发起由单据侧维护关联）
--     engine_deployment_matched 1 引擎 latest == 定义 deployment_id；0 被顶替；
--                               NULL 未知（定义未落 deployment_id，或本次为测试态发起）
--
--   存量数据：三列全为 NULL（= 未自检），前端与巡检按"未知"处理，不误报为异常。
--
-- ⚠️ 执行顺序：先执行 V2026.09.20_005（deployment_id），再执行本脚本，最后重启 blade-workflow。
-- ⚠️ 严禁使用 workflow-schema.sql 同步结构（会 DROP TABLE 清空数据）。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_instance';

-- business_row_ready ----------------------------------------------------------
SET @v_col = 'business_row_ready';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_instance`
         ADD COLUMN `business_row_ready` TINYINT NULL
         COMMENT ''L3自检：业务数据行已就绪 1=有对应行 0=仅占位 NULL=未自检''
         AFTER `is_test`',
    'SELECT ''SKIP: wf_instance.business_row_ready already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- request_id_bound ------------------------------------------------------------
SET @v_col = 'request_id_bound';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_instance`
         ADD COLUMN `request_id_bound` TINYINT NULL
         COMMENT ''L3自检：业务行 request_id 已回填 1=是 0=否 NULL=无需/未自检''
         AFTER `business_row_ready`',
    'SELECT ''SKIP: wf_instance.request_id_bound already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- engine_deployment_matched ----------------------------------------------------
SET @v_col = 'engine_deployment_matched';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_instance`
         ADD COLUMN `engine_deployment_matched` TINYINT NULL
         COMMENT ''L3自检：引擎 latest 部署 == 定义 deployment_id 1=一致 0=被顶替 NULL=未知''
         AFTER `request_id_bound`',
    'SELECT ''SKIP: wf_instance.engine_deployment_matched already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（三项均应返回 1）---------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_instance'
        AND COLUMN_NAME = 'business_row_ready')         AS business_row_ready_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_instance'
        AND COLUMN_NAME = 'request_id_bound')           AS request_id_bound_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_instance'
        AND COLUMN_NAME = 'engine_deployment_matched')  AS engine_deployment_matched_should_be_1;
