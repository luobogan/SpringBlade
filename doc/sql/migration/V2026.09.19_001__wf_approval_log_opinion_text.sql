-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_approval_log.opinion 扩容为 TEXT（审批意见改为富文本 HTML）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.19_001
-- 目标库    : blade_workflow（wf_approval_log 所在库）
-- 影响表    : wf_approval_log（列类型变更）
-- 变更类型  : DDL
-- 是否幂等  : 是（先查 information_schema 当前类型，已是 TEXT 则跳过）
-- 是否丢数据: 否（VARCHAR(2000) → TEXT，纯扩容）
-- 回滚脚本  :
--   ALTER TABLE `wf_approval_log` MODIFY COLUMN `opinion` VARCHAR(2000) NOT NULL COMMENT '审批意见';
--
-- 背景
--   审批意见改由富文本编辑器（TinyMCE，见前端 components/RichTextEditor）录入，
--   提交/落库的是 HTML 片段（如 <p><b>同意</b>，请按期付款</p>）。
--   原 VARCHAR(2000) 太短：带颜色/列表/多段落/表格的意见实测可达数千字符，
--   超长会触发 MySQL 严格模式报 “Data too long for column 'opinion'”。
--
-- 注意
--   wf_approval_log 按 operate_time 做 RANGE 分区，opinion 不参与分区键，可直接 MODIFY。
--   另需把 /blade-workflow/** 加入 Nacos blade.yaml 的 xss.skip-url，
--   否则富文本标签会被 XssFilter 清洗掉（参见该文件的注释）。

SET @col_type := (
    SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_approval_log'
      AND COLUMN_NAME = 'opinion'
);

SET @ddl := IF(@col_type IS NULL OR @col_type = 'text',
    'SELECT ''wf_approval_log.opinion 已是 TEXT，跳过'' AS result',
    'ALTER TABLE `wf_approval_log` MODIFY COLUMN `opinion` TEXT NOT NULL COMMENT ''审批意见（富文本 HTML）''');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
