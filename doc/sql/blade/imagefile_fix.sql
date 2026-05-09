-- ImageFile 表结构修复脚本
-- 如果表已存在但缺少某些列，执行此脚本进行修复

-- 添加缺失的列（如果不存在）
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `downloads` int DEFAULT '0' COMMENT '下载次数';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `miniimgpath` varchar(500) DEFAULT NULL COMMENT '缩略图路径';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `imgsize` varchar(100) DEFAULT NULL COMMENT '缩略图大小';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `isftp` varchar(10) DEFAULT NULL COMMENT '是否FTP存储';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `ftpconfigid` int DEFAULT NULL COMMENT 'FTP配置ID';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `tokenkey` varchar(255) DEFAULT NULL COMMENT 'OSS Token';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `storagestatus` varchar(50) DEFAULT NULL COMMENT '传输状态';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `comefrom` varchar(100) DEFAULT NULL COMMENT '附件来源';
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `secretvalidity` varchar(100) DEFAULT NULL COMMENT '保密期限';
