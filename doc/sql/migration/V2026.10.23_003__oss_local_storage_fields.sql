-- =============================================================================
-- 迁移脚本：blade_oss 本地存储扩展字段
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.23_003
-- 目标库    : blade
-- 影响表    : blade_oss（新增 6 个本地存储相关字段）
-- 变更类型  : DDL
-- 是否幂等  : 是（使用 ADD COLUMN IF NOT EXISTS，需 MySQL 8.0.29+；
--            低版本请改用文末“兼容幂等脚本”）
-- 是否丢数据: 否（全部为新增可空列）
-- 依赖      : 需先执行 V2026.10.23_002__oss_config.sql（blade_oss 表已存在）
-- 回滚脚本  :
--   ALTER TABLE `blade_oss`
--     DROP COLUMN `image_dir`,
--     DROP COLUMN `file_dir`,
--     DROP COLUMN `backup_dir`,
--     DROP COLUMN `backup_cycle`,
--     DROP COLUMN `compress_enable`,
--     DROP COLUMN `encrypt_enable`;
--
-- 背景
--   当 blade_oss.category = 'local'（本地存储）/ 'rustfs' 时，需要本地磁盘目录、
--   备份目录、备份周期以及压缩/加密开关等配置项。这些字段仅本地类存储生效，
--   其余分类（qiniu/alioss/minio 等）留空即可。
--
-- Linux 路径注意事项（重要）
--   1. 路径请使用正斜杠 “/”，不要使用 Windows 反斜杠 “\” 或盘符（如 C:\）。
--   2. Linux 路径区分大小写，/data/oss 与 /Data/oss 是不同的目录。
--   3. 必须是运行用户（如启动 blade-resource 的系统用户）拥有读写权限的目录，
--      建议目录权限 0755、文件 0644；若启用 SELinux/AppArmor 需放行该路径。
--   4. 备份目录应与主目录（image_dir/file_dir）分离，避免备份覆盖原数据。
--   5. 配置路径时建议不带结尾斜杠，由程序在拼接文件名时统一追加 “/”。
--   6. 路径所在挂载点需提前存在（如 /data），程序一般不自动创建多级父目录。
-- =============================================================================

USE `blade`;

ALTER TABLE `blade_oss`
  ADD COLUMN IF NOT EXISTS `image_dir`       VARCHAR(512) NULL DEFAULT NULL COMMENT '图片存放目录（本地存储生效，Linux 绝对路径如 /data/blade/oss/images，正斜杠、区分大小写）',
  ADD COLUMN IF NOT EXISTS `file_dir`        VARCHAR(512) NULL DEFAULT NULL COMMENT '文件存放目录（本地存储生效，Linux 绝对路径如 /data/blade/oss/files）',
  ADD COLUMN IF NOT EXISTS `backup_dir`      VARCHAR(512) NULL DEFAULT NULL COMMENT '文件备份目录（本地存储生效，应与主目录分离，如 /data/blade/oss/backup）',
  ADD COLUMN IF NOT EXISTS `backup_cycle`    INT          NULL DEFAULT 1440   COMMENT '文件备份周期（分钟），默认 1440 = 24 小时',
  ADD COLUMN IF NOT EXISTS `compress_enable` TINYINT(1)   NULL DEFAULT 0      COMMENT '压缩存储开关（0关 1开）',
  ADD COLUMN IF NOT EXISTS `encrypt_enable`  TINYINT(1)   NULL DEFAULT 0      COMMENT '加密存储开关（0关 1开）';

-- =============================================================================
-- 兼容幂等脚本（适用于 MySQL < 8.0.29，不支持 ADD COLUMN IF NOT EXISTS）
-- 可按需整段执行；已存在列时自动跳过，不会报错也不会丢数据。
-- =============================================================================
--
-- SET @db = 'blade';
-- SET @tbl = 'blade_oss';
--
-- SET @c1 = 0; SELECT COUNT(*) INTO @c1 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='image_dir';
-- SET @s1 = IF(@c1=0, "ALTER TABLE `blade_oss` ADD COLUMN `image_dir` VARCHAR(512) NULL DEFAULT NULL COMMENT '图片存放目录'", "SELECT 1");
-- PREPARE stmt FROM @s1; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @c2 = 0; SELECT COUNT(*) INTO @c2 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='file_dir';
-- SET @s2 = IF(@c2=0, "ALTER TABLE `blade_oss` ADD COLUMN `file_dir` VARCHAR(512) NULL DEFAULT NULL COMMENT '文件存放目录'", "SELECT 1");
-- PREPARE stmt FROM @s2; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @c3 = 0; SELECT COUNT(*) INTO @c3 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='backup_dir';
-- SET @s3 = IF(@c3=0, "ALTER TABLE `blade_oss` ADD COLUMN `backup_dir` VARCHAR(512) NULL DEFAULT NULL COMMENT '文件备份目录'", "SELECT 1");
-- PREPARE stmt FROM @s3; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @c4 = 0; SELECT COUNT(*) INTO @c4 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='backup_cycle';
-- SET @s4 = IF(@c4=0, "ALTER TABLE `blade_oss` ADD COLUMN `backup_cycle` INT NULL DEFAULT 1440 COMMENT '文件备份周期(分钟)'", "SELECT 1");
-- PREPARE stmt FROM @s4; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @c5 = 0; SELECT COUNT(*) INTO @c5 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='compress_enable';
-- SET @s5 = IF(@c5=0, "ALTER TABLE `blade_oss` ADD COLUMN `compress_enable` TINYINT(1) NULL DEFAULT 0 COMMENT '压缩存储开关(0关1开)'", "SELECT 1");
-- PREPARE stmt FROM @s5; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @c6 = 0; SELECT COUNT(*) INTO @c6 FROM information_schema.columns WHERE table_schema=@db AND table_name=@tbl AND column_name='encrypt_enable';
-- SET @s6 = IF(@c6=0, "ALTER TABLE `blade_oss` ADD COLUMN `encrypt_enable` TINYINT(1) NULL DEFAULT 0 COMMENT '加密存储开关(0关1开)'", "SELECT 1");
-- PREPARE stmt FROM @s6; EXECUTE stmt; DEALLOCATE PREPARE stmt;
