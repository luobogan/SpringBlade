-- V2026.10.22_007__blade_doc_tenant_backfill.sql
-- 回填内容引擎 12 表的 tenant_id（空串/NULL → 000000），配合 blade.tenant.tables 启用租户隔离。
-- 必须在启用租户拦截器「之前」执行（否则拦截器会给 UPDATE 追加 tenant_id 条件导致 0 行）。
-- 库无关；作用于当前 DATABASE() 的 blade 库。
UPDATE `doc_detail` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_sec_category` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_image_file` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_detail_content` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_share` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_dummy_detail` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_detail_log` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `download_log` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_recycle` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_custom_field` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_custom_field_value` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
UPDATE `doc_mould` SET tenant_id = '000000' WHERE tenant_id = '' OR tenant_id IS NULL;
