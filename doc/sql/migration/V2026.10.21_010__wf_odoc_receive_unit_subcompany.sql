-- -----------------------------------------------------------------------------
-- 迁移脚本：收发文单位 增加「归属机构」字段（对齐 E9 DOCRECEIVEUNIT.subcompanyid）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_010
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_receive_unit
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : ALTER TABLE wf_odoc_receive_unit DROP COLUMN subcompany_id;
--
-- 背景
--   原收文单位仅有 superiorId 层级，没有「归属机构/组织」维度。E9 的 DOCRECEIVEUNIT
--   以 subcompanyid 把单位挂到 HR 机构（分部）下，维护页左侧机构树、右侧其下单位。
--   此处补 subcompany_id，由前端从 blade_system 机构树选择，实现「组织 → 层级」两级归并。

SET @t = 'wf_odoc_receive_unit';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'subcompany_id');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN subcompany_id BIGINT NULL DEFAULT NULL COMMENT ''归属机构ID(对应blade_system部门节点,deptType=1分部)'' AFTER unit_level'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;
