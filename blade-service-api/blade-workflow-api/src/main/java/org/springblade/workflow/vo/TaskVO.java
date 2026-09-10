package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务视图对象（引擎任务）
 */
@Data
@Schema(description = "任务")
public class TaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID（引擎任务ID）")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "流程实例ID（引擎实例ID）")
    private String processInstanceId;

    @Schema(description = "任务节点Key")
    private String taskDefinitionKey;

    @Schema(description = "办理人")
    private String assignee;

    public TaskVO() {
    }

    public TaskVO(String taskId, String taskName, String processInstanceId, String taskDefinitionKey) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.processInstanceId = processInstanceId;
        this.taskDefinitionKey = taskDefinitionKey;
    }

}
