-- ============================================================================
-- SpringBlade 表单建模 - 重命名 form_definition 表为 workflow_bill
-- ============================================================================

USE `blade`;

-- 检查 form_definition 表是否存在，如果存在则重命名为 workflow_bill
-- 注意：根据 schema SQL，表名应该已经是 workflow_bill
-- 此脚本用于确保表名正确

-- 1. 检查 form_definition 表是否存在
-- SELECT COUNT(*) FROM information_schema.tables 
-- WHERE table_schema = 'blade' AND table_name = 'form_definition';

-- 2. 如果 form_definition 表存在，重命名为 workflow_bill
-- RENAME TABLE `form_definition` TO `workflow_bill`;

-- 3. 验证表名
-- SELECT table_name FROM information_schema.tables 
-- WHERE table_schema = 'blade' AND (table_name = 'form_definition' OR table_name = 'workflow_bill');

-- ============================================================================
-- 说明
-- ============================================================================
-- 根据项目 schema SQL (formmode-schema.sql)，表单定义表已经命名为 `workflow_bill`。
-- 如果实际数据库中表名是 `form_definition`，请执行上面的 RENAME 语句。
-- 
-- 后端实体类已经更新：
-- - FormDefinition.java → WorkflowBill.java
-- - @TableName("form_definition") → @TableName("workflow_bill")
-- 
-- 前端类型定义已经更新：
-- - FormDefinition → WorkflowBill
-- - FormDefinitionFormData → WorkflowBillFormData
-- - formDefinitionApi → workflowBillApi
-- ============================================================================
