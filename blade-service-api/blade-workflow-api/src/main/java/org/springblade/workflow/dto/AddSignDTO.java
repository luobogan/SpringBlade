package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 加签请求
 *
 * <p>对应 {@code POST /task/{id}/add-sign}。</p>
 * <p>addSignType：0前加签（在自己之前） 1后加签（在自己之后）—— 属国产 OA 特有语义，
 * 在自建语义层实现，BPMN 仅承载主干流转。</p>
 */
@Data
@Schema(description = "加签请求")
public class AddSignDTO implements Serializable {

    /** 前加签 */
    public static final int TYPE_BEFORE = 0;
    /** 后加签 */
    public static final int TYPE_AFTER = 1;

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "加签人")
    private Long assignee;

    @Schema(description = "0前加签 1后加签")
    private Integer addSignType;

}
