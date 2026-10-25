package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 公文电子签章签署流程（e签宝 SaaS API V3）
 *
 * <p>一条记录 = 一次用印签署业务，与公文正文（{@code docId}）、流程实例（{@code instId}）关联；
 * {@code bizNum} 为发起幂等键（唯一索引），{@code flowId} 为 e签宝侧签署流程ID（唯一索引）。
 * 定稿后发起签署，签署完成由回调/补偿更新 {@code signStatus}，再把签章件归档回写为
 * 新的公文版本（{@code archiveVersionId}）。</p>
 *
 * <p>注意：父类 BaseEntity 的 {@code status} 是「1正常 0禁用」的公共列，
 * 本表的签署业务状态一律走 {@code signStatus}，两者不可混用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_sign_flow")
@Schema(description = "公文电子签章签署流程")
public class WfOdocSignFlow extends TenantEntity {

    /** 待发起（已落库，尚未调用 e签宝） */
    public static final int SIGN_STATUS_PENDING = 0;
    /** 进行中（已发起，等待签署） */
    public static final int SIGN_STATUS_RUNNING = 1;
    /** 已完成（全部签署方完成） */
    public static final int SIGN_STATUS_FINISHED = 2;
    /** 已撤销 */
    public static final int SIGN_STATUS_CANCELED = 3;
    /** 已拒签 */
    public static final int SIGN_STATUS_REJECTED = 4;
    /** 已过期 */
    public static final int SIGN_STATUS_EXPIRED = 5;
    /** 失败（接口异常等） */
    public static final int SIGN_STATUS_FAILED = 9;

    /** 用印方式：自研印章（本地） */
    public static final int SEAL_MODE_LOCAL = 0;
    /** 用印方式：e签宝印章 */
    public static final int SEAL_MODE_ESIGN = 1;

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instId;

    @Schema(description = "公文正文ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long docId;

    @Schema(description = "发起签署时的正文版本ID（原文版本，归档后仍保留）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long srcVersionId;

    @Schema(description = "e签宝文件ID")
    private String esignFileId;

    @Schema(description = "e签宝签署流程ID")
    private String flowId;

    @Schema(description = "业务流水号(customBizNum)，发起幂等键")
    private String bizNum;

    @Schema(description = "签署标题")
    private String title;

    @Schema(description = "用印方式 0自研印章(本地) 1e签宝印章")
    private Integer sealMode;

    @Schema(description = "签署状态 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败")
    private Integer signStatus;

    @Schema(description = "e签宝 signResult（2=签署完成）")
    private Integer signResult;

    @Schema(description = "归档后回写的新版本ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long archiveVersionId;

    @Schema(description = "区块链存证信息(JSON)")
    private String antchainInfo;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "回调次数")
    private Integer notifyCount;

    @Schema(description = "最近回调时间")
    private LocalDateTime lastNotifyTime;

    @Schema(description = "最近补偿查询时间")
    private LocalDateTime lastQueryTime;

    // ==================== 「签完才流转」：待推进上下文 ====================

    /** 推进状态：待推进（提交时发起签署，等待回调推进） */
    public static final int ADVANCE_STATUS_PENDING = 0;
    /** 推进状态：推进中（已抢占，防并发重复推进） */
    public static final int ADVANCE_STATUS_RUNNING = 1;
    /** 推进状态：已推进（引擎已流转到下一节点） */
    public static final int ADVANCE_STATUS_DONE = 2;
    /** 推进状态：已终止（拒签/过期，流程不再自动推进，待办恢复为待办） */
    public static final int ADVANCE_STATUS_TERMINATED = 3;

    @Schema(description = "签章节点Key")
    private String nodeKey;

    @Schema(description = "业务待办ID（wf_task.id，拒签/过期时恢复为待办用）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    @Schema(description = "引擎任务ID（推进时 completeTask 用）")
    private String engineTaskId;

    @Schema(description = "提交人（推进时作为 currentOperator）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operator;

    @Schema(description = "提交变量JSON（推进时回填引擎）")
    private String varsJson;

    @Schema(description = "推进状态 0待推进 1推进中 2已推进 3已终止")
    private Integer advanceStatus;

    @Schema(description = "推进完成时间")
    private LocalDateTime advanceTime;

    @Schema(description = "推进结果/失败原因")
    private String advanceMsg;

    /**
     * 主键以字符串形式序列化，避免 19 位雪花 ID 在前端 JS 解析时丢失精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
