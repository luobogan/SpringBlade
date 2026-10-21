-- -----------------------------------------------------------------------------
-- 迁移脚本：订正 workflow_billfield 的字段HTML类型编码（历史脏数据）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_070
-- 目标库    : blade（dev 实测：jdbc:mysql://localhost:3306/blade，root/123456，
--              见 doc/nacos/blade.yaml 的 blade.datasource.dev）
--              说明：dev 环境下各服务共用 blade 库，workflow_billfield 就在其中
--              （它未出现在 doc/sql/blade/blade.sql 种子脚本里，但库中实际存在 508 行）。
-- 执行记录  : 2026-10-21 于 dev 库执行成功 —— 备份 508 行、订正 13 行、校验剩余 0 行。
-- 影响表    : workflow_billfield
-- 变更类型  : DML（幂等，可重复执行）
-- 是否幂等  : 是 —— 订正后原条件不再命中，第二次执行影响 0 行
-- 预计影响  : 13 行（fieldhtmltype=2 AND fieldtype=2）
-- 回滚      : 无精确回滚（订正后与原脏数据无法区分），执行前请先备份：
--             CREATE TABLE workflow_billfield_bak_20261021 AS SELECT * FROM workflow_billfield;
--
-- 背景
--   字段类型编码曾存在两套解释。唯一权威为前端
--   src/components/FormMode/fieldTypes.ts（自称「字段类型编码的唯一权威」，
--   且与后端 EcologyFormImportServiceImpl 的 HTML_* 常量一致）：
--       1文本字段 2多行文本 3浏览按钮 4选择框 5附件上传 6复选框 7特殊字段 8布局组件
--   「字段管理」页历史版本用了另一套错位编码（2=浏览按钮、3=选择框、4=附件上传…），
--   导致经该页面创建的字段把「浏览按钮」错存为 fieldhtmltype=2。
--   该页面现已改为走权威编码（getFieldHtmlTypeOptions / getFieldTypeOptions），
--   本脚本订正历史脏数据。
--
-- 为什么只订正 (fieldhtmltype=2, fieldtype=2) 这一个组合
--   1. 权威编码下 htmlType=2（多行文本）的 fieldtype 只有 1，
--      故 (2,2) 必然非法 → 一定是脏数据；
--   2. 导入器 EcologyFormImportServiceImpl.mapType() 对 FIELDHTMLTYPE=2 恒返回 1，
--      导入数据永远不可能产生 (2,2) → 只可能是旧「字段管理」页建的；
--   3. 旧编码语义 =「浏览按钮(htmlType=2) + 部门(fieldtype=2)」，
--      正确编码应为「浏览按钮(3) + 部门(2)」；且 2 在「10 类编号」与
--      「35+ 浏览按钮编号」两套空间中都表示「部门」，语义完全一致，
--      故只改 fieldhtmltype、不动 fieldtype。
--   4. 其余组合均为合法权威编码，尤其
--      3 与 24/26(文档)、30/31/32(流程)、57(附件)、98(日期)、161(多人力资源)、
--      256/257(自定义树形)、260~264(公文字典/文号) 属 35+ 编号空间
--      （旧 10 类编号只到 9），证明存量主体为正确导入编码 —— 一律不动。
--
-- 执行前诊断（期望：13 行）
--   SELECT fieldhtmltype, fieldtype, COUNT(*) c
--     FROM workflow_billfield
--    WHERE fieldhtmltype = 2 AND fieldtype <> 1
--    GROUP BY fieldhtmltype, fieldtype;

UPDATE workflow_billfield
   SET fieldhtmltype = 3
 WHERE fieldhtmltype = 2
   AND fieldtype = 2;

-- 执行后校验（期望：0 行）
--   SELECT COUNT(*) FROM workflow_billfield WHERE fieldhtmltype = 2 AND fieldtype <> 1;
