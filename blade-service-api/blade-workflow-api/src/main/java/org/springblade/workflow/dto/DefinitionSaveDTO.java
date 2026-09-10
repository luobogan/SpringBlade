package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;

import java.io.Serializable;
import java.util.List;

/**
 * 流程定义保存请求（定义 + 节点 + 出口 + 操作者 一次性提交）
 *
 * <p>对应 {@code POST /definition} 与 {@code PUT /definition/{id}}。
 * 节点 / 出口 / 操作者按「先删后插」整体覆盖，保证与设计器画布结果一致。</p>
 */
@Data
@Schema(description = "流程定义保存请求")
public class DefinitionSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义")
    private WfProcessDefinition definition;

    @Schema(description = "节点列表")
    private List<WfProcessNode> nodes;

    @Schema(description = "出口（连线）列表")
    private List<WfNodeLink> links;

    @Schema(description = "节点操作者列表")
    private List<WfNodeOperator> operators;

}
