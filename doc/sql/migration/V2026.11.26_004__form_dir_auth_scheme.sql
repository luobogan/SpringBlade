-- 目录权限增加方案分组信息，支持列表、复制、删除。

ALTER TABLE `form_dir_auth`
  ADD COLUMN `scheme_id` bigint NOT NULL DEFAULT 0 COMMENT '方案ID',
  ADD COLUMN `scheme_name` varchar(200) DEFAULT NULL COMMENT '方案名称',
  ADD COLUMN `priority` int NOT NULL DEFAULT 100 COMMENT '优先级';

UPDATE `form_dir_auth`
SET `scheme_id` = `id`,
    `scheme_name` = CONCAT('目录权限方案-', `id`)
WHERE `scheme_id` = 0;

CREATE INDEX `idx_form_dir_auth_scheme` ON `form_dir_auth` (`tenant_id`, `dir_id`, `scheme_id`);
