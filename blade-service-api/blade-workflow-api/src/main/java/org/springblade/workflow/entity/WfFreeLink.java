package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 自由流程实例级步骤链路。
 *
 * <p>供退回 BFS（由本表提供入边，替代静态 {@code wf_node_link}）、分支合并与
 * 「复制定义态出口接到动态链尾部」使用。from/to 均为预置池内静态 activityId。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_free_link")
@Schema(description = "自由流程实例级步骤链路")
public class WfFreeLink extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 合并策略：按比例通过 */
    public static final int MERGE_BY_RATIO = 1;
    /** 合并策略：按分支数 */
    public static final int MERGE_BY_BRANCH_COUNT = 2;
    /** 合并策略：指定通过分支 */
    public static final int MERGE_BY_SPECIFIED = 3;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "源节点（池内静态 key）")
    private String fromNodeKey;

    @Schema(description = "目标节点（池内静态 key）")
    private String toNodeKey;

    @Schema(description = "是否退回线")
    private Integer isReject;

    @Schema(description = "分叉必经分支")
    private Integer isMustPass;

    @Schema(description = "合并策略：1按比例 2按分支数 3指定通过分支")
    private Integer mergeType;

    @Schema(description = "合并阈值 / 指定分支")
    private String mergeValue;

    @Schema(description = "从定义态出口复制的条件（留痕，不参与求值）")
    private String conditionCopy;

    @Schema(description = "排序")
    private Integer sortOrder;

}
