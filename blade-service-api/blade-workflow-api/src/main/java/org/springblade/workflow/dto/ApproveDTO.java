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

    @Schema(description = "指定流转-下一节点Key（仅当前节点开启「指定流转」时生效；为空则按默认流向）")
    private String nextNodeKey;

    @Schema(description = "指定流转-下一节点操作者（模式1=用户指定；模式2忽略，取目标节点设置）")
    private Long nextAssignee;

    @Schema(description = "明文密码（仅当前节点开启「二次认证」时需要）")
    private String password;

}
