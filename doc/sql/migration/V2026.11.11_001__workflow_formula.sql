-- =============================================================================
-- 迁移脚本：新建自定义函数表 workflow_formula
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.11_001
-- 目标库    : blade（formmode 所有表统一在 blade 库）
-- 影响表    : workflow_formula（新建）
-- 变更类型  : 建表（DDL，幂等）
-- 是否幂等  : 是（IF NOT EXISTS，重复执行不报错）
-- 是否丢数据: 否
-- 回滚脚本  : DROP TABLE IF EXISTS `workflow_formula`;
--
-- 背景
--   对齐泛微 E9 workflow_formula 表（新建自定义函数）。用户在公式弹框的「函数」面板里
--   维护自定义函数（名称 / 描述 / 分类 / JS 实现），保存后在公式表达式里即可像内置函数
--   一样被引用（如 MYFUNC(A1)）。
--   作用域 formscope 为空表示全局可用；也可绑定到具体表单ID，仅该表单的公式面板可见。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `workflow_formula` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '函数名（如 MYFUNC，公式中引用）',
    `formuladesc` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '函数描述',
    `category`    VARCHAR(100) NOT NULL DEFAULT '' COMMENT '函数分类（自定义分组名，用于公式面板分组）',
    `fncode`      TEXT                             COMMENT '函数实现（JS代码/公式规则）',
    `formscope`   VARCHAR(64)  DEFAULT NULL        COMMENT '作用域：空=全局，或具体表单ID',
    `status`      TINYINT      NOT NULL DEFAULT 1  COMMENT '状态（1正常 0禁用）',
    `createtime`  VARCHAR(20)  DEFAULT NULL        COMMENT '创建时间',
    `modifytime`  VARCHAR(20)  DEFAULT NULL        COMMENT '修改时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0  COMMENT '是否删除（0未删除 1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_formula_scope` (`formscope`),
    KEY `idx_workflow_formula_category` (`category`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '自定义函数（对标泛微E9 workflow_formula）';

-- 校验
SELECT
    (SELECT COUNT(1) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_formula') AS workflow_formula_should_be_1;
