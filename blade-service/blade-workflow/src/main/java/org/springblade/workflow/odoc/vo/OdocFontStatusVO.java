package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 国标字体可用性（信创适配自检）。
 */
@Data
@Schema(description = "国标字体可用性")
public class OdocFontStatusVO {

	@Schema(description = "字体族名")
	private String font;

	@Schema(description = "公文用途")
	private String usage;

	@Schema(description = "是否必需（标题/正文必需，缺则版式不符国标）")
	private Boolean required;

	@Schema(description = "本机是否已安装")
	private Boolean available;

	@Schema(description = "提示")
	private String note;
}
