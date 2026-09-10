package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 转发 / 转办请求
 *
 * <p>对应 {@code POST /task/{id}/forward}。logType 记为 FORWARD(7)。</p>
 */
@Data
@Schema(description = "转发/转办请求")
public class ForwardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "转办目标人")
    private Long assignee;

}
