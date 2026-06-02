-- ============================================================================
-- SpringBlade 表单建模 - 扩展字段属性（对标泛微E9）
-- ============================================================================

USE `blade`;

-- ============================================================================
-- 1. 扩展 workflow_billfield 表字段（对标泛微E9 workflow_billfield 表）
-- ============================================================================

-- 检查字段是否已存在，避免重复添加
-- textheight: 文本高度（多行文本）
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `textheight` int(11) DEFAULT ''4'' COMMENT ''文本高度（多行文本）'''
        ELSE 'SELECT ''Column textheight already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'textheight'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ismand: 是否必填（泛微标准字段名）
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `ismand` int(11) DEFAULT ''0'' COMMENT ''是否必填（0否 1是）'''
        ELSE 'SELECT ''Column ismand already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'ismand'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- fieldorder: 显示顺序（泛微标准字段名）
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `fieldorder` int(11) DEFAULT ''0'' COMMENT ''显示顺序'''
        ELSE 'SELECT ''Column fieldorder already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'fieldorder'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- isused: 是否启用
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `isused` int(11) DEFAULT ''1'' COMMENT ''是否启用（0否 1是）'''
        ELSE 'SELECT ''Column isused already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'isused'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- description: 字段描述
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `description` varchar(1000) DEFAULT NULL COMMENT ''字段描述'''
        ELSE 'SELECT ''Column description already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'description'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- quicktype: 快速录入类型
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `quicktype` int(11) DEFAULT ''0'' COMMENT ''快速录入类型'''
        ELSE 'SELECT ''Column quicktype already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'quicktype'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- impcheck: 导入验证类型
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `impcheck` int(11) DEFAULT ''0'' COMMENT ''导入验证类型（0不验证 1电话 2手机 3邮编 4身份证 5日期 6时间 7email 8自定义）'''
        ELSE 'SELECT ''Column impcheck already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'impcheck'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- checkexpression: 验证表达式
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `checkexpression` varchar(3000) DEFAULT NULL COMMENT ''验证表达式（正则表达式）'''
        ELSE 'SELECT ''Column checkexpression already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'checkexpression'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- placeholder: 提示信息
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `placeholder` varchar(1000) DEFAULT NULL COMMENT ''提示信息（输入框占位符）'''
        ELSE 'SELECT ''Column placeholder already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'placeholder'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- needlog: 记录日志
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `needlog` int(11) DEFAULT ''0'' COMMENT ''是否记录字段修改日志（0否 1是）'''
        ELSE 'SELECT ''Column needlog already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'needlog'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- needexcel: Excel导入
SET @sql = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'ALTER TABLE `workflow_billfield` ADD COLUMN `neeedcel` int(11) DEFAULT ''0'' COMMENT ''是否允许Excel导入（0否 1是）'''
        ELSE 'SELECT ''Column needexcel already exists'' LIMIT 0'
    END
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'workflow_billfield' AND column_name = 'neeedcel'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 2. 创建字段扩展属性表（对标泛微E9 ModeFormFieldExtend 表）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `mode_form_field_extend` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `fieldid` bigint(20) NOT NULL COMMENT '字段ID',
    `formid` bigint(20) NOT NULL COMMENT '表单ID',
    `expendattr` text DEFAULT NULL COMMENT '扩展属性（JSON格式）',
    `createdate` char(10) DEFAULT NULL COMMENT '创建日期',
    `createtime` char(8) DEFAULT NULL COMMENT '创建时间',
    `modifydate` char(10) DEFAULT NULL COMMENT '修改日期',
    `modifytime` char(8) DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_fieldid` (`fieldid`),
    KEY `idx_formid` (`formid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段扩展属性表（对标泛微E9）';

-- ============================================================================
-- 3. 创建字段选项配置表（对标泛微E9 selectItem 表）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `mode_form_field_option` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `fieldid` bigint(20) NOT NULL COMMENT '字段ID',
    `formid` bigint(20) NOT NULL COMMENT '表单ID',
    `optionvalue` varchar(1000) DEFAULT NULL COMMENT '选项值',
    `optionlabel` varchar(1000) DEFAULT NULL COMMENT '选项标签',
    `listorder` int(11) DEFAULT '0' COMMENT '显示顺序',
    `isdefault` int(11) DEFAULT '0' COMMENT '是否默认选中（0否 1是）',
    PRIMARY KEY (`id`),
    KEY `idx_fieldid` (`fieldid`),
    KEY `idx_formid` (`formid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段选项配置表（选择框/单选/复选）';

-- ============================================================================
-- 说明
-- ============================================================================
-- 1. workflow_billfield 表扩展了以下字段：
--    - textheight: 文本高度（多行文本使用）
--    - ismand: 是否必填（替代原有的 isnull 字段，与泛微E9保持一致）
--    - fieldorder: 显示顺序（替代原有的 dsorder 字段，与泛微E9保持一致）
--    - isused: 是否启用
--    - description: 字段描述
--    - quicktype: 快速录入类型
--    - impcheck: 导入验证类型
--    - checkexpression: 验证表达式（正则表达式）
--    - placeholder: 提示信息（输入框占位符）
--    - needlog: 是否记录字段修改日志
--    - needexcel: 是否允许Excel导入
--
-- 2. mode_form_field_extend 表用于存储字段的扩展属性：
--    - expendattr 字段存储 JSON 格式的扩展配置
--    - 例如：浏览按钮的 sqlwhere、sqlcondition 配置
--    - 例如：SQL计算字段的配置
--
-- 3. mode_form_field_option 表用于存储选择框/单选框/复选框的选项数据：
--    - optionvalue: 选项值
--    - optionlabel: 选项标签
--    - listorder: 显示顺序
--    - isdefault: 是否默认选中
--
-- 4. 执行此脚本后，需要同步更新后端实体类 FieldDefinition.java
-- ============================================================================
