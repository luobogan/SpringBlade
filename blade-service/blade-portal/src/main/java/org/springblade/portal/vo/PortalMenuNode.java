package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 门户菜单树节点（运行时/管理端通用）
 */
@Data
@Schema(name = "门户菜单节点", description = "门户菜单树节点")
public class PortalMenuNode implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "菜单ID")
	private Long id;

	@Schema(name = "父菜单ID（0为顶级）")
	private Long parentId;

	@Schema(name = "菜单名称")
	private String menuName;

	@Schema(name = "图标")
	private String icon;

	@Schema(name = "链接地址")
	private String routeUrl;

	@Schema(name = "菜单类型")
	private String menuType;

	@Schema(name = "链接模式：1站内路由 2外部链接")
	private Integer linkMode;

	@Schema(name = "排序号")
	private Integer sortOrder;

	@Schema(name = "是否可见")
	private Integer visible;

	@Schema(name = "子菜单")
	private List<PortalMenuNode> children = new ArrayList<>();
}
