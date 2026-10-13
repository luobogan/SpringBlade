package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智能排版（L1 规则）单条版式规则（GB/T 9704 要素展平）。
 */
@Data
@Schema(description = "版式规则")
public class OdocLayoutRuleVO {

	@Schema(description = "要素类型代码")
	private String elementType;

	@Schema(description = "要素中文名")
	private String label;

	@Schema(description = "字体族")
	private String fontFamily;

	@Schema(description = "字号(pt)")
	private Double fontSizePt;

	@Schema(description = "对齐")
	private String align;

	@Schema(description = "X(mm)")
	private Double xMm;

	@Schema(description = "Y(mm)")
	private Double yMm;

	@Schema(description = "宽(mm)")
	private Double widthMm;

	@Schema(description = "高(mm)")
	private Double heightMm;

	@Schema(description = "是否必出要素")
	private Boolean required;

	@Schema(description = "数据源键（表单字段/meta_key）")
	private String dataKey;
}
