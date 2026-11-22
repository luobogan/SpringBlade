package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 引擎死信作业摘要（{@code ACT_RU_DEADLETTER_JOB}）。
 *
 * <p>死信非空 = 作业执行失败且重试耗尽 = 台账可能已漏更新，需人工介入（方案C §3.4 兜底三）。</p>
 */
@Data
@Schema(description = "死信作业摘要")
public class DeadLetterJobVO {

    @Schema(description = "死信作业ID")
    private String jobId;

    @Schema(description = "流程实例ID（可能为空，如流程级作业）")
    private String processInstanceId;

    @Schema(description = "流程定义ID")
    private String processDefinitionId;

    @Schema(description = "元素ID（节点key）")
    private String elementId;

    @Schema(description = "作业处理器类型")
    private String handlerType;

    @Schema(description = "剩余重试次数（死信通常为0）")
    private Integer retries;

    @Schema(description = "异常消息")
    private String exceptionMessage;
}
