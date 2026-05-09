-- ImageFile 表创建脚本
-- 参考 ecology 项目的 ImageFile 表结构

CREATE TABLE IF NOT EXISTS `ImageFile` (
  `imagefileid` bigint NOT NULL COMMENT '附件ID',
  `imagefilename` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `imagefiletype` varchar(255) DEFAULT NULL COMMENT '文件类型（MIME类型）',
  `imagefileused` int DEFAULT '1' COMMENT '是否使用（1=使用）',
  `filerealpath` varchar(500) DEFAULT NULL COMMENT '文件真实存储路径',
  `iszip` int DEFAULT '0' COMMENT '是否ZIP压缩（0/1）',
  `isencrypt` int DEFAULT '0' COMMENT '是否加密（0/1）- 已基本不使用',
  `filesize` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `downloads` int DEFAULT '0' COMMENT '下载次数',
  `miniimgpath` varchar(500) DEFAULT NULL COMMENT '缩略图路径',
  `imgsize` varchar(100) DEFAULT NULL COMMENT '缩略图大小',
  `isftp` varchar(10) DEFAULT NULL COMMENT '是否FTP存储',
  `ftpconfigid` int DEFAULT NULL COMMENT 'FTP配置ID',
  `isaesencrypt` int DEFAULT '0' COMMENT '是否AES加密（0/1）',
  `aescode` varchar(255) DEFAULT NULL COMMENT 'AES加密密钥',
  `tokenkey` varchar(255) DEFAULT NULL COMMENT 'OSS Token',
  `storagestatus` varchar(50) DEFAULT NULL COMMENT '传输状态',
  `comefrom` varchar(100) DEFAULT NULL COMMENT '附件来源',
  `secretlevel` int DEFAULT '4' COMMENT '密级',
  `secretvalidity` varchar(100) DEFAULT NULL COMMENT '保密期限',
  `createdat` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedat` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`imagefileid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件文件表';


ALTER TABLE ImageFile ADD COLUMN `downloads` int DEFAULT 0 COMMENT '下载次数';
ALTER TABLE ImageFile ADD COLUMN `miniimgpath` varchar(500) DEFAULT NULL COMMENT '缩略图路径';
ALTER TABLE ImageFile ADD COLUMN `imgsize` varchar(100) DEFAULT NULL COMMENT '缩略图大小';
ALTER TABLE ImageFile ADD COLUMN `isftp` varchar(10) DEFAULT NULL COMMENT '是否FTP存储';
ALTER TABLE ImageFile ADD COLUMN `ftpconfigid` int DEFAULT NULL COMMENT 'FTP配置ID';
ALTER TABLE ImageFile ADD COLUMN `tokenkey` varchar(255) DEFAULT NULL COMMENT 'OSS Token';
ALTER TABLE ImageFile ADD COLUMN `storagestatus` varchar(50) DEFAULT NULL COMMENT '传输状态';
ALTER TABLE ImageFile ADD COLUMN `comefrom` varchar(100) DEFAULT NULL COMMENT '附件来源';
ALTER TABLE ImageFile ADD COLUMN `secretvalidity` varchar(100) DEFAULT NULL COMMENT '保密期限';
ALTER TABLE ImageFile ADD COLUMN `createdat` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE ImageFile ADD COLUMN `updatedat` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
