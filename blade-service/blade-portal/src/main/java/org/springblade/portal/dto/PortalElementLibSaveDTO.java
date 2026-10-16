package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 元素库保存入参
 */
@Data
@Schema(name = "元素库保存入参", description = "独立元素库 新增/编辑")
public class PortalElementLibSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "库元素ID（为空为新增）")
	private Long libId;

	@Schema(name = "元素类型")
	private String elementType;

	@Schema(name = "元素名称")
	private String elementName;

	@Schema(name = "元素私有配置JSON")
	private String configJson;

	@Schema(name = "元素样式配置JSON")
	private String styleJson;

	@Schema(name = "备注")
	private String remark;
}
