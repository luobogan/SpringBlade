-- 模块目录主键改为数据库自增，避免手工生成时间戳ID重复。

ALTER TABLE `modetreefield`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID';
