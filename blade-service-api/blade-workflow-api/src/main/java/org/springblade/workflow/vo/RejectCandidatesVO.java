package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 可退回节点集合（退回前查询）。
 *
 * <p>对应 ecology 退回时前端 {@code RejectNodeSet.jsp} 取到的候选节点 + 退回方式。</p>
 */
@Data
@Schema(description = "可退回节点集合（退回前查询）")
public class RejectCandidatesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "退回方式：1=直接退回（退到默认/上一节点，不弹窗） 2=选择退回节点（弹窗选节点）")
    private int type;

    @Schema(description = "默认退回节点Key")
    private String defaultNodeKey;

    @Schema(description = "退回时是否提醒已走过的节点")
    private boolean remind;

    @Schema(description = "退回时是否允许变更退回节点")
    private boolean changeNode;

    @Schema(description = "候选节点（已按离当前节点由近及远排序）")
    private List<RejectCandidateVO> nodes;

}
