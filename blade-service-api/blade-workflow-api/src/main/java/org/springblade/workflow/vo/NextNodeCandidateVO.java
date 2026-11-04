package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 「下一步设置」的单个候选节点。
 *
 * <p>权限口径遵循 WF-N9「条件 ≠ 权限」：{@code selectable} 与 {@code allowedUserIds}
 * 由服务端按节点白名单、选人范围、节点级提交权限独立计算，<b>不依赖出口条件求值</b>；
 * 前端不可据此伪造目标节点或办理人，提交时服务端会二次校验。</p>
 */
@Data
@Schema(description = "下一步候选节点")
public class NextNodeCandidateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点Key（预置池静态 activityId）")
    private String nodeKey;

    @Schema(description = "节点显示名")
    private String nodeName;

    @Schema(description = "是否可选（无权限时为 false 并给出原因）")
    private Boolean selectable;

    @Schema(description = "不可选原因")
    private String disabledReason;

    @Schema(description = "允许选择的办理人ID列表；空列表=不限（按节点操作者配置解析）")
    private List<Long> allowedUserIds;

    @Schema(description = "是否允许办理人指定办理人")
    private Boolean assigneeEditable;

    @Schema(description = "默认会签关系：0非会签 1会签 2依次逐个")
    private Integer signTypeDefault;

}
