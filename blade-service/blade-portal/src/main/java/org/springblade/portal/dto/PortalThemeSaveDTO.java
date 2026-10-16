package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 门户主题保存入参（新增/编辑）
 */
@Data
@Schema(name = "门户主题保存入参", description = "新增/编辑主题")
public class PortalThemeSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主题ID（编辑时必填）")
	private Long themeId;

	@Schema(name = "主题名称")
	private String themeName;

	@Schema(name = "主题标识（租户内唯一）")
	private String themeKey;

	@Schema(name = "主题配置对象（{primaryColor,bgColor,logoUrl,...}）")
	private Map<String, Object> themeConfig;

	@Schema(name = "是否开启定时切换（0否 1是）")
	private Integer openTimer;

	@Schema(name = "定时开始 yyyy-MM-dd HH:mm:ss")
	private String timerStart;

	@Schema(name = "定时结束 yyyy-MM-dd HH:mm:ss")
	private String timerEnd;
}
