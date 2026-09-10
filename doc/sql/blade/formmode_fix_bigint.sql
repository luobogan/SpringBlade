-- =========================================================
-- 修复 workflow_bill 和 workflow_billfield 表结构
-- 使其兼容 BladeX 的 BIGINT 雪花ID
-- =========================================================

-- 1. 修改 workflow_bill 表 id 字段类型为 BIGINT AUTO_INCREMENT
ALTER TABLE `workflow_bill` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '表单ID';

-- 2. 修改 workflow_billfield 表：
--    id 字段改为 BIGINT AUTO_INCREMENT
--    billid 字段改为 BIGINT（匹配 form_definition.id 的 BIGINT 类型）
ALTER TABLE `workflow_billfield` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字段ID';
ALTER TABLE `workflow_billfield` MODIFY COLUMN `billid` BIGINT DEFAULT NULL COMMENT '所属表单ID';

-- 3. 同样修复其他相关表（如有）
-- ALTER TABLE `workflow_formbase` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

-- =========================================================
-- 执行完毕后，重启后端服务
-- =========================================================
