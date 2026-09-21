-- ============================================================================
-- 版本    : V2026.09.21_006
-- 目标库  : blade_workflow
-- 影响表  : wf_node_timeout（新建）、wf_task（加 timeout_handled 列）
-- 变更类型: 超时「单条 hours」升级为「多条规则」——起算字段/时长/截止（固定时刻·表单时间字段）
--           + 四态动作（自动通过/转指定节点/指定操作者/提醒）+ 提醒通道与对象；并加防重列。
-- 是否幂等: 是（CREATE TABLE IF NOT EXISTS + 列存在性守卫）
-- 是否丢数据: 否
-- 回滚脚本: DROP TABLE IF EXISTS `wf_node_timeout`; ALTER TABLE `wf_task` DROP COLUMN `timeout_handled`;
-- 决策依据: 泛微节点信息「超时设置」（超时时长 / 超时动作 / 提醒）；旧 settings.timeout 单条 hours 仅作兜底兼容（不迁移，读取时回退）。
-- ============================================================================

USE `blade_workflow`;

CREATE TABLE IF NOT EXISTS `wf_node_timeout` (
    `id`             BIGINT           NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`      VARCHAR(12)      NOT NULL DEFAULT '000000' COMMENT '租户',
    `def_id`         BIGINT           NOT NULL                COMMENT '流程定义 ID',
    `node_key`       VARCHAR(64)      NOT NULL                COMMENT '节点 Key',
    `seq`            INT              NOT NULL DEFAULT 0      COMMENT '排序（升序执行）',
    `enabled`        TINYINT          NOT NULL DEFAULT 1      COMMENT '是否启用 1=是 0=否',
    `start_type`     TINYINT          NOT NULL DEFAULT 1      COMMENT '起算方式 1=节点到达(收到待办) 2=表单时间字段',
    `start_field`    VARCHAR(64)      NULL                    COMMENT '起算=表单时间字段时的字段名',
    `duration_min`   INT              NULL                    COMMENT '截止=相对时长时的分钟数',
    `end_type`       TINYINT          NOT NULL DEFAULT 1      COMMENT '截止方式 1=相对(起算+时长) 2=固定时刻(HH:mm) 3=表单时间字段',
    `end_fixed_time` VARCHAR(8)       NULL                    COMMENT '截止=固定时刻时的 HH:mm',
    `end_field`      VARCHAR(64)      NULL                    COMMENT '截止=表单时间字段时的字段名',
    `action_way`     VARCHAR(16)      NOT NULL DEFAULT 'autoApprove' COMMENT '超时动作 autoApprove/forward/assign/remind',
    `target_node_key`VARCHAR(64)      NULL                    COMMENT '动作=forward 时的目标节点（扩展位，当前按 autoApprove 语义流转）',
    `operator_ids`   VARCHAR(255)     NULL                    COMMENT '动作=assign 时的指定操作者（人力资源 ID 逗号分隔）',
    `opinion`        VARCHAR(255)     NULL                    COMMENT '动作意见',
    `remind_types`   VARCHAR(32)      NULL                    COMMENT '提醒方式 sys=流程提醒 ml=短信 sm=邮件（逗号分隔）',
    `remind_before_operator` TINYINT  NOT NULL DEFAULT 0      COMMENT '提醒对象：节点当前处理人 1=是 0=否',
    `remind_persons` VARCHAR(255)     NULL                    COMMENT '提醒对象：指定人员（人力资源 ID 逗号分隔）',
    `create_time`    DATETIME         NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_node_timeout_def_node` (`def_id`, `node_key`, `seq`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '节点超时规则（多条，对齐泛微超时设置）';

-- wf_task 加「超时动作已执行」防重列
SET @v_exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_task' AND COLUMN_NAME = 'timeout_handled');
SET @v_sql = IF(@v_exists = 0,
  'ALTER TABLE `wf_task` ADD COLUMN `timeout_handled` TINYINT NOT NULL DEFAULT 0 COMMENT ''超时动作是否已执行（防重复触发）''',
  'SELECT 1');
PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 尾部校验
SELECT 'wf_node_timeout' AS `table`, COUNT(1) AS `exists_flag`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_timeout';
