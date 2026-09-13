package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程版本差异 VO（版本对比）。
 *
 * <p>按 nodeKey / fromNodeKey→toNodeKey 双向对照两个版本的节点与出口，
 * 输出新增 / 删除 / 变更三类差异，供前端版本对比弹窗展示。
 * source = 当前打开的版本，target = 被对比的版本。</p>
 */
@Data
@Schema(description = "流程版本差异")
public class VersionDiffVO {

    @Schema(description = "源版本（当前打开）定义ID")
    private Long sourceDefId;

    @Schema(description = "源版本号")
    private Integer sourceVersion;

    @Schema(description = "目标版本定义ID")
    private Long targetDefId;

    @Schema(description = "目标版本号")
    private Integer targetVersion;

    @Schema(description = "仅在目标版本中存在的节点（新增）")
    private List<NodeDiff> addedNodes = new ArrayList<>();

    @Schema(description = "仅在源版本中存在的节点（删除）")
    private List<NodeDiff> removedNodes = new ArrayList<>();

    @Schema(description = "两版本共有但属性不同的节点（变更）")
    private List<NodeDiff> changedNodes = new ArrayList<>();

    @Schema(description = "仅在目标版本中存在的出口（新增）")
    private List<LinkDiff> addedLinks = new ArrayList<>();

    @Schema(description = "仅在源版本中存在的出口（删除）")
    private List<LinkDiff> removedLinks = new ArrayList<>();

    @Schema(description = "两版本共有但属性不同的出口（变更）")
    private List<LinkDiff> changedLinks = new ArrayList<>();

    /**
     * 节点差异行。
     */
    @Data
    @Schema(description = "节点差异行")
    public static class NodeDiff {

        @Schema(description = "节点Key（BPMN 元素 id）")
        private String nodeKey;

        @Schema(description = "源版本节点名称")
        private String sourceName;

        @Schema(description = "目标版本节点名称")
        private String targetName;

        @Schema(description = "源版本节点类型：0创建 1审批 2提交 3归档 5等待 6自动处理")
        private Integer sourceType;

        @Schema(description = "目标版本节点类型")
        private Integer targetType;

        @Schema(description = "源版本会签关系：0或签 1会签 2依次 3抄送不需提交 4抄送需提交")
        private Integer sourceSignOrder;

        @Schema(description = "目标版本会签关系")
        private Integer targetSignOrder;
    }

    /**
     * 出口差异行。
     */
    @Data
    @Schema(description = "出口差异行")
    public static class LinkDiff {

        @Schema(description = "源节点Key")
        private String fromNodeKey;

        @Schema(description = "目标节点Key")
        private String toNodeKey;

        @Schema(description = "源版本条件中文描述")
        private String sourceConditionCn;

        @Schema(description = "目标版本条件中文描述")
        private String targetConditionCn;

        @Schema(description = "源版本是否退回线")
        private Integer sourceIsReject;

        @Schema(description = "目标版本是否退回线")
        private Integer targetIsReject;
    }
}
