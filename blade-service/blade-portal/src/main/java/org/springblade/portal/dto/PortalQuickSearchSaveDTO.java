package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 快捷搜索类型保存入参（新增/编辑）
 * 对标泛微 quick_search_types §14.2 H
 */
@Data
@Schema(name = "快捷搜索类型入参", description = "新增/编辑快捷搜索类型")
public class PortalQuickSearchSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "主键ID（编辑时必填）")
	private Long id;

	@Schema(name = "所属模块（如 workflow/doc/notice）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String module;

	@Schema(name = "搜索类型标识（模块内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
	private String type;

	@Schema(name = "搜索类型显示名", requiredMode = Schema.RequiredMode.REQUIRED)
	private String customName;

	@Schema(name = "图标（antd 图标名或图片URL）")
	private String icon;

	@Schema(name = "搜索跳转地址")
	private String url;

	@Schema(name = "链接模式（1站内路由 2外部链接）")
	private Integer linkMode;

	@Schema(name = "是否启用（0停用 1启用）")
	private Integer isUse;

	@Schema(name = "排序号（越小越靠前）")
	private Integer orderNum;

	@Schema(name = "备注")
	private String remark;
}
