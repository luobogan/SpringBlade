-- =============================================================================
-- 迁移脚本 V2026.11.21_001：按主体字段权限唯一约束
-- -----------------------------------------------------------------------------
-- 目标库：blade
-- 影响表：modefieldauthorize
-- 说明：
--   对象优先字段权限保存会按 (tenant, objtype, objid, modeid, fieldid) upsert，
--   必须先清理历史重复数据并加唯一索引，防止并发写入产生重复权限行。
--   并发版本校验继续复用现有 modeId 级 Redis 版本（/right/version/{modeId}），
--   本脚本不新增版本表。
-- 回滚：
--   ALTER TABLE modefieldauthorize DROP INDEX uk_field_auth_subject;
-- =============================================================================

USE `blade`;

-- 1) 清理重复数据：同一主体/模块/字段只保留最小 id 一条
DELETE a
FROM modefieldauthorize a
JOIN modefieldauthorize b
  ON a.tenant_id <=> b.tenant_id
 AND a.objtype = b.objtype
 AND a.objid = b.objid
 AND a.modeid = b.modeid
 AND a.fieldid = b.fieldid
 AND a.id > b.id;

-- 2) 幂等新增唯一索引
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'blade'
    AND TABLE_NAME = 'modefieldauthorize'
    AND INDEX_NAME = 'uk_field_auth_subject'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE modefieldauthorize ADD UNIQUE KEY uk_field_auth_subject (tenant_id, objtype, objid, modeid, fieldid)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
