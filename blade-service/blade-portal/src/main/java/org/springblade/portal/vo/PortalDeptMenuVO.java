package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门菜单权限项（「部门 → 菜单」反查授权用）
 *
 * <p>与 {@link PortalMenuNode} 的区别：本 VO 面向「按部门授权」场景，扁平结构
 * （前端自行按 parentId 组装 antd Tree），并带上该部门的授权状态：
 * <ul>
 *   <li>{@code checked} —— 该菜单对当前选中部门是否可见</li>
 *   <li>{@code limited} —— 菜单是否已纳入限定管理（存在任意 share 记录）</li>
 *   <li>{@code shareCount} —— 现有授权记录数（不含限定标记）</li>
 * </ul>
 *
 * @author blade
 */
@Data
@Schema(description = "部门菜单权限项")
public class PortalDeptMenuVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "菜单ID")
	private Long id;

	@Schema(description = "父菜单ID（0=顶级）")
	private Long parentId;

	@Schema(description = "菜单名称")
	private String menuName;

	@Schema(description = "图标")
	private String icon;

	@Schema(description = "链接地址")
	private String routeUrl;

	@Schema(description = "菜单类型：front前端/top后端/toolbar工具栏/quick快捷入口/report报表")
	private String menuType;

	@Schema(description = "排序号")
	private Integer sortOrder;

	@Schema(description = "是否可见（0隐藏 1可见）")
	private Integer visible;

	@Schema(description = "该部门是否可见（true=已授权或全员可见）")
	private Boolean checked;

	@Schema(description = "是否已纳入限定管理（存在授权记录 = 非全员可见）")
	private Boolean limited;

	@Schema(description = "现有授权记录数（不含限定标记）")
	private Integer shareCount;

}
