package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 元素样式保存入参（新增/编辑；styleKind 区分 元素样式/头条样式）
 * 对标泛微 hpMenuStyle(menustyletype='element') + headlinestyle §15.3 B3/B4
 */
@Data
@Schema(name = "元素样式入参", description = "新增/编辑元素样式/头条样式")
public class PortalElementStyleSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "样式分类（element元素样式 headline头条样式）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String styleKind;

	@Schema(name = "样式名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String styleName;

	@Schema(name = "样式描述")
	private String styleDesc;

	@Schema(name = "样式配置对象（卡片/网格/头条布局参数）")
	private Map<String, Object> styleConfig;

	@Schema(name = "备注")
	private String remark;
}
