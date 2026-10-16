package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 元素模板保存入参（另存为模板）
 * 对标泛微 HPELEMENTTEMPLATE §15.3 B2
 */
@Data
@Schema(name = "元素模板入参", description = "新增/编辑元素模板（另存为模板）")
public class PortalElementTemplateSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "元素类型（todo/notice/message/chart/links/rich）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String elementType;

	@Schema(name = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String templateName;

	@Schema(name = "元素私有配置对象（模板快照）")
	private Map<String, Object> config;

	@Schema(name = "元素样式配置对象（模板快照）")
	private Map<String, Object> style;

	@Schema(name = "备注")
	private String remark;
}
