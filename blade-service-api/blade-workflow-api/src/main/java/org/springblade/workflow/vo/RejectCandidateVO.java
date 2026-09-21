package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 可退回节点候选
 */
@Data
@Schema(description = "可退回节点候选")
public class RejectCandidateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型 0创建 1审批 2提交 3归档 5等待 6自动处理 7网关")
    private Integer nodeType;

}
