package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程出口（连线）实体
 *
 * <p>对齐 ecology {@code workflow_nodelink}；
 * is_reject=1 表示退回线，is_must_pass=1 表示分叉必经分支。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_link")
@Schema(description = "流程出口（连线）")
public class WfNodeLink extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "源节点")
    private String fromNodeKey;

    @Schema(description = "目标节点")
    private String toNodeKey;

    @Schema(description = "是否退回线")
    private Integer isReject;

    @Schema(description = "分叉必经分支")
    private Integer isMustPass;

    @Schema(description = "条件表达式")
    private String conditionExpr;

    @Schema(description = "条件中文描述")
    private String conditionCn;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否经由网关折叠而来的逻辑连线 1=是（A→网关→B 折叠为 A→B，条件取自网关出口分支）")
    private Integer viaGateway;

}
