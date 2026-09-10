package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 抄送 / 传阅请求
 *
 * <p>对应 {@code POST /task/{id}/circulate}。
 * logType 记为 CIRCULATE(t)；抄送人任务状态为 8抄送（对齐 isremark）。</p>
 */
@Data
@Schema(description = "抄送/传阅请求")
public class CirculateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "抄送人列表")
    private List<Long> assignees;

}
