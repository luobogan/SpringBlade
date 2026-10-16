package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户页面保存入参
 */
@Data
@Schema(name = "门户页面保存入参", description = "门户页面新增/编辑入参")
public class PortalPageSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "页面ID（为空为新增）")
	private Long pageId;

	@Schema(name = "页面编码（唯一）")
	private String pageCode;

	@Schema(name = "页面名称")
	private String pageName;

	@Schema(name = "是否默认门户（0否 1是）")
	private Integer isDefault;

	@Schema(name = "页面类型：0登录前门户 1登录后门户")
	private Integer pageType;

	@Schema(name = "显示顺序")
	private Integer sort;

	@Schema(name = "主题标识")
	private String theme;

	@Schema(name = "备注")
	private String remark;
}