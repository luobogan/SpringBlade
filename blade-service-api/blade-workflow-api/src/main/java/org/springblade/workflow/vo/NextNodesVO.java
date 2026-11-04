package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 「下一步设置」查询结果包装。
 */
@Data
@Schema(description = "下一步候选集合")
public class NextNodesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前实例是否为自由流程（false 时前端走既有指定流转/默认流向）")
    private Boolean freeFlow;

    @Schema(description = "当前节点Key")
    private String currentNodeKey;

    @Schema(description = "指定流转模式：0未启用 1指定节点和操作者 2仅指定节点 3多目标并行")
    private Integer appointFlowMode;

    @Schema(description = "预置节点池容量（=单实例自由步骤上限）")
    private Integer poolSize;

    @Schema(description = "候选节点（已按权限过滤并排序）")
    private List<NextNodeCandidateVO> nodes;

}
