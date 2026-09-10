package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 退回请求
 *
 * <p>对应 {@code POST /task/{id}/reject}。logType 记为 REJECT(3)。
 * targetNodeKey 为空时退回至上一步处理节点（对齐 ecology 默认退回行为）。</p>
 */
@Data
@Schema(description = "退回请求")
public class RejectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "退回目标节点Key（为空则退回上一节点）")
    private String targetNodeKey;

}
