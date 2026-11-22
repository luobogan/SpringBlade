/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.TopMenu;
import org.springblade.system.service.IMenuService;
import org.springblade.system.service.ITopMenuService;
import org.springblade.system.vo.CheckedTreeVO;
import org.springblade.system.vo.GrantTreeVO;
import org.springblade.system.vo.MenuVO;
import org.springblade.system.wrapper.MenuWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 控制器
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/menu")
@ApiOrder
@Tag(name = "菜单", description = "菜单")
public class MenuController extends BladeController {

	private final IMenuService menuService;

	private final ITopMenuService topMenuService;

	private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "详情", description = "传入menu")
	public R<MenuVO> detail(Menu menu) {
		Menu detail = menuService.getOne(Condition.getQueryWrapper(menu));
		return R.data(MenuWrapper.build().entityVO(detail));
	}

	/**
	 * 列表
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "code", description = "菜单编号", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "菜单名称", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "列表", description = "传入menu")
	public R<List<MenuVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * 菜单列表
	 */
	@GetMapping("/menu-list")
	@Parameters({
		@Parameter(name = "code", description = "菜单编号", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "菜单名称", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "菜单列表", description = "传入menu")
	public R<List<MenuVO>> menuList(@Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().eq(Menu::getCategory, 1).orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * 懒加载菜单列表
	 */
	@GetMapping("/lazy-menu-list")
	@Parameters({
		@Parameter(name = "code", description = "菜单编号", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "name", description = "菜单名称", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "懒加载菜单列表", description = "传入menu")
	public R<List<MenuVO>> lazyMenuList(Long parentId, @Parameter(hidden = true) @RequestParam Map<String, Object> menu) {
		List<MenuVO> list = menuService.lazyMenuList(parentId, menu);
		return R.data(MenuWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "新增或修改", description = "传入menu")
	public R submit(@Valid @RequestBody Menu menu) {
		return R.status(menuService.saveOrUpdate(menu));
	}


	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(menuService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * 前端菜单数据
	 */
	@GetMapping("/routes")
	@Operation(summary = "前端菜单数据", description = "前端菜单数据")
	public R<List<MenuVO>> routes(BladeUser user, Long topMenuId) {
		List<MenuVO> list = menuService.routes((user == null || user.getUserId() == 0L) ? null : user.getRoleId(), topMenuId);
		return R.data(list);
	}

	/**
	 * 前端按钮数据
	 */
	@GetMapping("/buttons")
	@Operation(summary = "前端按钮数据", description = "前端按钮数据")
	public R<List<MenuVO>> buttons(BladeUser user) {
		List<MenuVO> list = menuService.buttons(user.getRoleId());
		return R.data(list);
	}

	/**
	 * 获取菜单树形结构
	 */
	@GetMapping("/tree")
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<MenuVO>> tree() {
		List<MenuVO> tree = menuService.tree();
		return R.data(tree);
	}

	/**
	 * 获取权限分配树形结构
	 */
	@GetMapping("/grant-tree")
	@Operation(summary = "权限分配树形结构", description = "权限分配树形结构")
	public R<GrantTreeVO> grantTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.grantTree(user));
		vo.setDataScope(menuService.grantDataScopeTree(user));
		vo.setApiScope(menuService.grantApiScopeTree(user));
		return R.data(vo);
	}

	/**
	 * 获取权限分配树形结构
	 */
	@GetMapping("/role-tree-keys")
	@Operation(summary = "角色所分配的树", description = "角色所分配的树")
	public R<CheckedTreeVO> roleTreeKeys(String roleIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		CheckedTreeVO.TreeKeys menuKeys = new CheckedTreeVO.TreeKeys();
		menuKeys.setCheckedKeys(menuService.roleTreeKeys(roleIds));
		vo.setMenu(menuKeys);
		CheckedTreeVO.TreeKeys dataScopeKeys = new CheckedTreeVO.TreeKeys();
		dataScopeKeys.setCheckedKeys(menuService.dataScopeTreeKeys(roleIds));
		vo.setDataScope(dataScopeKeys);
		CheckedTreeVO.TreeKeys apiScopeKeys = new CheckedTreeVO.TreeKeys();
		apiScopeKeys.setCheckedKeys(menuService.apiScopeTreeKeys(roleIds));
		vo.setApiScope(apiScopeKeys);
		return R.data(vo);
	}

	/**
	 * 获取配置的角色权限
	 */
	@GetMapping("auth-routes")
	@Operation(summary = "菜单的角色权限")
	public R<List<Kv>> authRoutes(BladeUser user) {
		if (Func.isEmpty(user) || user.getUserId() == 0L) {
			return null;
		}
		return R.data(menuService.authRoutes(user));
	}

	/**
	 * 顶部菜单数据
	 */
	@GetMapping("/top-menu")
	@Operation(summary = "顶部菜单数据", description = "顶部菜单数据")
	public R<List<TopMenu>> topMenu(BladeUser user) {
		if (Func.isEmpty(user)) {
			return null;
		}
		List<TopMenu> list = topMenuService.list(Wrappers.<TopMenu>query().lambda().orderByAsc(TopMenu::getSort));
		return R.data(list);
	}

	/**
	 * 获取顶部菜单树形结构
	 */
	@GetMapping("/grant-top-tree")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "顶部菜单树形结构", description = "顶部菜单树形结构")
	public R<GrantTreeVO> grantTopTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.grantTopTree(user));
		return R.data(vo);
	}

	/**
	 * 获取顶部菜单树形结构
	 */
	@GetMapping("/top-tree-keys")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "顶部菜单所分配的树", description = "顶部菜单所分配的树")
	public R<CheckedTreeVO> topTreeKeys(String topMenuIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		CheckedTreeVO.TreeKeys menuKeys = new CheckedTreeVO.TreeKeys();
		menuKeys.setCheckedKeys(menuService.topTreeKeys(topMenuIds));
		vo.setMenu(menuKeys);
		return R.data(vo);
	}

	/**
	 * 查询角色级菜单显示名配置（管理员）
	 */
	@GetMapping("/role-name/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "角色菜单显示名列表", description = "按角色+菜单查询自定义显示名")
	public R<List<Map<String, Object>>> roleNameList(@RequestParam(required = false) Long roleId,
	                                                 @RequestParam(required = false) Long menuId) {
		StringBuilder sql = new StringBuilder("SELECT id, role_id, menu_id, name_override, tenant_id FROM blade_role_menu_name WHERE 1=1");
		List<Object> args = new java.util.ArrayList<>();
		if (roleId != null) { sql.append(" AND role_id=?"); args.add(roleId); }
		if (menuId != null) { sql.append(" AND menu_id=?"); args.add(menuId); }
		return R.data(jdbcTemplate.queryForList(sql.toString(), args.toArray()));
	}

	/**
	 * 保存角色级菜单显示名（管理员）：同角色+菜单幂等覆盖，name 为空则删除配置。
	 */
	@PostMapping("/role-name/save")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "保存角色菜单显示名", description = "roleId+menuId+name，name 为空则删除")
	public R<Boolean> roleNameSave(@RequestBody Map<String, Object> body) {
		Object roleId = body.get("roleId"), menuId = body.get("menuId"), name = body.get("name");
		// 雪花ID 19 位，JS Number 会丢精度，前端一律传字符串；这里兼容字符串/数值两种形态
		long r, m;
		try {
			r = Long.parseLong(String.valueOf(roleId));
			m = Long.parseLong(String.valueOf(menuId));
		} catch (Exception ex) {
			return R.fail("roleId / menuId 必填且必须为数字");
		}
		String tenantId = org.springblade.core.tenant.TenantUtil.getTenantId();
		String n = name == null ? null : String.valueOf(name).trim();
		if (Func.isBlank(n)) {
			jdbcTemplate.update("DELETE FROM blade_role_menu_name WHERE role_id=? AND menu_id=? AND tenant_id=?", r, m, tenantId);
			return R.data(true);
		}
		Long exists = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM blade_role_menu_name WHERE role_id=? AND menu_id=? AND tenant_id=?", Long.class, r, m, tenantId);
		if (exists != null && exists > 0) {
			jdbcTemplate.update("UPDATE blade_role_menu_name SET name_override=? WHERE role_id=? AND menu_id=? AND tenant_id=?", n, r, m, tenantId);
		} else {
			Long max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id),0) FROM blade_role_menu_name", Long.class);
			jdbcTemplate.update("INSERT INTO blade_role_menu_name (id,role_id,menu_id,name_override,tenant_id,create_time) VALUES (?,?,?,?,?,NOW())",
				(max == null ? 0L : max) + 1, r, m, n, tenantId);
		}
		return R.data(true);
	}

}
