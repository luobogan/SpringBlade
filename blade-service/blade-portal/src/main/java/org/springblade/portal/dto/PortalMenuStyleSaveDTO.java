package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 菜单样式保存入参（新增/编辑）
 * 对标泛微 hpMenuStyle menuh/menuv §14.2 E
 */
@Data
@Schema(name = "菜单样式入参", description = "新增/编辑菜单样式（menuh/menuv）")
public class PortalMenuStyleSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "样式名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String styleName;

	@Schema(name = "样式描述")
	private String styleDesc;

	@Schema(name = "样式类型（menuh横向 menuv纵向）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String styleType;

	@Schema(name = "样式配置对象（配色/字号/图标位置等）")
	private Map<String, Object> styleConfig;

	@Schema(name = "备注")
	private String remark;
}
