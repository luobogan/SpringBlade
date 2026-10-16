package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 布局模板保存入参（把某页面元素排列存为模板，对标 portallayout）
 */
@Data
@Schema(name = "布局模板保存入参", description = "把页面元素排列存为可复用模板")
public class PortalLayoutTemplateSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "来源页面ID")
	private Long pageId;

	@Schema(name = "模板名称")
	private String layoutName;

	@Schema(name = "模板类型：sys内置 cus自定义")
	private String layoutType;

	@Schema(name = "备注")
	private String remark;
}
