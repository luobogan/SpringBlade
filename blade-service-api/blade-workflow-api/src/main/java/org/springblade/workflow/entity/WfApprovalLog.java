package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 审批流转记录实体
 *
 * <p>对齐 ecology {@code workflow_requestlog}，log_type 对齐 {@code RequestLogType}：</p>
 * <p>0批准 2提交 3退回 7转发 9批注 h转办 s督办 t抄送 y批示；
 * <b>正常流转仅认定 SUBMIT(2) 与 APPROVE(0)</b>。</p>
 * <p>本表为日志大表，已按月 RANGE 分区（分区列 operate_time 纳入主键，含 p_max 兜底分区）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_approval_log")
@Schema(description = "审批流转记录")
public class WfApprovalLog extends TenantEntity {

    /** 批准 */
    public static final String LOG_APPROVE = "0";
    /** 提交 */
    public static final String LOG_SUBMIT = "2";
    /** 退回 */
    public static final String LOG_REJECT = "3";
    /** 转发 */
    public static final String LOG_FORWARD = "7";
    /** 批注 */
    public static final String LOG_COMMENT = "9";
    /** 转办 */
    public static final String LOG_TRANSFER = "h";
    /** 督办 */
    public static final String LOG_SUPERVISE = "s";
    /** 抄送 */
    public static final String LOG_CIRCULATE = "t";
    /** 批示 */
    public static final String LOG_INSTRUCTION = "y";

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "流转动作（对齐 RequestLogType）")
    private String logType;

    @Schema(description = "审批意见")
    private String opinion;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "操作时间（分区列）")
    private Date operateTime;

}
