-- V2026.10.23_002__blade_doc_ext_file.sql
-- 内容引擎 - 附件支持「本地 uploads 直链预览」。
-- doc_image_file 新增：
--   oss_file_id / oss_file_url : 外部文件(OSS)回写元数据（is_ext_file=1 时预览直接返回 oss_file_url）。
--   file_path                : 本地附件相对路径（springBlade/uploads/{docId}/{文件名}），预览经网关白名单静态直链。
-- 幂等：列不存在才添加（兼容已存在场景）。

SET @db = DATABASE();
SET @col1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'doc_image_file' AND COLUMN_NAME = 'oss_file_id');
SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'doc_image_file' AND COLUMN_NAME = 'oss_file_url');
SET @col3 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'doc_image_file' AND COLUMN_NAME = 'file_path');

SET @sql1 = IF(@col1 = 0, 'ALTER TABLE doc_image_file ADD COLUMN oss_file_id VARCHAR(255) NULL COMMENT ''外部文件OSS ID(系统通用附件上传)''', 'SELECT 1');
SET @sql2 = IF(@col2 = 0, 'ALTER TABLE doc_image_file ADD COLUMN oss_file_url VARCHAR(1024) NULL COMMENT ''外部文件OSS访问URL(系统通用附件上传)''', 'SELECT 1');
SET @sql3 = IF(@col3 = 0, 'ALTER TABLE doc_image_file ADD COLUMN file_path VARCHAR(1024) NULL COMMENT ''本地附件相对路径(springBlade/uploads/{docId}/{文件名})''', 'SELECT 1');

PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;
