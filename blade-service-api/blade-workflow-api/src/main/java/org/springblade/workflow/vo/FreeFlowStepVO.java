package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 自由流程步骤编排回显。
 */
@Data
@Schema(description = "自由流程步骤回显")
public class FreeFlowStepVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤顺序")
    private Integer flowOrder;

    @Schema(description = "步骤显示名")
    private String nodeName;

    @Schema(description = "绑定的预置池静态 activityId")
    private String boundNodeKey;

    @Schema(description = "办理人ID列表")
    private List<Long> operatorIds;

    @Schema(description = "会签关系")
    private Integer signType;

    @Schema(description = "是否定稿取号点")
    private Boolean isFinalize;

    @Schema(description = "是否套红必做点")
    private Boolean mustTaohong;

    @Schema(description = "并行分支组标识")
    private String parallelGroup;

    @Schema(description = "0未流转 1已流转（已流转行前端置灰）")
    private Integer nodeStatus;

    @Schema(description = "扩展配置")
    private String extJson;

}
