package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 新闻模板保存入参（新增/编辑）
 * 对标泛微 PAGENEWSTEMPLATE §15.4 C1
 */
@Data
@Schema(name = "门户新闻模板入参", description = "新增/编辑新闻模板（inner/outer）")
public class PortalNewsTemplateSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
	private String templateName;

	@Schema(name = "模板类型（inner内部新闻 outer外部新闻）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String templateType;

	@Schema(name = "版式HTML（{title}/{summary}/{time} 占位符替换）")
	private String layoutHtml;

	@Schema(name = "是否默认模板（同类型内唯一）")
	private Integer isDefault;

	@Schema(name = "备注")
	private String remark;
}
