package org.springblade.workflow.odoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文种转换提交入参（对应 E9 submitDocTypeChange）。
 */
@Data
@Schema(description = "文种转换提交入参")
public class OdocDocTypeChangeSubmitDTO {

	@Schema(description = "原流程实例ID")
	private Long instanceId;

	@Schema(description = "目标流程定义ID")
	private Long targetDefId;

	@Schema(description = "变更原因")
	private String reasons;

}
