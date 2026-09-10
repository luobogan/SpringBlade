-- =============================================================================
-- 迁移脚本 009：workflow_bill 补「表单类型」字段（区分自定义表单 / 系统表单）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_009
-- 目标库    : blade（workflow_bill 在 blade 库，见 blade-formmode/sql/formmode-schema.sql）
-- 影响表    : workflow_bill
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `workflow_bill` DROP COLUMN `form_type`;
--
-- 背景
--   「流程设计 → 添加路径 → 对应表单」需按 ecology isBill 联动两种表单：
--     formType = 0  自定义表单：用户在「表设计」(formmode/tabledesign) 中设计生成的表
--     formType = 1  系统表单  ：平台预置的标准表
--   两者的 formId 都必须来自 workflow_bill —— 流程画布渲染表单时调用
--   formmode.getFormLayout(formId, ...)，只有 workflow_bill 中的表单才有布局数据。
--   故区分二者的唯一方式是在 workflow_bill 上标记表单类型，而不是另一张表。
--
--   实体 WorkflowBill 已同步增加 formType（blade-formmode），
--   GET /api/blade-formmode/form-definition/all 直接返回实体，无需改动接口，
--   前端即可按 type 过滤下拉。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-formmode，否则接口不返回 form_type。
--
-- 说明
--   * 列设为 NULL DEFAULT 0：表设计器新建表单时不传该列也不会报错，
--     已存在行自动视为 0（自定义表单），与改动前行为一致。
--   * 系统表单需要平台预置数据；若库中尚无 form_type=1 的记录，
--     「系统表单」下拉会为空（前端会提示「暂无系统预置表单」）。
--     如需把某张已有表登记为系统表单，执行下方第 2 段示例 SQL。
-- =============================================================================

USE `blade`;

SET @v_db  = DATABASE();
SET @v_tbl = 'workflow_bill';

-- 1) form_type ---------------------------------------------------------------
SET @v_col = 'form_type';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `workflow_bill`
         ADD COLUMN `form_type` TINYINT NULL DEFAULT 0
         COMMENT ''表单类型：0自定义表单 1系统表单''
         AFTER `table_name`',
    'SELECT ''SKIP: workflow_bill.form_type already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 2)（可选）把已有表登记为系统表单 ---------------------------------------------
--    取消注释并把条件改成你的目标表单（按 id 或 tablename 均可），执行一次即可。
-- UPDATE `workflow_bill` SET `form_type` = 1 WHERE `id` IN (/* 表单ID */) AND `is_deleted` = 0;
-- UPDATE `workflow_bill` SET `form_type` = 1 WHERE `tablename` IN ('formtable_main_1') AND `is_deleted` = 0;

-- 校验（应返回 1）--------------------------------------------------------------
SELECT COUNT(1) AS form_type_should_be_1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'workflow_bill'
  AND COLUMN_NAME  = 'form_type';

-- 各类型表单数量（确认 0/1 分布）------------------------------------------------
-- 先投影出 ft 再分组：避免 SELECT 裸列 form_type + GROUP BY 表达式
-- 触发 sql_mode=only_full_group_by 的 "not functionally dependent" 报错。
SELECT ft AS form_type,
       CASE ft WHEN 1 THEN '系统表单' ELSE '自定义表单' END AS form_type_name,
       COUNT(1) AS cnt
FROM (
    SELECT IFNULL(`form_type`, 0) AS ft
    FROM `workflow_bill`
    WHERE `is_deleted` = 0
) t
GROUP BY ft;
