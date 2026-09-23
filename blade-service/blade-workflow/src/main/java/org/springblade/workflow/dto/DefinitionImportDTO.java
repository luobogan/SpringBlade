package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 从 BPMN 2.0 XML 导入并自动配置流程定义。
 *
 * <p>请求体含一份符合 Flowable 规范的 BPMN XML，其中平台专属配置（节点操作者、操作菜单、
 * 字段权限）以 {@code wf:} 命名空间写在 {@code extensionElements} 内。后端先落库 BPMN 并按
 * 节点/出口 upsert，再解析 {@code wf:} 扩展自动写入 {@code wf_node_operator} /
 * {@code wf_custom_operation}(+action/+right) / {@code wf_node_field_perm} 三张表。</p>
 */
@Data
@Schema(description = "BPMN XML 导入请求（含 wf: 扩展自动配置）")
public class DefinitionImportDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "流程名称（为空时回退为 BPMN process id）")
	private String name;

	@Schema(description = "关联表单ID（workflow_bill.id），可空")
	private Long formId;

	@Schema(description = "路径类型（字典 code），可空")
	private String type;

	@NotBlank(message = "BPMN 内容不能为空")
	@Schema(description = "含 wf: 扩展的 BPMN 2.0 XML（base64 或原文均可）")
	private String bpmnXml;

}
