-- =============================================================================
-- V2026.11.25_005：矩阵主键改为 bigint 自增
-- =============================================================================
-- 1. 历史雪花矩阵 ID 重编号为当前最大小整数之后的连续 ID。
-- 2. 同步矩阵子表、授权方案、字段权限、模块权限、数据范围、菜单共享、
--    流程操作者以及 BPMN XML 中的矩阵 ID。
-- 3. 保留映射表 wf_matrix_id_remap_20261125 供审核。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `wf_matrix_id_remap_20261125` (
  `old_id` BIGINT NOT NULL,
  `new_id` BIGINT NOT NULL,
  `remap_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_wf_matrix_id_remap_new` (`new_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='矩阵雪花ID改自增值映射';

CREATE TABLE IF NOT EXISTS `wf_matrix_snapshot_before_autoinc_20261125` AS SELECT * FROM `wf_matrix`;

DELETE FROM `wf_matrix_id_remap_20261125`;

SELECT COALESCE(MAX(`id`), 0) INTO @base_id
FROM `wf_matrix`
WHERE `id` < 1000000000000000000;

INSERT INTO `wf_matrix_id_remap_20261125` (`old_id`, `new_id`)
SELECT `id`, @base_id + ROW_NUMBER() OVER (ORDER BY `id`)
FROM `wf_matrix`
WHERE `id` >= 1000000000000000000;

-- 子表及权限表引用
UPDATE `wf_matrix_field` t JOIN `wf_matrix_id_remap_20261125` m ON t.`matrix_id` = m.`old_id`
SET t.`matrix_id` = m.`new_id`;

UPDATE `wf_matrix_data` t JOIN `wf_matrix_id_remap_20261125` m ON t.`matrix_id` = m.`old_id`
SET t.`matrix_id` = m.`new_id`;

UPDATE `mode_auth_scheme` t JOIN `wf_matrix_id_remap_20261125` m ON t.`subject_id` = m.`old_id`
SET t.`subject_id` = m.`new_id`
WHERE t.`subject_type` = 6;

UPDATE `mode_field_auth_subject` t JOIN `wf_matrix_id_remap_20261125` m ON t.`objid` = m.`old_id`
SET t.`objid` = m.`new_id`
WHERE t.`objtype` IN (6, 7);

UPDATE `modefieldauthorize` t JOIN `wf_matrix_id_remap_20261125` m ON t.`objid` = m.`old_id`
SET t.`objid` = m.`new_id`
WHERE t.`objtype` IN (6, 7);

UPDATE `moderightinfo` t JOIN `wf_matrix_id_remap_20261125` m ON t.`objid` = m.`old_id`
SET t.`objid` = m.`new_id`
WHERE t.`objtype` IN (6, 7);

UPDATE `modedatasharerule` t JOIN `wf_matrix_id_remap_20261125` m ON t.`objid` = m.`old_id`
SET t.`objid` = m.`new_id`
WHERE t.`objtype` IN (6, 7);

UPDATE `blade_menu_share` t JOIN `wf_matrix_id_remap_20261125` m ON t.`share_id` = m.`old_id`
SET t.`share_id` = m.`new_id`
WHERE t.`share_type` = 8;

UPDATE `wf_node_operator` t JOIN `wf_matrix_id_remap_20261125` m ON CAST(t.`obj_id` AS UNSIGNED) = m.`old_id`
SET t.`obj_id` = CAST(m.`new_id` AS CHAR)
WHERE t.`op_type` = 99;

-- BPMN XML 中的矩阵 ID
DROP PROCEDURE IF EXISTS `p_remap_matrix_id_in_bpmn`;
DELIMITER $$
CREATE PROCEDURE `p_remap_matrix_id_in_bpmn`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE old_id BIGINT;
  DECLARE new_id BIGINT;
  DECLARE cur CURSOR FOR SELECT `old_id`, `new_id` FROM `wf_matrix_id_remap_20261125` ORDER BY `old_id`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO old_id, new_id;
    IF done = 1 THEN LEAVE read_loop; END IF;
    SET @old_text = CAST(old_id AS CHAR);
    SET @new_text = CAST(new_id AS CHAR);
    SET @pattern = CONCAT('%', @old_text, '%');
    SET @sql = 'UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, ?, ?) WHERE `bpmn_xml` LIKE ?';
    PREPARE stmt FROM @sql;
    EXECUTE stmt USING @old_text, @new_text, @pattern;
    DEALLOCATE PREPARE stmt;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL `p_remap_matrix_id_in_bpmn`();
DROP PROCEDURE IF EXISTS `p_remap_matrix_id_in_bpmn`;

-- 兼容部分 MySQL 客户端在批量执行时不处理 DELIMITER 的情况；以下替换幂等。
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110186695577083905', '14') WHERE `bpmn_xml` LIKE '%2110186695577083905%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110187991973535746', '15') WHERE `bpmn_xml` LIKE '%2110187991973535746%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110188562159804418', '16') WHERE `bpmn_xml` LIKE '%2110188562159804418%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110189299723952130', '17') WHERE `bpmn_xml` LIKE '%2110189299723952130%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110221413181849602', '18') WHERE `bpmn_xml` LIKE '%2110221413181849602%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110223059932708866', '19') WHERE `bpmn_xml` LIKE '%2110223059932708866%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110291933877309441', '20') WHERE `bpmn_xml` LIKE '%2110291933877309441%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110312913190318081', '21') WHERE `bpmn_xml` LIKE '%2110312913190318081%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110328584045375489', '22') WHERE `bpmn_xml` LIKE '%2110328584045375489%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110328848840175617', '23') WHERE `bpmn_xml` LIKE '%2110328848840175617%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110339244003209217', '24') WHERE `bpmn_xml` LIKE '%2110339244003209217%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110533836635955202', '25') WHERE `bpmn_xml` LIKE '%2110533836635955202%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110536116844158978', '26') WHERE `bpmn_xml` LIKE '%2110536116844158978%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110536272129875969', '27') WHERE `bpmn_xml` LIKE '%2110536272129875969%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110536518448766978', '28') WHERE `bpmn_xml` LIKE '%2110536518448766978%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110536871625940993', '29') WHERE `bpmn_xml` LIKE '%2110536871625940993%';
UPDATE `wf_process_definition` SET `bpmn_xml` = REPLACE(`bpmn_xml`, '2110656791881768962', '30') WHERE `bpmn_xml` LIKE '%2110656791881768962%';

-- 最后改主表 ID，并设置为自增
UPDATE `wf_matrix` t JOIN `wf_matrix_id_remap_20261125` m ON t.`id` = m.`old_id`
SET t.`id` = m.`new_id`;

SET @auto_inc = (SELECT COALESCE(MAX(`id`), 0) + 1 FROM `wf_matrix`);
SET @sql = CONCAT('ALTER TABLE `wf_matrix` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT ''主键（自增）'', AUTO_INCREMENT=', @auto_inc);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
