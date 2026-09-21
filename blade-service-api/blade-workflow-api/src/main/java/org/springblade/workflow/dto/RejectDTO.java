package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 退回请求
 *
 * <p>对应 {@code POST /task/{id}/reject}。logType 记为 REJECT(3)。
 * targetNodeKey 为空时按节点「退回设置」退回默认/上一节点（对齐 ecology 默认退回行为）。</p>
 */
@Data
@Schema(description = "退回请求")
public class RejectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审批意见")
    private String opinion;

    @Schema(description = "退回目标节点Key（为空则退回默认/上一节点；选择退回时需显式指定）")
    private String targetNodeKey;

    @Schema(description = "需提醒的已走过节点Key列表（退回时一并提醒，对应 ecology isrejectremind）")
    private List<String> remindNodeKeys;

}
