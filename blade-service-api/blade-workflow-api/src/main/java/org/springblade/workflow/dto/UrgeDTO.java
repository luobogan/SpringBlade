package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 催办 / 督办请求
 *
 * <p>对应 {@code POST /task/{id}/urge}。logType 记为 SUPERVISE(s)。</p>
 */
@Data
@Schema(description = "催办请求")
public class UrgeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "催办意见")
    private String opinion;

}
