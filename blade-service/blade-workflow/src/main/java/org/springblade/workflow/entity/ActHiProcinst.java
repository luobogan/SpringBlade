package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Flowable 原生历史流程实例表 {@code ACT_HI_PROCINST} 的「读映射」实体。
 *
 * <p>去 wf_ 表改造（P4/P5）后，实例台账的<b>读源</b>从 {@link WfInstance} 切换为本类。
 * 仅映射实例接口实际消费的原生列 + 业务扩展列（{@code DEF_ID_/DATA_ID_/FORM_ID_/TITLE_/
 * BUSINESS_STATUS_/STARTER_/CURRENT_NODE_KEY_/...}），不映射引擎内部冗余列。</p>
 *
 * <p><b>列名全部显式标注</b>：Flowable 原生列是大写 + 尾部下划线（如 {@code BUSINESS_ID_}），
 * MyBatis-Plus 默认驼峰转下划线（{@code businessId → business_id}）无法命中，必须逐列指定。</p>
 *
 * <p><b>雪花 ID 外键</b>：{@link #businessId} 承载原 {@code wf_instance.id} 的 19 位雪花值，
 * 业务表 {@code formtable_main_*.request_id} 仍指向该值，迁移后 {@code InstanceVO.id} 继续返回它。</p>
 *
 * <p>本类<b>只读</b>：业务列由 {@code WfInstanceActWriter} 在引擎同事务内写回，
 * 不通过本实体的 insert/update 直接改表。</p>
 */
@Data
@TableName("ACT_HI_PROCINST")
public class ActHiProcinst implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 运行中 */
    public static final int STATUS_RUNNING = 0;
    /** 通过 */
    public static final int STATUS_APPROVED = 1;
    /** 不通过 */
    public static final int STATUS_REJECTED = 2;
    /** 撤销 */
    public static final int STATUS_CANCELED = 3;
    /** 暂停 */
    public static final int STATUS_SUSPENDED = 4;
    /** 草稿 */
    public static final int STATUS_DRAFT = 5;

    /** Flowable 流程实例ID（原生主键，= wf_instance.engine_inst_id） */
    @TableId(value = "ID_", type = IdType.INPUT)
    private String id;

    /** 业务实例ID（原 wf_instance.id 雪花，业务表 request_id 外键） */
    @TableField("BUSINESS_ID_")
    private Long businessId;

    /** 流程定义ID（blade 业务定义，wf_definition.id） */
    @TableField("DEF_ID_")
    private Long defId;

    /** 业务数据ID（formtable_main_{id}.id） */
    @TableField("DATA_ID_")
    private Long dataId;

    /** 表单ID（workflow_bill.id） */
    @TableField("FORM_ID_")
    private Long formId;

    /** 流程标题 */
    @TableField("TITLE_")
    private String title;

    /** 业务主键 formId:dataId（原生 BUSINESS_KEY_） */
    @TableField("BUSINESS_KEY_")
    private String bizKey;

    /** 业务终态（原生，非变量）：RUNNING/APPROVED/REJECTED/CANCELED/SUSPENDED/DRAFT */
    @TableField("BUSINESS_STATUS_")
    private String businessStatus;

    /** 发起人（原 wf_instance.starter） */
    @TableField("STARTER_")
    private Long starter;

    /** 当前节点Key（advance 维护） */
    @TableField("CURRENT_NODE_KEY_")
    private String currentNodeKey;

    /** 紧急程度 0/1/2 */
    @TableField("URGENCY_")
    private Integer urgency;

    /** 是否测试态 1=是 0=否 */
    @TableField("IS_TEST_")
    private Integer isTest;

    /** L3自检：业务数据行是否就绪 1/0/NULL */
    @TableField("BUSINESS_ROW_READY_")
    private Integer businessRowReady;

    /** L3自检：业务行 request_id 是否已回填 1/0/NULL */
    @TableField("REQUEST_ID_BOUND_")
    private Integer requestIdBound;

    /** L3自检：引擎 latest 部署 == 定义 deployment_id 1/0/NULL */
    @TableField("ENGINE_DEPLOY_MATCHED_")
    private Integer engineDeploymentMatched;

    /** 父流程实例ID（子流程） */
    @TableField("PARENT_ID_")
    private Long parentId;

    /** 测试临时部署ID（清理用） */
    @TableField("TEST_DEPLOYMENT_ID_")
    private String testDeploymentId;

    /** 期望终态 intent 1/2/3/NULL */
    @TableField("PENDING_STATUS_")
    private Integer pendingStatus;

    /** 租户ID（Flowable 标准列） */
    @TableField("TENANT_ID_")
    private String tenantId;

    /** 本实例实际使用的引擎流程定义ID（ACT_RE_PROCDEF.ID_） */
    @TableField("PROC_DEF_ID_")
    private String procDefId;

    /** 发起时间 */
    @TableField("START_TIME_")
    private Date startTime;

    /** 结束时间 */
    @TableField("END_TIME_")
    private Date endTime;

    /** 业务终态字符串 → 状态码（对齐 WfInstance 语义） */
    public static Integer statusCode(String businessStatus) {
        if (businessStatus == null) {
            return STATUS_RUNNING;
        }
        return switch (businessStatus) {
            case "APPROVED" -> STATUS_APPROVED;
            case "REJECTED" -> STATUS_REJECTED;
            case "CANCELED" -> STATUS_CANCELED;
            case "SUSPENDED" -> STATUS_SUSPENDED;
            case "DRAFT" -> STATUS_DRAFT;
            default -> STATUS_RUNNING;
        };
    }
}
