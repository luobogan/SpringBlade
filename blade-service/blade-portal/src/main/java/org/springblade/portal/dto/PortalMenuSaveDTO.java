package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户菜单保存入参
 */
@Data
@Schema(name = "门户菜单保存入参", description = "新增/编辑门户菜单")
public class PortalMenuSaveDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "菜单ID（为空视为新增）")
	private Long menuId;

	@Schema(name = "父菜单ID（0为顶级）")
	private Long parentId;

	@Schema(name = "菜单名称")
	private String menuName;

	@Schema(name = "图标")
	private String icon;

	@Schema(name = "链接地址")
	private String routeUrl;

	@Schema(name = "菜单类型：top/toolbar/quick")
	private String menuType;

	@Schema(name = "链接模式：1站内路由 2外部链接")
	private Integer linkMode;

	@Schema(name = "排序号")
	private Integer sortOrder;

	@Schema(name = "是否可见（0隐藏 1可见）")
	private Integer visible;

	@Schema(name = "扩展配置JSON")
	private String configJson;
}
