package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门菜单批量授权入参
 *
 * <p>语义（与 blade_portal_menu_share 的差量写法一致，只动 (2,deptId) 这类记录）：
 * <ul>
 *   <li>{@code menuIds} 内的菜单 → 确保存在「部门 deptId」授权记录</li>
 *   <li>不在 {@code menuIds} 内的同类型菜单 → 移除「部门 deptId」授权记录，
 *       并补写 {@code type=6} 限定标记，保证它不会回落成全员可见</li>
 * </ul>
 *
 * @author blade
 */
@Data
@Schema(description = "部门菜单批量授权入参")
public class PortalMenuDeptShareDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "部门ID（blade_dept.id）", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long deptId;

	@Schema(description = "菜单类型：front前端/top后端", example = "front")
	private String menuType = "front";

	@Schema(description = "该部门可见的菜单ID集合；缺失/空=该部门全部不可见")
	private List<Long> menuIds = new ArrayList<>();

}
