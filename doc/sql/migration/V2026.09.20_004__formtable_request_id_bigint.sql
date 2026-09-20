-- ============================================================================
-- V2026.09.20_004  业务表 request_id 改为 BIGINT（存雪花流程实例ID）
-- ============================================================================
-- 背景：
--   动态业务表 formtable_main_N 的 request_id 列语义 = 关联的流程实例ID
--   （wf_instance.id，19 位雪花，> 2^31），早期 DDL 建成 INT：
--     `request_id` INT DEFAULT NULL COMMENT '关联流程ID'
--   INT 上限 2147483647，存雪花必然溢出/截断，导致「单据 ↔ 流程」反查失效。
--
-- 改动：
--   1) 建表 DDL 已同步改为 BIGINT（两处）：
--        - DynamicTableServiceImpl#createMainTable
--        - DynamicTableServiceImpl#createTableWithAllColumns（同步表结构用）
--      FormModeServiceImpl#createDynamicTable（旧链路，列名 requestId）也已改 BIGINT。
--   2) 存量表用本脚本 MODIFY 成 BIGINT。
--
-- 幂等：MODIFY COLUMN 可重复执行（已是 BIGINT 时无副作用）。
-- 执行：mysql -uroot -p*** -t -e "source d:/.../V2026.09.20_004__formtable_request_id_bigint.sql" blade
-- ============================================================================

-- 动态主表（当前库内仅主表带 request_id；明细表用 main_id 关联）
ALTER TABLE `formtable_main_1` MODIFY COLUMN `request_id` BIGINT DEFAULT NULL COMMENT '关联流程实例ID（wf_instance.id，雪花）';
ALTER TABLE `formtable_main_2` MODIFY COLUMN `request_id` BIGINT DEFAULT NULL COMMENT '关联流程实例ID（wf_instance.id，雪花）';
ALTER TABLE `formtable_main_3` MODIFY COLUMN `request_id` BIGINT DEFAULT NULL COMMENT '关联流程实例ID（wf_instance.id，雪花）';
ALTER TABLE `formtable_main_5` MODIFY COLUMN `request_id` BIGINT DEFAULT NULL COMMENT '关联流程实例ID（wf_instance.id，雪花）';

-- 历史命名兜底：若某表用 camelCase 的 requestId（FormModeServiceImpl 旧链路建的表），一并修正
-- （表不存在时该语句报错，可按需注释；当前库无此类表）

-- 校验：应返回 0 行（仍为 int 的 request_id / requestId）
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'formtable_main%'
  AND LOWER(COLUMN_NAME) IN ('request_id', 'requestid')
  AND DATA_TYPE = 'int';
