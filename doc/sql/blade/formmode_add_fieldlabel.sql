-- 添加 fieldlabel 列到 workflow_billfield 表
-- 用于存字段显示名称（字段标签）

USE `blade`;

ALTER TABLE `workflow_billfield` 
ADD COLUMN `fieldlabel` VARCHAR(500) DEFAULT NULL COMMENT '字段标签（显示名称）' AFTER `fieldname`;

-- 验证
-- SELECT id, billid, fieldname, fieldlabel, fielddbname FROM workflow_billfield LIMIT 10;
