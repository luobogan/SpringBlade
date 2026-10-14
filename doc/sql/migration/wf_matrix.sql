-- =============================================================================
-- 矩阵（Matrix）模块 P0 · 建表脚本
-- 作用：为流程节点操作者 opType=99（矩阵）提供数据模型，运行时按
--       「表单/系统字段值 → 匹配矩阵条件维度 → 取结果维度人员」解析办理人。
--
-- 背景：对齐泛微 E9 矩阵能力，详见
--       doc/md/泛微E9-矩阵管理功能分析及SpringBlade落地建议.md
--
-- ── 关键设计决策（与 E9 的差异，务必先读）──────────────────────────────────
-- 1) 不照搬 E9 的「每矩阵动态建表 Matrixtable_<id>」，改用**统一表 + data_json**。
--    理由：E9 动态建表在多租户、迁移、备份、跨矩阵统计下成本随矩阵数线性增长；
--          本项目已有 wf_form_snapshot.data_json / wf_process_node.ext_json 等同款
--          JSON 存储实践（整列取出 → Java 侧解析），且矩阵行数据量小（单矩阵几十~几百行）。
--    代价：JSON 内字段无 DB 约束 → **一致性由应用层校验保证**（见下方 4）。
--
-- 2) 条件维度（field_type=0）之间是 AND 关系；结果维度（field_type=1）存放人员。
--    data_json 结构：{ "<fieldKey>": "值", ... }，fieldKey 即 wf_matrix_field.field_key。
--
-- 3) data_order 非空且 (matrix_id, data_order) 唯一：
--    修复 E9 「多行命中无 ORDER BY、无去重、dataorder 可空可重复」的缺陷
--    （证据：MatrixUtil.java:622/631-650 无 ORDER BY、无去重）。
--
-- 4) 应用层必须落实的校验（E9 教训转化，见文档 10.3）：
--    · 条件集合为空 = 不匹配（返回空），绝不退化为全表匹配
--    · 条件值为空 → 该条件不参与匹配 + log.warn，禁止拼 is null
--    · 结果维度至少 1 个；每行数据至少一个条件维度有值
--    · field_key 正则白名单 + 保留字黑名单 + 长度限制 + 同矩阵内重名拒绝
--    · 取人后按在职状态过滤；结果 LinkedHashSet 去重保序
--    · 删除矩阵/字段前强校验引用；删除全部走 @Transactional
--
-- 5) 幂等：使用 CREATE TABLE IF NOT EXISTS，可重复执行，**不会清空已有数据**。
--    （不要改成 DROP TABLE IF EXISTS —— 会毁数据。）
-- =============================================================================

USE blade;

-- -----------------------------------------------------------------------------
-- 1) 矩阵主表：一个矩阵 = 一组条件维度 + 若干结果维度 + 多行数据
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_matrix`
(
    `id`           BIGINT       NOT NULL COMMENT '主键（雪花）',
    `tenant_id`    VARCHAR(50)  NOT NULL COMMENT '租户ID',
    `matrix_name`  VARCHAR(100) NOT NULL COMMENT '矩阵名称',
    `matrix_desc`  VARCHAR(500) NULL COMMENT '矩阵描述',
    `is_system`    INT          NOT NULL DEFAULT 0 COMMENT '0=自定义 1=分部系统矩阵 2=部门系统矩阵（对齐E9 issystem）',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
    `create_user`  BIGINT       NULL COMMENT '创建人',
    `create_dept`  BIGINT       NULL COMMENT '创建部门',
    `create_time`  DATETIME     NULL COMMENT '创建时间',
    `update_user`  BIGINT       NULL COMMENT '更新人',
    `update_time`  DATETIME     NULL COMMENT '更新时间',
    `is_deleted`   INT          NULL COMMENT '是否已删除 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_wf_matrix_tenant` (`tenant_id`),
    KEY `idx_wf_matrix_tenant_name` (`tenant_id`, `matrix_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '矩阵主表';

-- -----------------------------------------------------------------------------
-- 2) 矩阵字段定义：条件维度（0）/ 结果维度（1）
--    browser_type 对齐 E9 browsertypeid：4/57=部门，164/194=分部，17=多人力资源，1=人力资源
--    ⚠️ 本实现不把 field_key 当物理列名（JSON 存储），但仍须按白名单校验，
--       避免与 E9 同款的注入/保留字/同名列问题（文档 10.3 #6）。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_matrix_field`
(
    `id`           BIGINT       NOT NULL COMMENT '主键（雪花）',
    `tenant_id`    VARCHAR(50)  NOT NULL COMMENT '租户ID',
    `matrix_id`    BIGINT       NOT NULL COMMENT '所属矩阵 wf_matrix.id',
    `field_key`    VARCHAR(50)  NOT NULL COMMENT '字段唯一键（应用层唯一，用于 data_json 的 key）',
    `display_name` VARCHAR(100) NOT NULL COMMENT '字段显示名',
    `browser_type` INT          NULL COMMENT '浏览按钮类型（对齐E9 browsertypeid：4/57部门 164/194分部 17多人力资源 1人力资源）',
    `field_type`   INT          NOT NULL COMMENT '0=条件维度（黄） 1=结果维度（蓝）',
    `field_order`  INT          NOT NULL DEFAULT 0 COMMENT '字段排序',
    `col_width`    INT          NULL COMMENT '列宽',
    `status`       INT          NULL DEFAULT 1 COMMENT '状态（预留，与 wf_matrix 一致，避免 BaseEntity.status 查询缺列）',
    `create_user`  BIGINT       NULL COMMENT '创建人',
    `create_dept`  BIGINT       NULL COMMENT '创建部门',
    `create_time`  DATETIME     NULL COMMENT '创建时间',
    `update_user`  BIGINT       NULL COMMENT '更新人',
    `update_time`  DATETIME     NULL COMMENT '更新时间',
    `is_deleted`   INT          NULL COMMENT '是否已删除 0否 1是',
    PRIMARY KEY (`id`),
    -- 同一矩阵内 field_key 唯一（应用层亦需强校验，此处双保险）
    UNIQUE KEY `uk_wf_matrix_field_key` (`matrix_id`, `field_key`),
    KEY `idx_wf_matrix_field_matrix` (`matrix_id`, `field_type`),
    KEY `idx_wf_matrix_field_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '矩阵字段定义（条件维度/结果维度）';

-- -----------------------------------------------------------------------------
-- 3) 矩阵数据行：一行 = 一组条件值 + 各结果维度人员
--    data_json 形如 {"f_dept":"1001","f_manager":"3,7","f_cashier":"9"}
--    · 条件维度值：单值或逗号分隔多值
--    · 结果维度值：逗号分隔的人员ID串
--    · data_order 非空 + (matrix_id, data_order) 唯一 → 保证行序稳定、可复现
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_matrix_data`
(
    `id`          BIGINT          NOT NULL COMMENT '主键（雪花）',
    `tenant_id`   VARCHAR(50)     NOT NULL COMMENT '租户ID',
    `matrix_id`   BIGINT          NOT NULL COMMENT '所属矩阵 wf_matrix.id',
    `data_order`  DECIMAL(10, 2)  NOT NULL COMMENT '行序（同一矩阵内唯一，升序即优先级）',
    `data_json`   JSON            NOT NULL COMMENT '维度值 JSON：{fieldKey: 值}',
    `status`       INT          NULL DEFAULT 1 COMMENT '状态（预留，与 wf_matrix 一致，避免 BaseEntity.status 查询缺列）',
    `create_user` BIGINT          NULL COMMENT '创建人',
    `create_dept` BIGINT          NULL COMMENT '创建部门',
    `create_time` DATETIME        NULL COMMENT '创建时间',
    `update_user` BIGINT          NULL COMMENT '更新人',
    `update_time` DATETIME        NULL COMMENT '更新时间',
    `is_deleted`  INT             NULL COMMENT '是否已删除 0否 1是',
    PRIMARY KEY (`id`),
    -- 行序唯一：修复 E9 dataorder 可空可重复导致行序不确定的缺陷
    UNIQUE KEY `uk_wf_matrix_data_order` (`matrix_id`, `data_order`),
    -- 取数主路径：按矩阵取全部行，Java 侧做条件 AND 过滤
    KEY `idx_wf_matrix_data_matrix` (`matrix_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '矩阵数据行';

-- =============================================================================
-- 附：无需新增 wf_node_operator.matrix_json 列
--   经勘察，wf_node_operator 已是「P6 停写」的过渡表，节点操作者的**权威源**是
--   BPMN <wf:operator> 扩展属性。因此 matrixJson 只落在 BPMN 属性 + moddle schema
--   （blade-workflow/src/main/resources/moddle/wf-moddle-extension.json），
--   WfNodeOperator 实体用 @TableField(exist=false) 承载，不新增 DB 列，
--   避免与正在进行的「去 wf_ 表」改造冲突。
-- =============================================================================
