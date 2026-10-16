package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 主题视图（当前生效主题）
 * <p>解析优先级：用户偏好 &gt; 页面 theme_key &gt; 租户默认主题</p>
 */
@Data
@Schema(name = "生效主题", description = "当前生效主题的标识与配置")
public class ThemeVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主题标识")
	private String themeKey;

	@Schema(name = "主题名称")
	private String themeName;

	@Schema(name = "主题配置（{primaryColor,bgColor,logoUrl,...}）")
	private Map<String, Object> themeConfig;
}
