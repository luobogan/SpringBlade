-- ============================================================================
-- 手动迁移脚本（非 Flyway 自动执行）：blade_workflow 库 ACT_*/FLW_*/WF_* 并入 blade
-- 生成日期 : 2026-09-28
-- 背景     : 将 Flowable 7 引擎表(ACT_*) + 事件表(FLW_*) + 业务台账(WF_*) 从独立库
--            blade_workflow 集中到 blade 库，与 workflow_bill / formtable_main_* 等同库，
--            消除跨库引用（wf_instance.form_id -> workflow_bill.id / data_id -> formtable_main_*.id）。
-- 数据说明 : RENAME TABLE 移动的是【整张表】（表结构 + 全部数据行一并迁移），无需额外的 INSERT/数据导出步骤；
--            因此本脚本天然包含数据迁移，不会只搬结构漏数据（等价于把 .ibd 文件与元数据整体挪到 blade 库）。
--
-- 前置条件（务必确认）:
--   1. blade_workflow 与 blade 【同一 MySQL 实例】。跨实例 RENAME 不支持，须改用 mysqldump：
--        mysqldump --no-create-info --single-transaction blade_workflow \
--          $(mysql -N -e "SHOW TABLES FROM blade_workflow LIKE 'act_%'" ...) | mysql blade
--   2. 已停 blade-workflow 服务（无增量写入，避免丢数据）。
--   3. blade 库【不存在】 act_/flw_/wf_ 表（由下方守卫校验；有冲突则先解决）。
--
-- 执行顺序（与“建议实施路径”一致）:
--   (1) 停服  (2) 执行本脚本  (3) 改 Nacos blade.datasource.workflow.url -> .../blade
--   (4) 启动验证 FlowableConfig 日志 [映射自检] catalog=blade 且版本一致
--   (5) 跑 drift_check 6a/6b/6c 全 0；走一遍暂停/恢复/撤回回归
--   (6) 旧库 blade_workflow 留空观察，确认无误后择期 DROP（勿立即删）
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 0) 守卫：列出 blade 库中已存在的 act_/flw_/wf_ 表。
--    有任意结果 => 说明目标表已存在，【手动停止】，先解决冲突再继续。
--    返回 0 行 => 可安全执行第 1 步。
-- ---------------------------------------------------------------------------
SELECT 'CONFLICT_TABLES_IN_BLADE' AS check_type, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blade'
  AND TABLE_NAME REGEXP '^(act_|flw_|wf_)';


-- ---------------------------------------------------------------------------
-- 1) 动态 RENAME：把 blade_workflow 所有 act_/flw_/wf_ 表跨库搬到 blade。
--    RENAME TABLE 跨库为【元数据操作】：秒级、原子、不放长事务锁，适合大表（ACT_HI_*）。
--    外键检查临时关闭（整库搬迁；迁移后由应用层 + drift_check 保证引用完整性）。
--    目标表已存在则跳过（幂等、防覆盖）。
-- ---------------------------------------------------------------------------
-- 选定默认库：CREATE PROCEDURE 需要一个默认 database（过程本身只读写 information_schema 并 RENAME 到 blade）
USE blade;
DROP PROCEDURE IF EXISTS _migrate_wf_tables;
DELIMITER $$
CREATE PROCEDURE _migrate_wf_tables()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tname VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = 'blade_workflow'
          AND TABLE_NAME REGEXP '^(act_|flw_|wf_)';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SET FOREIGN_KEY_CHECKS = 0;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tname;
        IF done THEN LEAVE read_loop; END IF;

        -- 目标已存在则跳过，避免覆盖 blade 已有表
        SET @exists = (SELECT COUNT(*)
                       FROM information_schema.TABLES
                       WHERE TABLE_SCHEMA = 'blade' AND TABLE_NAME = tname);
        IF @exists = 0 THEN
            SET @sql = CONCAT('RENAME TABLE blade_workflow.', tname, ' TO blade.', tname);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
    SET FOREIGN_KEY_CHECKS = 1;
END$$
DELIMITER ;

CALL _migrate_wf_tables();
DROP PROCEDURE IF EXISTS _migrate_wf_tables;


-- ---------------------------------------------------------------------------
-- 2) 校验：两库表计数。blade_workflow 应归零，blade 应等于迁移数（且 > 0）。
-- ---------------------------------------------------------------------------
SELECT 'blade_workflow 剩余(应=0)' AS label,
       COUNT(*) AS cnt
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blade_workflow' AND TABLE_NAME REGEXP '^(act_|flw_|wf_)';

SELECT 'blade 现有(应=迁移总数)' AS label,
       COUNT(*) AS cnt
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blade' AND TABLE_NAME REGEXP '^(act_|flw_|wf_)';


-- ---------------------------------------------------------------------------
-- 3) 收尾：blade_workflow 已清空，可保留空壳观察或择期 DROP（MySQL 不支持 RENAME DATABASE）。
--    -- 确认无误后执行：
--    -- DROP DATABASE blade_workflow;
-- ---------------------------------------------------------------------------
