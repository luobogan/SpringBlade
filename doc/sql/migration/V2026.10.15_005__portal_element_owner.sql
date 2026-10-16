-- =============================================================================
-- 迁移脚本 V2026.10.15_005：门户元素增加归属用户列 owner_user_id
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_005
-- 目标库    : blade
-- 影响表    : blade_portal_element（ADD COLUMN owner_user_id + INDEX）
-- 变更类型  : DDL
-- 是否幂等  : 是（information_schema 守卫，列存在则跳过）
-- 是否丢数据: 否（纯新增可空列，默认 0）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §18.2 / §23
--              泛微 UserAddElementInfo（终端用户"我的门户"新增元素，仅本人可见）
-- 说明      : owner_user_id=0 页面级共享元素（管理员/设计器维护，全员可见页内可见）；
--              owner_user_id>0 终端用户私有元素（/my-portal/element/add 新增，仅本人 /my-portal 可见）。
--              运行时会话过滤：owner_user_id IN (0, 当前用户ID)。
-- 回滚脚本  : ALTER TABLE `blade_portal_element` DROP COLUMN `owner_user_id`;
-- =============================================================================

USE `blade`;

SET @dbname = DATABASE();
SET @tbl = 'blade_portal_element';
SET @col = 'owner_user_id';
SET @s = IF(
  (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = @dbname AND table_name = @tbl AND column_name = @col) = 0,
  CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
         '` bigint DEFAULT ''0'' COMMENT ''归属用户ID（0页面级共享；>0用户私有）'''),
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引（同样幂等守卫）
SET @idx = 'idx_owner_user';
SET @s2 = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @dbname AND table_name = @tbl AND index_name = @idx) = 0,
  CONCAT('ALTER TABLE `', @tbl, '` ADD INDEX `', @idx, '` (`owner_user_id`)'),
  'SELECT 1');
PREPARE stmt2 FROM @s2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `shared_elements` FROM `blade_portal_element` WHERE `owner_user_id` = 0 AND `is_deleted` = 0;
SELECT COUNT(*) AS `user_elements`   FROM `blade_portal_element` WHERE `owner_user_id` > 0 AND `is_deleted` = 0;
