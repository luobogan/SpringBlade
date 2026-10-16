package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户页面简要信息（供「我的门户」返回可见页面列表，做多门户切换）
 */
@Data
@Schema(name = "门户页面简要信息", description = "可见门户页面（切换用）")
public class PortalPageBrief implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "页面ID")
	private Long id;

	@Schema(name = "页面编码")
	private String pageCode;

	@Schema(name = "页面名称")
	private String pageName;

	@Schema(name = "是否默认门户（0否 1是）")
	private Integer isDefault;
}
