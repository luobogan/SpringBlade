-- 修复 workflow_bill.id 和 workflow_billfield.billid 类型溢出问题
-- 将 INT 改为 BIGINT 以兼容 form_definition.id (雪花ID/BIGINT)

-- 1. 修改 workflow_bill 表 id 字段类型为 BIGINT
ALTER TABLE `workflow_bill` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '表单ID';

-- 2. 修改 workflow_billfield 表 billid 字段类型为 BIGINT
ALTER TABLE `workflow_billfield` MODIFY COLUMN `billid` BIGINT DEFAULT NULL COMMENT '所属表单ID';

-- 3. 如果 workflow_billfield.id 也是 INT，一并修改
ALTER TABLE `workflow_billfield` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '字段ID';

-- 4. 修改 form_definition 表，添加 bill_id 字段关联 workflow_bill（可选，用于双写兼容）
-- ALTER TABLE `form_definition` ADD COLUMN `bill_id` INT DEFAULT NULL COMMENT '关联的 ecology 表单ID' AFTER `id`;

-- 注意：执行前请备份数据！
-- 如果表中已有大量数据，ALTER TABLE 可能会锁表，请在维护窗口执行。
