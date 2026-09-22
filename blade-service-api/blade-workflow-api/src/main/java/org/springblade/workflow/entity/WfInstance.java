package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 流程实例实体
 *
 * <p>对齐 ecology {@code workflow_requestbase}。</p>
 * <ul>
 *   <li>status：0运行中 1通过 2不通过 3撤销 4暂停
 *       （对齐 ecology {@code currentstatus} 语义；注意 ecology 的 status 是状态文字描述，
 *       真正的状态码是 currentstatus，本项目只保留语义正确的单一 status）</li>
 *   <li>engine_inst_id：Flowable 实例ID，<b>弱关联</b>，业务语义不依赖 ACT_* 表，
 *       保留替换引擎的演进位</li>
 *   <li>biz_key：{@code formId:dataId}，支持按业务数据反查流程</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_instance")
@Schema(description = "流程实例")
public class WfInstance extends TenantEntity {

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
    /** 草稿（已保存未提交：不占引擎，关联发起人待办，提交后原地提升为运行中） */
    public static final int STATUS_DRAFT = 5;

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎实例ID（Flowable PROC_INST_ID_）")
    private String engineInstId;

    @Schema(description = "流程定义ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "表单ID（workflow_bill.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "业务数据ID（formtable_main_{id}.id）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dataId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "业务主键 formId:dataId")
    private String bizKey;

    @Schema(description = "当前节点")
    private String currentNodeKey;

    @Schema(description = "发起人")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long starter;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "发起时间")
    private Date startTime;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "紧急程度 0/1/2")
    private Integer urgency;

    @Schema(description = "父流程实例（子流程）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @Schema(description = "测试态标记 1=测试产生的实例（可一键清理，不污染正常数据）")
    private Integer isTest;

    /**
     * L3 运行时自检：业务数据行是否就绪。
     *
     * <p>1 = 业务表里有对应行（单据发起时行本就存在 / 本次现场创建成功）；
     * 0 = 创建失败，用了唯一占位 dataId（流程能跑，但业务表里查不到这张单，
     * 见巡检 ①）。NULL = 未自检（存量实例 / 早期版本发起）。</p>
     */
    @Schema(description = "L3自检：业务数据行是否就绪 1=有对应行 0=仅占位 NULL=未自检")
    private Integer businessRowReady;

    /**
     * L3 运行时自检：业务行的 {@code request_id} 是否已回写为本实例ID（流程 ↔ 单据闭环）。
     *
     * <p>1 = 已回填；0 = 未回填 / 回填失败（见巡检 ②）；NULL = 无需回填（单据发起，
     * 关联关系由单据侧维护）或未自检。</p>
     */
    @Schema(description = "L3自检：业务行 request_id 是否已回填 1=是 0=否 NULL=无需/未自检")
    private Integer requestIdBound;

    /**
     * L3 运行时自检：引擎中该 procKey 的最新部署 == 本定义记录的 {@code deployment_id}。
     *
     * <p>1 = 一致（正式版本仍占据 latest）；0 = 不一致（latest 被测试部署 / 手工部署顶替）；
     * NULL = 未知（定义尚未落 deployment_id，或本次为测试态发起）。</p>
     */
    @Schema(description = "L3自检：引擎 latest 部署 == 定义 deployment_id 1=一致 0=被顶替 NULL=未知")
    private Integer engineDeploymentMatched;

    @Schema(description = "测试临时部署ID（Flowable deploymentId），清理时级联卸载")
    private String testDeploymentId;

    /**
     * 本实例实际使用的 Flowable {@code processDefinitionId}（{@code ACT_RE_PROCDEF.ID_}）。
     *
     * <p>审计与回滚依据：同一 procKey 多版本并存时，凭本列可确定这张单跑的是哪一版；
     * 灰度期还可据此统计「新版本跑了多少单、有无卡单」（巡检 ⑨）。NULL = 存量数据。</p>
     */
    @Schema(description = "本实例实际使用的引擎流程定义ID（ACT_RE_PROCDEF.ID_）；NULL=存量")
    private String procDefId;

    /**
     * 是否灰度实例：1 = 本次发起命中灰度规则、走的是灰度版本；0 = 正式版本。
     *
     * <p>用于灰度期的观测与回滚排查（方案 §4.3）：按定义维度统计灰度单量、健康度，
     * 出问题先切回 base 版本（在途实例不受影响，Flowable 原生按定义 ID 隔离）。</p>
     */
    @Schema(description = "灰度实例 1=走灰度版本 0=正式版本")
    private Integer isGray;

    /**
     * 主键以字符串形式序列化（对齐 {@code WfProcessDefinition}）：
     * 避免 19 位雪花 ID 在前端 JS 解析时丢失精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
