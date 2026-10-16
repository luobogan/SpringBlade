package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 登录前门户皮肤视图对象（config 已解析为对象，供登录页渲染）
 */
@Data
@Schema(name = "登录前门户皮肤", description = "登录页皮肤（config 已解析）")
public class PortalLoginTemplateVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "模板ID")
	private Long id;

	@Schema(name = "模板名称")
	private String templateName;

	@Schema(name = "模板类型标识")
	private String templateType;

	@Schema(name = "背景配置")
	private Map<String, Object> backgroundConfig;

	@Schema(name = "Logo配置")
	private Map<String, Object> logoConfig;

	@Schema(name = "登录框配置")
	private Map<String, Object> formConfig;

	@Schema(name = "二维码配置")
	private Map<String, Object> qrcodeConfig;

	@Schema(name = "版权配置")
	private Map<String, Object> copyrightConfig;

	@Schema(name = "是否当前启用")
	private Integer isCurrent;
}
