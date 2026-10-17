package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 特色门户布局保存入参（对标泛微 hp_grid_layout §16.2）
 */
@Data
@Schema(name = "特色门户布局入参", description = "门户网格布局保存入参")
public class PortalGridLayoutSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "布局ID（为空视为新增）")
	private Long id;

	@Schema(name = "布局类型：login登录前门户/theme登录后门户", requiredMode = Schema.RequiredMode.REQUIRED)
	private String layoutType;

	@Schema(name = "布局名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String layoutName;

	@Schema(name = "布局标题（展示用）")
	private String layoutTitle;

	@Schema(name = "网格布局配置JSON（五区元素清单）")
	private String layoutConfig;

	@Schema(name = "背景图/背景色配置")
	private String layoutBackground;

	@Schema(name = "是否启用（0否 1是）")
	private Integer layoutUse;

	@Schema(name = "是否默认入口（同类型内唯一）")
	private Integer layoutDefaultEntrance;

	@Schema(name = "备注")
	private String remark;
}
