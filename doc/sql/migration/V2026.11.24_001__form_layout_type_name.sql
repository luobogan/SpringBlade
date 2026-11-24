-- =============================================================================
-- 迁移脚本 V2026.11.24_001：表单布局类型显示名
-- -----------------------------------------------------------------------------
-- 目标库：blade
-- 说明：
--   支持按表单自定义 0/1/3/4 四类布局类型的显示名；
--   layoutType 编码不变，运行期映射不变。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `form_layout_type_name` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(12) DEFAULT '000000',
  `form_id` bigint NOT NULL,
  `layout_type` int NOT NULL,
  `layout_name` varchar(100) NOT NULL DEFAULT '',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_layout_type_name` (`tenant_id`, `form_id`, `layout_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单布局类型显示名';
