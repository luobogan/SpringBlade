package org.springblade.workflow.odoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发起督办入参。
 */
@Data
@Schema(description = "发起督办入参")
public class OdocSuperviseSaveDTO {

	@Schema(description = "流程实例ID")
	private Long instanceId;

	@Schema(description = "督办人ID（为空=当前登录人）")
	private Long supervisorId;

	@Schema(description = "督办期限")
	private LocalDateTime deadline;

	@Schema(description = "督办说明")
	private String remark;

}
