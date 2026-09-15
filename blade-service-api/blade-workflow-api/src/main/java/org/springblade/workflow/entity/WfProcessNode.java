package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程节点实体
 *
 * <p>对齐 ecology {@code workflow_flownode} / {@code workflow_nodebase}。</p>
 * <ul>
 *   <li>node_type：0创建 1审批 2提交 3归档 5等待 6自动处理（NodeType）</li>
 *   <li>sign_order：0或签 1会签 2依次 3抄送不需提交 4抄送需提交（SignOrder）</li>
 *   <li>merge_type：0普通 1分叉起点 2分叉中间 3按分支数合并 4指定分支合并 5比例合并（nodeattribute）</li>
 *   <li>pass_num：合并阈值（分支数或百分比，对应 passnum）</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_node")
@Schema(description = "流程节点")
public class WfProcessNode extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    private Long defId;

    @Schema(description = "引擎节点ID")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型 0创建 1审批 2提交 3归档 5等待 6自动处理")
    private Integer nodeType;

    @Schema(description = "审批方式 0或签 1会签 2依次 3抄送不需提交 4抄送需提交")
    private Integer signOrder;

    @Schema(description = "合并类型 0普通 1分叉起点 2分叉中间 3按分支数合并 4指定分支合并 5比例合并")
    private Integer mergeType;

    @Schema(description = "合并阈值：分支数或百分比")
    private Integer passNum;

    @Schema(description = "允许退回")
    private Integer allowReject;

    @Schema(description = "允许转发/转办")
    private Integer allowForward;

    @Schema(description = "自动批准")
    private Integer autoApprove;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "节点测试状态 0未测试 1测试通过 2测试未通过（模拟运行校验结果，设计期使用）")
    private Integer testStatus;

    @Schema(description = "扩展属性（超时、提醒、签章等）")
    private String extJson;

}
