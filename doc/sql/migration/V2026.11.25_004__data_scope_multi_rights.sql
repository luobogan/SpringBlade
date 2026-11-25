USE `blade`;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='right_types');
SET @sql = IF(@col=0, 'ALTER TABLE modedatasharerule ADD COLUMN right_types varchar(64) NULL COMMENT "权限类型列表：1查看 2新增 3编辑 4删除 5详情" AFTER type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='scope_subjects');
SET @sql = IF(@col=0, 'ALTER TABLE modedatasharerule ADD COLUMN scope_subjects text NULL COMMENT "数据范围多授权对象JSON数组" AFTER objid', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE modedatasharerule
SET right_types = CASE WHEN type=1 THEN '1,5' WHEN type=2 THEN '1,3,4' ELSE '1,5' END
WHERE right_types IS NULL OR right_types='';
