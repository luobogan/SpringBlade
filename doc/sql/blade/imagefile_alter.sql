-- ImageFile 表结构修复脚本 - 添加所有缺失的列
-- 如果表已存在但缺少某些列，执行此脚本进行修复

-- 检查并添加 downloads 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `downloads` int DEFAULT 0 COMMENT '下载次数';

-- 检查并添加 miniimgpath 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `miniimgpath` varchar(500) DEFAULT NULL COMMENT '缩略图路径';

-- 检查并添加 imgsize 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `imgsize` varchar(100) DEFAULT NULL COMMENT '缩略图大小';

-- 检查并添加 isftp 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `isftp` varchar(10) DEFAULT NULL COMMENT '是否FTP存储';

-- 检查并添加 ftpconfigid 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `ftpconfigid` int DEFAULT NULL COMMENT 'FTP配置ID';

-- 检查并添加 tokenkey 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `tokenkey` varchar(255) DEFAULT NULL COMMENT 'OSS Token';

-- 检查并添加 storagestatus 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `storagestatus` varchar(50) DEFAULT NULL COMMENT '传输状态';

-- 检查并添加 comefrom 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `comefrom` varchar(100) DEFAULT NULL COMMENT '附件来源';

-- 检查并添加 secretvalidity 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `secretvalidity` varchar(100) DEFAULT NULL COMMENT '保密期限';

-- 检查并添加 createdat 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `createdat` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 检查并添加 updatedat 列
ALTER TABLE ImageFile ADD COLUMN IF NOT EXISTS `updatedat` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
