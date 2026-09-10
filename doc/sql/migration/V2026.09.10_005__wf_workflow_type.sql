-- =============================================================================
-- 迁移脚本 005：新建 wf_workflow_type（路径类型，浏览框 wftype 数据源）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_005
-- 目标库    : blade_workflow
-- 影响表    : wf_workflow_type（新建）
-- 变更类型  : 建表 + 种子数据（DDL + DML）
-- 是否幂等  : 是（表存在则跳过建表；种子用 INSERT IGNORE 避免重复）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_workflow_type`;
--
-- 背景
--   对齐 ecology「路径类型」浏览框（BrowserBean type=wftype）。前端新增/编辑「路径」
--   时，「路径类型」字段改用 BROWSER 控件：点击弹出选择弹窗，经
--   GET /api/blade-workflow/definition/browser/wftype 取数，回写 workflow_type.id。
--   本表即该浏览框的数据源。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-workflow。
-- =============================================================================

USE `blade_workflow`;

-- 1) 建表（幂等）---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_workflow_type` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type_name`   VARCHAR(100) NOT NULL                COMMENT '类型名称',
    `type_desc`   VARCHAR(500) NULL                   COMMENT '类型描述',
    `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '显示顺序',
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user` BIGINT       NULL                   COMMENT '创建人',
    `create_dept` BIGINT       NULL                   COMMENT '创建部门',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT       NULL                   COMMENT '修改人',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      INT          NOT NULL DEFAULT 1      COMMENT '0停用 1启用',
    `is_deleted`  INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除:1已删 0未删',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程（路径）类型（浏览框 wftype 数据源）';

-- 2) 种子数据（幂等，INSERT IGNORE）--------------------------------------------
INSERT IGNORE INTO `wf_workflow_type` (`id`, `type_name`, `type_desc`, `sort_order`) VALUES
    (1, '行政审批', '行政审批类流程', 1),
    (2, '人事流程', '招聘、入转调离、考勤等', 2),
    (3, '财务流程', '报销、预算、付款等', 3),
    (4, 'IT流程',  '系统权限、资源申请、运维', 4),
    (5, '业务流程', '各业务运营审批', 5);
