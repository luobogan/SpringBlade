package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户元素保存入参
 */
@Data
@Schema(name = "门户元素保存入参", description = "门户元素新增/编辑入参")
public class PortalElementSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "元素ID（为空为新增）")
	private Long elementId;

	@Schema(name = "所属页面ID")
	private Long pageId;

	@Schema(name = "元素类型：todo/notice/message/chart/links/rich")
	private String elementType;

	@Schema(name = "元素标题")
	private String elementName;

	@Schema(name = "是否显示卡片标题（0否 1是）")
	private Integer showTitle;

	@Schema(name = "所在行号")
	private Integer rowNo;

	@Schema(name = "栅格宽度（24栅格：4/8/12/16/24）")
	private Integer colSpan;

	@Schema(name = "同页排序号")
	private Integer sortOrder;

	@Schema(name = "元素私有配置JSON")
	private String configJson;

	@Schema(name = "元素样式配置JSON")
	private String styleJson;
}