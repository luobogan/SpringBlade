package org.springblade.workflow.odoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 内部交换发送入参。
 */
@Data
@Schema(description = "内部交换发送入参")
public class OdocExchangeSendDTO {

	@Schema(description = "流程实例ID")
	private Long instanceId;

	@Schema(description = "目标机构/单位列表")
	private List<String> targetUnits;

	@Schema(description = "备注")
	private String remark;

}
