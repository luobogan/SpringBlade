package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 自由流程「流转设定」中的单个步骤行。
 *
 * <p>对齐 ecology {@code FreeWorkflowSet.jsp} 的四列：顺序 / 节点名 / 会签类型 / 操作者。
 * {@code boundNodeKey} 由服务端绑定到预置池静态 activityId，前端不传（防伪造）。</p>
 */
@Data
@Schema(description = "自由流程步骤行")
public class FreeFlowStepDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤顺序（从1开始；保存时按本字段重排）")
    private Integer flowOrder;

    @Schema(description = "步骤显示名")
    private String nodeName;

    @Schema(description = "办理人ID列表")
    private List<Long> operatorIds;

    @Schema(description = "会签关系：0非会签 1会签 2依次逐个")
    private Integer signType;

    @Schema(description = "是否定稿取号点")
    private Boolean isFinalize;

    @Schema(description = "是否套红必做点")
    private Boolean mustTaohong;

    @Schema(description = "并行分支组标识；为空表示串行")
    private String parallelGroup;

    @Schema(description = "影子节点扩展配置（JSON 串，可选）")
    private String extJson;

}
