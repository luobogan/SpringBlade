package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.time.LocalDateTime;

/**
 * 公文督办记录（按实例，对应 E9 odoc_superviseinfo）。
 *
 * <p>对在途公文发起的一条督办：督办人(supervisorId) 督促发起人(starterId) 的实例尽快办理。
 * status：0督办中 1已办结 2已取消。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_supervise")
@Schema(description = "公文督办记录(按实例)")
public class WfOdocSupervise extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 督办中 */
    public static final int STATUS_DOING = 0;
    /** 已办结 */
    public static final int STATUS_FINISHED = 1;
    /** 已取消 */
    public static final int STATUS_CANCELED = 2;

    @Schema(description = "流程实例ID")
    private Long instanceId;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "公文类型 1发文 2收文 3签报")
    private Integer officalType;

    @Schema(description = "督办人ID")
    private Long supervisorId;

    @Schema(description = "被督办对象(实例发起人)ID")
    private Long starterId;

    @Schema(description = "督办状态 0督办中 1已办结 2已取消")
    private Integer superviseStatus;

    @Schema(description = "督办期限")
    private LocalDateTime deadline;

    @Schema(description = "督办说明")
    private String remark;

    @Schema(description = "最近提醒时间")
    private LocalDateTime lastRemindTime;

    @Schema(description = "测试态标记 1=测试产生(可一键清理)")
    private Integer isTest;

}
