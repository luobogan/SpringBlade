package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程模拟运行结果（带模拟表单数据走查节点 / 网关条件）。
 *
 * <p>设计期校验：从开始节点出发，按出口条件（结合模拟表单数据）选择分支，
 * 逐节点校验其配置（操作者 / 设置等）是否足以正常流转，回写每条节点的
 * 测试状态；全部通过（{@link #allPassed}=true）后方可生成出口条件等属性。</p>
 */
@Data
@Schema(description = "流程模拟运行结果")
public class SimulateResultVO {

    @Schema(description = "是否全部节点测试通过")
    private boolean allPassed;

    @Schema(description = "走查出的流转路径（按经过顺序）：每段含源/目标节点与所走出口条件")
    private List<PathStep> path;

    @Schema(description = "逐节点校验结果（含未走到但参与校验的节点）")
    private List<NodeSimResult> nodes;

    @Schema(description = "可读摘要")
    private String summary;

    @Schema(description = "可读流转时间线（带时间戳的叙述式走查日志，逐事件一行）：到达节点/节点前附加操作/开始自动测试/操作者提交/通过节点/执行出口/生成流程编号…")
    private List<String> logLines;

    @Data
    @Schema(description = "流转路径中的一段")
    public static class PathStep {
        @Schema(description = "源节点Key")
        private String fromNodeKey;
        @Schema(description = "目标节点Key")
        private String toNodeKey;
        @Schema(description = "所走出口条件中文（无则为空）")
        private String conditionCn;
    }

    @Data
    @Schema(description = "单个节点的模拟校验结果")
    public static class NodeSimResult {
        @Schema(description = "节点Key")
        private String nodeKey;
        @Schema(description = "节点名称")
        private String nodeName;
        @Schema(description = "节点类型 0创建 1审批 2提交 3归档 5等待 6自动处理")
        private Integer nodeType;
        @Schema(description = "测试状态 0未测试 1通过 2未通过")
        private Integer status;
        @Schema(description = "校验说明（未通过时指出原因）")
        private String message;
    }
}
