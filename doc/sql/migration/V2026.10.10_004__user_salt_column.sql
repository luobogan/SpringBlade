-- 补齐 blade_user 仅剩的缺失列 salt（密码盐值；实体 User.salt 无 @TableField，默认映射 salt 列）
USE `blade`;
ALTER TABLE `blade_user` ADD COLUMN `salt` varchar(255) DEFAULT NULL COMMENT '密码盐值(预留，配合加盐算法惰性升级)';
