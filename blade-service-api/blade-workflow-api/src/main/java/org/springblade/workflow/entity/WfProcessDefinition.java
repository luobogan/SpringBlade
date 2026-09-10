package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程定义实体
 *
 * <p>对齐 ecology {@code workflow_base}；status：0草稿 1已发布 2停用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_definition")
@Schema(description = "流程定义")
public class WfProcessDefinition extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎流程Key（= BPMN process id）")
    private String procKey;

    @Schema(description = "关联 workflow_bill.id（表单）")
    private Long formId;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）")
    private String bpmnXml;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否自由流程")
    private Integer isFree;

    @Schema(description = "自由流程类型：1简易 2高级（对齐 ecology newFreeWfType）")
    private Integer freeWfType;

    @Schema(description = "路径类型（对齐 ecology path_type 字典 code）")
    private String type;

    @Schema(description = "对应表单类型：0自定义表单 1系统表单")
    private Integer formType;

    @Schema(description = "路径描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

}
