package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 范文（公文模板示例）。
 */
@Data
@Schema(description = "范文")
public class OdocSampleVO {

	@Schema(description = "文种代码")
	private String docType;

	@Schema(description = "文种名")
	private String docTypeName;

	@Schema(description = "示例标题")
	private String title;

	@Schema(description = "示例正文（含占位符）")
	private String body;
}
