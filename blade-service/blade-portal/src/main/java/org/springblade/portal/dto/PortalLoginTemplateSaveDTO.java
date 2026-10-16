package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 登录前门户皮肤保存入参
 */
@Data
@Schema(name = "登录前门户皮肤保存入参", description = "登录页皮肤/模板 新增/编辑")
public class PortalLoginTemplateSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "模板ID（为空为新增）")
	private Long templateId;

	@Schema(name = "模板名称")
	private String templateName;

	@Schema(name = "模板类型标识")
	private String templateType;

	@Schema(name = "背景配置 {bgColor,bgImage}")
	private Map<String, Object> backgroundConfig;

	@Schema(name = "Logo配置 {logoUrl,title}")
	private Map<String, Object> logoConfig;

	@Schema(name = "登录框配置")
	private Map<String, Object> formConfig;

	@Schema(name = "二维码配置")
	private Map<String, Object> qrcodeConfig;

	@Schema(name = "版权配置")
	private Map<String, Object> copyrightConfig;

	@Schema(name = "是否开启定时切换（0否 1是）")
	private Integer openTimer;

	@Schema(name = "定时开始 yyyy-MM-dd HH:mm:ss")
	private String timerStart;

	@Schema(name = "定时结束 yyyy-MM-dd HH:mm:ss")
	private String timerEnd;
}
