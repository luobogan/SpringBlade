-- ============================================================================
-- SpringBlade 表单建模微服务 - 数据库初始化脚本
-- 基于 ecology 表单建模功能的数据表结构迁移
-- 数据库: MySQL 8.0+
-- 目标库: blade（所有表统一放到 blade 库）
-- ============================================================================

-- 数据库使用 blade（需提前创建或由其他模块初始化）
-- CREATE DATABASE IF NOT EXISTS `blade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `blade`;

-- ============================================================================
-- 批次1：核心配置表
-- ============================================================================

-- 1.1 模块信息表（modeinfo）
CREATE TABLE IF NOT EXISTS `modeinfo` (
    `id`            BIGINT       NOT NULL COMMENT '模块ID',
    `modename`      VARCHAR(200) DEFAULT NULL COMMENT '模块名称',
    `modedescription` VARCHAR(500) DEFAULT NULL COMMENT '模块描述',
    `billid`        INT          DEFAULT NULL COMMENT '关联表单ID',
    `modetype`      INT          DEFAULT 0 COMMENT '模块类型',
    `modeTreeField` INT          DEFAULT NULL COMMENT '应用分类ID',
    `status`        INT          DEFAULT 1 COMMENT '状态:1启用 0禁用',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `modeicon`      VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
    `detailurl`     VARCHAR(500) DEFAULT NULL COMMENT '详情URL',
    `listurl`       VARCHAR(500) DEFAULT NULL COMMENT '列表URL',
    `creater`       INT          DEFAULT NULL COMMENT '创建人',
    `createdate`    VARCHAR(10)  DEFAULT NULL COMMENT '创建日期',
    `createtime`    VARCHAR(8)   DEFAULT NULL COMMENT '创建时间',
    `modifier`      INT          DEFAULT NULL COMMENT '修改人',
    `modifydate`    VARCHAR(10)  DEFAULT NULL COMMENT '修改日期',
    `modifytime`    VARCHAR(8)   DEFAULT NULL COMMENT '修改时间',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    `is_deleted`    INT          DEFAULT 0 COMMENT '逻辑删除:1已删 0未删',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`),
    KEY `idx_billid` (`billid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块信息';

-- 1.2 表单定义表（workflow_bill）
CREATE TABLE IF NOT EXISTS `workflow_bill` (
    `id`            INT          NOT NULL COMMENT '表单ID（负数）',
    `billname`      VARCHAR(200) DEFAULT NULL COMMENT '表单名称',
    `tablename`     VARCHAR(200) DEFAULT NULL COMMENT '物理表名(formtable_main_xxx)',
    `billtype`      INT          DEFAULT 0 COMMENT '表单类型:0普通 1明细 2组合',
    `detailtablecount` INT       DEFAULT 0 COMMENT '明细表数量',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `creater`       INT          DEFAULT NULL COMMENT '创建人',
    `createdate`    VARCHAR(10)  DEFAULT NULL COMMENT '创建日期',
    `createtime`    VARCHAR(8)   DEFAULT NULL COMMENT '创建时间',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    `is_deleted`    INT          DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单定义';

USE blade;
ALTER TABLE workflow_billfield MODIFY COLUMN billid BIGINT DEFAULT NULL;


-- 1.3 表单字段定义表（workflow_billfield）
CREATE TABLE IF NOT EXISTS `workflow_billfield` (
    `id`            INT          NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `billid`        BIGINT       DEFAULT NULL COMMENT '所属表单ID',
    `fieldname`     VARCHAR(200) DEFAULT NULL COMMENT '字段名',
    `fielddbname`   VARCHAR(60)  DEFAULT NULL COMMENT '数据库列名',
    `fieldhtmltype` INT          DEFAULT 1 COMMENT 'HTML表单元素类型(1文本 2多行 3数字 4日期 5下拉 7复选 8单选 10浏览 11附件 14HTML编辑)',
    `fieldtype`     INT          DEFAULT 0 COMMENT '字段类型',
    `fieldlen`      INT          DEFAULT 100 COMMENT '字段长度',
    `decimaldigit`  INT          DEFAULT 0 COMMENT '小数点位数',
    `defaultvalue`  VARCHAR(500) DEFAULT NULL COMMENT '默认值',
    `dsporder`      INT          DEFAULT 0 COMMENT '显示顺序',
    `isnull`        INT          DEFAULT 0 COMMENT '是否必填:1是 0否',
    `uniquevalue`   INT          DEFAULT 0 COMMENT '是否唯一:1是 0否',
    `fieldmsg`      VARCHAR(500) DEFAULT NULL COMMENT '字段提示',
    `fielddbtype`   INT          DEFAULT 1 COMMENT '数据库字段类型',
    `browtype`      INT          DEFAULT NULL COMMENT '浏览框类型',
    `browserurlid`  INT          DEFAULT NULL COMMENT '浏览框URL ID',
    `selectitem`    TEXT         DEFAULT NULL COMMENT '选项数据(JSON)',
    `detailtable`   INT          DEFAULT NULL COMMENT '关联明细表ID',
    `ismain`        INT          DEFAULT 1 COMMENT '是否主表字段:1是 0否',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_billid` (`billid`),
    KEY `idx_tenant` (`tenant_id`),
    CONSTRAINT `fk_field_bill` FOREIGN KEY (`billid`) REFERENCES `workflow_bill`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单字段定义';

-- 1.4 表单基本信息表（workflow_formbase）
CREATE TABLE IF NOT EXISTS `workflow_formbase` (
    `id`            INT          NOT NULL COMMENT '表单ID',
    `formname`      VARCHAR(200) DEFAULT NULL COMMENT '表单名称',
    `formtype`      INT          DEFAULT 0 COMMENT '表单类型',
    `formhtml`      LONGTEXT     DEFAULT NULL COMMENT '表单HTML',
    `formwidth`     INT          DEFAULT 800 COMMENT '表单宽度',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单基本信息';

-- 1.5 浏览框URL定义表（workflow_browserurl）
CREATE TABLE IF NOT EXISTS `workflow_browserurl` (
    `id`            INT          NOT NULL COMMENT 'URL ID',
    `name`          VARCHAR(200) DEFAULT NULL COMMENT '名称',
    `url`           TEXT         DEFAULT NULL COMMENT 'URL地址',
    `type`          INT          DEFAULT 0 COMMENT '类型',
    `param`         TEXT         DEFAULT NULL COMMENT '参数',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览框URL定义';

-- 1.6 字段类型表（mode_fieldtype）
CREATE TABLE IF NOT EXISTS `mode_fieldtype` (
    `id`            INT          NOT NULL AUTO_INCREMENT COMMENT '类型ID',
    `typename`      VARCHAR(100) DEFAULT NULL COMMENT '类型名称',
    `htmltype`      INT          DEFAULT NULL COMMENT 'HTML字段类型',
    `dbtype`        INT          DEFAULT NULL COMMENT '数据库类型',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段类型';

-- 1.7 应用分类表（modeTreeField）
CREATE TABLE IF NOT EXISTS `modeTreeField` (
    `id`            BIGINT       NOT NULL COMMENT '分类ID',
    `name`          VARCHAR(200) DEFAULT NULL COMMENT '分类名称',
    `parentid`      BIGINT       DEFAULT 0 COMMENT '父分类ID',
    `level`         INT          DEFAULT 1 COMMENT '层级',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `icon`          VARCHAR(200) DEFAULT NULL COMMENT '图标',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parentid`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用分类';

-- ============================================================================
-- 批次2：布局/扩展表
-- ============================================================================

-- 2.1 布局组表（modeformgroup）
CREATE TABLE IF NOT EXISTS `modeformgroup` (
    `id`            BIGINT       NOT NULL COMMENT '组ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `groupname`     VARCHAR(200) DEFAULT NULL COMMENT '组名称',
    `grouptype`     INT          DEFAULT 1 COMMENT '组类型:1主表 2明细表',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `showcondition` TEXT         DEFAULT NULL COMMENT '显示条件',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='布局组';

-- 2.2 布局字段表（modeformfield）
CREATE TABLE IF NOT EXISTS `modeformfield` (
    `id`            BIGINT       NOT NULL COMMENT '字段ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `groupid`       BIGINT       DEFAULT NULL COMMENT '所属组ID',
    `fieldid`       INT          DEFAULT NULL COMMENT 'workflow_billfield字段ID',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `isshow`        INT          DEFAULT 1 COMMENT '是否显示:1显示 0隐藏',
    `isreadonly`    INT          DEFAULT 0 COMMENT '是否只读:1只读 0可编辑',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_groupid` (`groupid`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='布局字段';

-- 2.3 模块表单扩展表（mode_formextend）
CREATE TABLE IF NOT EXISTS `mode_formextend` (
    `id`            BIGINT       NOT NULL COMMENT '扩展ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `extendjson`    LONGTEXT     DEFAULT NULL COMMENT '扩展配置JSON',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块表单扩展';

-- 2.4 页面扩展配置表（mode_pageexpand）
CREATE TABLE IF NOT EXISTS `mode_pageexpand` (
    `id`            BIGINT       NOT NULL COMMENT '扩展ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `expandtype`    INT          DEFAULT 0 COMMENT '扩展类型:0保存前 1保存后 2删除前 3删除后',
    `javaclass`     VARCHAR(500) DEFAULT NULL COMMENT 'Java扩展类名',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面扩展配置';

-- ============================================================================
-- 批次3：权限表
-- ============================================================================

-- 3.1 模块权限表（moderightinfo）
CREATE TABLE IF NOT EXISTS `moderightinfo` (
    `id`            BIGINT       NOT NULL COMMENT '权限ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `righttype`     INT          DEFAULT NULL COMMENT '权限类型:1可见 2新建 3编辑 4删除 5详情 6导入 7导出',
    `objtype`       INT          DEFAULT NULL COMMENT '授权对象类型:1角色 2部门 3岗位 4人员',
    `objid`         INT          DEFAULT NULL COMMENT '授权对象ID',
    `objname`       VARCHAR(200) DEFAULT NULL COMMENT '授权对象名称',
    `includesub`    INT          DEFAULT 0 COMMENT '包含子部门:1是 0否',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_righttype` (`righttype`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块权限';

-- 3.2 字段授权表（modefieldauthorize）
CREATE TABLE IF NOT EXISTS `modefieldauthorize` (
    `id`            BIGINT       NOT NULL COMMENT '授权ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `fieldid`       INT          DEFAULT NULL COMMENT '字段ID',
    `objtype`       INT          DEFAULT NULL COMMENT '授权对象类型:1角色 2部门 3岗位 4人员',
    `objid`         INT          DEFAULT NULL COMMENT '授权对象ID',
    `rightvalue`    INT          DEFAULT NULL COMMENT '权限:1可见 2可编辑',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段授权';

-- ============================================================================
-- 批次4：浏览框表
-- ============================================================================

-- 4.1 浏览框配置表（mode_browser）
CREATE TABLE IF NOT EXISTS `mode_browser` (
    `id`            BIGINT       NOT NULL COMMENT '浏览框ID',
    `showname`      VARCHAR(1000) DEFAULT NULL COMMENT '显示名称',
    `showclass`     CHAR(1)      DEFAULT NULL COMMENT '显示方式:0文本框 1树 2左右 3弹出 4下拉',
    `datafrom`      CHAR(1)      DEFAULT NULL COMMENT '数据来源:0SQL 1WS 2自定义',
    `datasourceid`  VARCHAR(1000) DEFAULT NULL COMMENT '数据源ID',
    `sqltext`       LONGTEXT     DEFAULT NULL COMMENT 'SQL查询',
    `wsurl`         VARCHAR(1000) DEFAULT NULL COMMENT 'WS URL',
    `wsoperation`   VARCHAR(1000) DEFAULT NULL COMMENT 'WS操作名',
    `xmltext`       LONGTEXT     DEFAULT NULL COMMENT 'XML配置',
    `inpara`        VARCHAR(1000) DEFAULT NULL COMMENT '传入参数',
    `showtype`      CHAR(1)      DEFAULT '0' COMMENT '显示类型:0列表 1树',
    `keyfield`      VARCHAR(1000) DEFAULT NULL COMMENT '标识字段',
    `parentfield`   VARCHAR(1000) DEFAULT NULL COMMENT '父字段(树)',
    `showfield`     VARCHAR(1000) DEFAULT NULL COMMENT '显示字段',
    `detailpageurl` VARCHAR(1000) DEFAULT NULL COMMENT '详情页URL',
    `typename`      CHAR(1)      DEFAULT NULL COMMENT '类型名称',
    `selecttype`    CHAR(1)      DEFAULT NULL COMMENT '选择类型',
    `showpageurl`   VARCHAR(1000) DEFAULT NULL COMMENT '展示页URL',
    `browserfrom`   INT          DEFAULT 0 COMMENT '浏览框来源',
    `name`          VARCHAR(1000) DEFAULT NULL COMMENT '名称',
    `customid`      INT          DEFAULT NULL COMMENT '自定义ID',
    `customhref`    VARCHAR(4000) DEFAULT NULL COMMENT '自定义链接',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览框配置';

-- 4.2 浏览框类型配置表（mode_browser_type）
CREATE TABLE IF NOT EXISTS `mode_browser_type` (
    `id`            BIGINT       NOT NULL COMMENT '类型ID',
    `browserid`     BIGINT       DEFAULT NULL COMMENT '浏览框ID',
    `typename`      VARCHAR(200) DEFAULT NULL COMMENT '类型名称',
    `typevalue`     VARCHAR(200) DEFAULT NULL COMMENT '类型值',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_browserid` (`browserid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览框类型配置';

-- ============================================================================
-- 批次5：流程配置表
-- ============================================================================

-- 5.1 触发工作流设置表（mode_triggerworkflowset）
CREATE TABLE IF NOT EXISTS `mode_triggerworkflowset` (
    `id`            BIGINT       NOT NULL COMMENT '设置ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `triggeropt`    INT          DEFAULT 0 COMMENT '触发操作:0新建 1编辑 2删除 3全部',
    `workflowid`    INT          DEFAULT NULL COMMENT '工作流ID',
    `workflowname`  VARCHAR(200) DEFAULT NULL COMMENT '工作流名称',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `showcondition` TEXT         DEFAULT NULL COMMENT '显示条件JSON',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_workflowid` (`workflowid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发工作流设置';

-- 5.2 触发工作流设置明细表（mode_triggerworkflowsetdetail）
CREATE TABLE IF NOT EXISTS `mode_triggerworkflowsetdetail` (
    `id`              BIGINT       NOT NULL COMMENT '明细ID',
    `setid`           BIGINT       DEFAULT NULL COMMENT '设置主表ID',
    `fieldid`         INT          DEFAULT NULL COMMENT '模块字段ID',
    `workflowfieldid` INT          DEFAULT NULL COMMENT '流程字段ID',
    `maptype`         INT          DEFAULT 0 COMMENT '映射方式:0直接 1固定值 2公式',
    `fixedvalue`      VARCHAR(500) DEFAULT NULL COMMENT '固定值或公式',
    `tenant_id`       VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_setid` (`setid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发工作流设置明细';

-- 5.3 流程到模块设置表（mode_workflowtomodeset）
CREATE TABLE IF NOT EXISTS `mode_workflowtomodeset` (
    `id`            BIGINT       NOT NULL COMMENT '设置ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `workflowid`    INT          DEFAULT NULL COMMENT '工作流ID',
    `workflowname`  VARCHAR(200) DEFAULT NULL COMMENT '工作流名称',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `writemode`     INT          DEFAULT 0 COMMENT '写入模式:0新建 1更新',
    `fieldmapping`  TEXT         DEFAULT NULL COMMENT '字段映射JSON',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_workflowid` (`workflowid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程到模块设置';

-- ============================================================================
-- 批次6：搜索/树/其他配置表
-- ============================================================================

-- 6.1 自定义搜索配置表（mode_customsearch）
CREATE TABLE IF NOT EXISTS `mode_customsearch` (
    `id`              BIGINT       NOT NULL COMMENT '搜索ID',
    `modeid`          INT          DEFAULT NULL COMMENT '模块ID',
    `searchname`      VARCHAR(200) DEFAULT NULL COMMENT '搜索名称',
    `searchcondition` TEXT         DEFAULT NULL COMMENT '搜索条件JSON',
    `isdefault`       INT          DEFAULT 0 COMMENT '是否默认:1是 0否',
    `dsporder`        INT          DEFAULT 0 COMMENT '排序号',
    `creater`         INT          DEFAULT NULL COMMENT '创建人',
    `tenant_id`       VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义搜索配置';

-- 6.2 自定义搜索按钮表（mode_customsearchbutton）
CREATE TABLE IF NOT EXISTS `mode_customsearchbutton` (
    `id`            BIGINT       NOT NULL COMMENT '按钮ID',
    `searchid`      BIGINT       DEFAULT NULL COMMENT '搜索ID',
    `buttonname`    VARCHAR(200) DEFAULT NULL COMMENT '按钮名称',
    `buttonaction`  VARCHAR(500) DEFAULT NULL COMMENT '按钮动作',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_searchid` (`searchid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义搜索按钮';

-- 6.3 自定义树配置表（mode_customtree）
CREATE TABLE IF NOT EXISTS `mode_customtree` (
    `id`            BIGINT       NOT NULL COMMENT '树ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `treename`      VARCHAR(200) DEFAULT NULL COMMENT '树名称',
    `datasql`       TEXT         DEFAULT NULL COMMENT '数据SQL',
    `showfield`     VARCHAR(200) DEFAULT NULL COMMENT '显示字段',
    `idfield`       VARCHAR(200) DEFAULT NULL COMMENT 'ID字段',
    `parentidfield` VARCHAR(200) DEFAULT NULL COMMENT '父ID字段',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义树配置';

-- 6.4 自定义树明细表（mode_customtreedetail）
CREATE TABLE IF NOT EXISTS `mode_customtreedetail` (
    `id`            BIGINT       NOT NULL COMMENT '明细ID',
    `treeid`        BIGINT       DEFAULT NULL COMMENT '树ID',
    `nodeid`        INT          DEFAULT NULL COMMENT '节点ID',
    `nodename`      VARCHAR(200) DEFAULT NULL COMMENT '节点名称',
    `parentid`      INT          DEFAULT NULL COMMENT '父节点ID',
    `dsporder`      INT          DEFAULT 0 COMMENT '排序号',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_treeid` (`treeid`),
    KEY `idx_parent` (`parentid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义树明细';

-- 6.5 编码规则表（ModeCode）
CREATE TABLE IF NOT EXISTS `ModeCode` (
    `id`            BIGINT       NOT NULL COMMENT '编码ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `codename`      VARCHAR(200) DEFAULT NULL COMMENT '编码名称',
    `codeformat`    VARCHAR(500) DEFAULT NULL COMMENT '编码格式',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码规则';

-- 6.6 编码规则明细表（ModeCodeDetail）
CREATE TABLE IF NOT EXISTS `ModeCodeDetail` (
    `id`            BIGINT       NOT NULL COMMENT '明细ID',
    `codeid`        BIGINT       DEFAULT NULL COMMENT '编码主表ID',
    `seq`           INT          DEFAULT 0 COMMENT '序号',
    `segmenttype`   INT          DEFAULT 0 COMMENT '段类型:0固定值 1日期 2流水号 3字段值',
    `segmentvalue`  VARCHAR(500) DEFAULT NULL COMMENT '段值',
    `length`        INT          DEFAULT 0 COMMENT '长度',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_codeid` (`codeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编码规则明细';

-- 6.7 导入导出记录表（mode_impexp_recorddetail）
CREATE TABLE IF NOT EXISTS `mode_impexp_recorddetail` (
    `id`            BIGINT       NOT NULL COMMENT '记录ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `operatetype`   VARCHAR(10)  DEFAULT NULL COMMENT '操作类型:import/export',
    `filename`      VARCHAR(500) DEFAULT NULL COMMENT '文件名',
    `records`       INT          DEFAULT 0 COMMENT '记录数',
    `status`        INT          DEFAULT 0 COMMENT '状态',
    `creater`       INT          DEFAULT NULL COMMENT '操作人',
    `createdate`    VARCHAR(10)  DEFAULT NULL COMMENT '操作日期',
    `createtime`    VARCHAR(8)   DEFAULT NULL COMMENT '操作时间',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入导出记录';

-- 6.8 默认值表（defaultvalue）
CREATE TABLE IF NOT EXISTS `defaultvalue` (
    `id`            BIGINT       NOT NULL COMMENT 'ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `fieldid`       INT          DEFAULT NULL COMMENT '字段ID',
    `defaulttype`   INT          DEFAULT 0 COMMENT '默认值类型:0固定值 1公式 2当前用户 3当前日期',
    `defaultvalue`  VARCHAR(500) DEFAULT NULL COMMENT '默认值',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='默认值配置';

-- 6.9 属性联动表（modeattrlinkage）
CREATE TABLE IF NOT EXISTS `modeattrlinkage` (
    `id`            BIGINT       NOT NULL COMMENT '联动ID',
    `modeid`        INT          DEFAULT NULL COMMENT '模块ID',
    `sourcefieldid` INT          DEFAULT NULL COMMENT '源字段ID',
    `targetfieldid` INT          DEFAULT NULL COMMENT '目标字段ID',
    `linkage_rule`  TEXT         DEFAULT NULL COMMENT '联动规则JSON',
    `status`        INT          DEFAULT 1 COMMENT '是否启用',
    `tenant_id`     VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='属性联动';

-- 6.10 表达式基础表（mode_expressionbase）
CREATE TABLE IF NOT EXISTS `mode_expressionbase` (
    `id`              BIGINT       NOT NULL COMMENT '表达式ID',
    `modeid`          INT          DEFAULT NULL COMMENT '模块ID',
    `expressname`     VARCHAR(200) DEFAULT NULL COMMENT '表达式名称',
    `expresscontent`  TEXT         DEFAULT NULL COMMENT '表达式内容',
    `targetfieldid`   INT          DEFAULT NULL COMMENT '目标字段ID',
    `expresstype`     INT          DEFAULT 0 COMMENT '表达式类型:0计算 1校验',
    `tenant_id`       VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_modeid` (`modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表达式基础';

-- ============================================================================
-- 批次7：动态表模板（占位表，运行时根据 billid 动态创建）
-- ============================================================================

-- 7.1 动态主数据表示例（实际运行时创建 formtable_main_{billid}）
-- CREATE TABLE IF NOT EXISTS `formtable_main_{billid}` (
--     `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '数据ID',
--     `requestId`       INT          DEFAULT NULL COMMENT '关联流程请求ID',
--     `modedatacreater` INT          DEFAULT NULL COMMENT '创建人',
--     `modedatacreatedate` VARCHAR(10) DEFAULT NULL COMMENT '创建日期',
--     `modedatacreatetime` VARCHAR(8)  DEFAULT NULL COMMENT '创建时间',
--     `modedatamodifier` INT          DEFAULT NULL COMMENT '修改人',
--     `modedatamodifydate` VARCHAR(10) DEFAULT NULL COMMENT '修改日期',
--     `modedatamodifytime` VARCHAR(8)  DEFAULT NULL COMMENT '修改时间',
--     `MODEUUID`        VARCHAR(100) DEFAULT NULL COMMENT '模块UUID',
--     `lastModDate`     CHAR(10)     DEFAULT NULL COMMENT '最后修改日期',
--     `lastModTime`     CHAR(8)      DEFAULT NULL COMMENT '最后修改时间',
--     `field0001`       ...          DEFAULT NULL COMMENT '实际字段由 workflow_billfield 定义',
--     ...
--     PRIMARY KEY (`id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态主数据表';

-- 7.2 动态明细表示例（运行时创建 formtable_main_{billid}_dt1, _dt2...）
-- CREATE TABLE IF NOT EXISTS `formtable_main_{billid}_dt1` (
--     `id`              BIGINT       NOT NULL AUTO_INCREMENT,
--     `mainid`          INT          DEFAULT NULL COMMENT '主表ID',
--     `field0001`       ...          DEFAULT NULL,
--     ...
--     PRIMARY KEY (`id`),
--     KEY `idx_mainid` (`mainid`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态明细表';

-- ============================================================================
-- 索引汇总（用于创建动态表时的标准字段）
-- ============================================================================
-- 动态主表标准字段（除用户自定义字段外）：
--   id, requestId, modedatacreater, modedatacreatedate, modedatacreatetime,
--   modedatamodifier, modedatamodifydate, modedatamodifytime, MODEUUID, lastModDate, lastModTime
--
-- 动态明细表标准字段：
--   id, mainid
-- ============================================================================

-- ============================================================================
-- 初始化完成标记
-- ============================================================================
-- 以上 SQL 执行完成后，blade 数据库包含全部配置表
-- 动态数据表（formtable_main_xxx）在运行时通过 DynamicTableNameInterceptor 自动创建
-- ============================================================================

-- ============================================================================
-- 批次8：表单布局表（Excel设计器）
-- ============================================================================

-- 8.1 表单布局表（form_layout）
-- 用于存储Excel风格的表单布局，JSON格式，兼容泛微E9实际格式
CREATE TABLE IF NOT EXISTS `form_layout` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '布局ID',
    `form_id`       BIGINT       NOT NULL COMMENT '表单ID（关联workflow_bill）',
    `layout_name`   VARCHAR(200) DEFAULT NULL COMMENT '布局名称',
    `layout_json`   LONGTEXT     DEFAULT NULL COMMENT '布局JSON（兼容泛微E9格式）',
    `layout_config`  TEXT         DEFAULT NULL COMMENT '布局配置JSON',
    `status`         INT          DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `tenant_id`      VARCHAR(32)  DEFAULT '000000' COMMENT '租户ID',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_form_id` (`form_id`),
    KEY `idx_tenant` (`tenant_id`),
    CONSTRAINT `fk_layout_form` FOREIGN KEY (`form_id`) REFERENCES `workflow_bill`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单布局（Excel设计器）';
