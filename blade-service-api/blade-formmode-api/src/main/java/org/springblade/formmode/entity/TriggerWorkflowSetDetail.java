package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 触发工作流设置明细实体（对应 mode_triggerworkflowsetdetail 表）
 */
@Data
@TableName("mode_triggerworkflowsetdetail")
@Schema(description = "触发工作流设置明细")
public class TriggerWorkflowSetDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "设置主表ID")
    private Long setid;

    @Schema(description = "模块字段ID（workflow_billfield.id）")
    private Integer fieldid;

    @Schema(description = "流程字段ID")
    private Integer workflowfieldid;

    @Schema(description = "映射方式：0直接 1固定值 2公式")
    private Integer maptype;

    @Schema(description = "固定值或公式")
    private String fixedvalue;

    @Schema(description = "租户ID")
    private String tenantId;

}
