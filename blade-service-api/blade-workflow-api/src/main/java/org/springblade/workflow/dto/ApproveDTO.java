package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 同意（批准）请求
 *
 * <p>对应 {@code POST /task/{id}/approve}。logType 记为 APPROVE(0)。</p>
 */
@Data
@Schema(description = "同意请求")
public class ApproveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "附加流程变量")
    private Map<String, Object> variables;

}
