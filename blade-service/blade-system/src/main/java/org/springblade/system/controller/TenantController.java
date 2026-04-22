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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.PackageMenu;
import org.springblade.system.entity.Role;
import org.springblade.system.entity.RoleMenu;
import org.springblade.system.entity.Tenant;
import org.springblade.system.entity.TenantPackage;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.mapper.RoleMapper;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IMenuService;
import org.springblade.system.service.IPackageMenuService;
import org.springblade.system.service.IRoleMenuService;
import org.springblade.system.service.ITenantMenuService;
import org.springblade.system.service.ITenantPackageService;
import org.springblade.system.service.ITenantService;
import org.springblade.system.service.IUserService;
import org.springblade.system.user.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import static org.springblade.core.cache.utils.CacheUtil.SYS_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/tenant")
@Hidden
@Tag(name = "租户管理", description = "接口")
public class TenantController extends BladeController {

	private ITenantService tenantService;
	private IMenuService menuService;
	private MenuMapper menuMapper;
	private RoleMapper roleMapper;
	private UserMapper userMapper;
	private IRoleMenuService roleMenuService;
	private ITenantPackageService tenantPackageService;
	private IPackageMenuService packageMenuService;
	private IUserService userService;
	private ITenantMenuService tenantMenuService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@Operation(summary = "详情", description = "传入tenant")
	public R<Tenant> detail(Tenant tenant) {
		Tenant detail = tenantService.getOne(Condition.getQueryWrapper(tenant));
		return R.data(detail);
	}

	/**
	 * 分页
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "tenantId", description = "参数名称", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "tenantName", description = "角色别名", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "contactNumber", description = "联系电话", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@Operation(summary = "分页", description = "传入tenant")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<IPage<Tenant>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> tenant, Query query, BladeUser bladeUser) {
		QueryWrapper<Tenant> queryWrapper = Condition.getQueryWrapper(tenant, Tenant.class);
		IPage<Tenant> pages = tenantService.page(Condition.getPage(query), (!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(Tenant::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(pages);
	}

	/**
	 * 下拉数据源
	 */
	@GetMapping("/select")
	@Operation(summary = "下拉数据源", description = "传入tenant")
	public R<List<Tenant>> select(Tenant tenant, BladeUser bladeUser) {
		QueryWrapper<Tenant> queryWrapper = Condition.getQueryWrapper(tenant);
		List<Tenant> list = tenantService.list((!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(Tenant::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(list);
	}

	/**
	 * 自定义分页
	 */
	@GetMapping("/page")
	@Operation(summary = "分页", description = "传入tenant")
	public R<IPage<Tenant>> page(Tenant tenant, Query query) {
		IPage<Tenant> pages = tenantService.selectTenantPage(Condition.getPage(query), tenant);
		return R.data(pages);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@Operation(summary = "新增或修改", description = "传入tenant")
	public R submit(@Valid @RequestBody Tenant tenant) {
		return R.status(tenantService.saveTenant(tenant));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(tenantService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 根据域名查询信息
	 *
	 * @param domain 域名
	 */
	@GetMapping("/info")
	@Operation(summary = "配置信息", description = "传入domain")
	public R<Kv> info(String domain) {
		Tenant tenant = tenantService.getOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getDomain, domain));
		Kv kv = Kv.init();
		if (tenant != null) {
			kv.set("tenantId", tenant.getTenantId()).set("domain", tenant.getDomain());
		}
		return R.data(kv);
	}

	/**
	 * 检查当前用户是否为超级管理员
	 */
	@GetMapping("/is-super-admin")
	@Operation(summary = "检查超级管理员", description = "检查当前用户是否为超级管理员")
	public R<Boolean> isSuperAdmin(BladeUser user) {
		if (user == null || Func.isEmpty(user.getTenantId())) {
			return R.data(false);
		}
		return R.data(BladeConstant.ADMIN_TENANT_ID.equals(user.getTenantId()));
	}

	/**
	 * 获取产品包列表（超级管理员专用）
	 */
	@GetMapping("/package-select")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "产品包下拉", description = "获取产品包列表（仅超级管理员可访问）")
	public R<List<TenantPackage>> packageSelect() {
		List<TenantPackage> packages = tenantPackageService.list();
		return R.data(packages);
	}

	/**
	 * 为租户分配产品包（超级管理员专用）
	 * 增强版：事务保护 + 分类统计(菜单/按钮) + 自动修复用户roleId
	 */
	@PostMapping("/assign-package")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Transactional(rollbackFor = Exception.class)
	@Operation(summary = "分配产品包", description = "为指定租户分配产品包（仅超级管理员可访问）")
	public R<Kv> assignPackage(@RequestParam String tenantId, @RequestParam Long packageId) {
		Tenant tenant = TenantUtil.ignore(() ->
			tenantService.getOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId))
		);

		if (tenant == null) {
			return R.fail("租户不存在");
		}

		TenantPackage tenantPackage = tenantPackageService.getById(packageId);
		if (tenantPackage == null) {
			return R.fail("产品包不存在");
		}

		List<Long> allMenuIds = tenantPackageService.getMenuIdsByPackageId(packageId);
		if (allMenuIds.isEmpty()) {
			return R.fail("产品包中没有配置任何菜单权限，请先在产品包管理中配置菜单");
		}

		List<Role> roles = TenantUtil.ignore(() ->
			roleMapper.selectList(Wrappers.<Role>query().lambda().eq(Role::getTenantId, tenantId))
		);

		if (roles.isEmpty()) {
			return R.fail("租户没有角色，请先为该租户创建角色后再分配产品包");
		}

		// 为租户复制菜单结构
		int copiedMenuCount = tenantMenuService.copyMenusForTenant(tenantId, packageId, allMenuIds);
		if (copiedMenuCount == 0) {
			return R.fail("菜单复制失败，请检查超级管理员的菜单配置");
		}

		// 获取复制后的菜单ID列表
		List<Long> tenantMenuIds = tenantMenuService.getTenantMenuIds(tenantId, allMenuIds);

		int menuCount = 0;
		int buttonCount = 0;
		if (!tenantMenuIds.isEmpty()) {
			List<Menu> menusInPackage = TenantUtil.ignore(() ->
				menuMapper.selectBatchIds(tenantMenuIds)
			);
			menuCount = (int) menusInPackage.stream().filter(m -> m.getCategory() != null && m.getCategory() == 1).count();
			buttonCount = (int) menusInPackage.stream().filter(m -> m.getCategory() != null && m.getCategory() == 2).count();
		}

		// 为租户所有角色复制权限
		List<Long> allRoleIds = roles.stream()
			.map(Role::getId)
			.collect(Collectors.toList());
		tenantMenuService.copyRoleMenusForTenant(tenantId, allRoleIds, tenantMenuIds);

		int fixedUserCount = 0;
		if (!roles.isEmpty()) {
			Long firstRoleId = roles.get(0).getId();
			List<User> usersNeedFix = TenantUtil.ignore(() ->
				userMapper.selectList(Wrappers.<User>query()
					.lambda().eq(User::getTenantId, tenantId)
					.eq(User::getIsDeleted, 0)
					.and(w -> w.isNull(User::getRoleId).or().eq(User::getRoleId, ""))
				)
			);

			if (!usersNeedFix.isEmpty()) {
				for (User user : usersNeedFix) {
					user.setRoleId(Func.toStr(firstRoleId));
				}
				TenantUtil.ignore(() -> {
					for (User user : usersNeedFix) {
						userService.updateById(user);
					}
				});
				fixedUserCount = usersNeedFix.size();
			}
		}

		tenant.setPackageId(packageId);
		TenantUtil.ignore(() -> tenantService.updateById(tenant));

		CacheUtil.clear(SYS_CACHE);

		Kv resultData = Kv.init()
			.set("menuCount", menuCount)
			.set("buttonCount", buttonCount)
			.set("roleCount", roles.size())
			.set("fixedUserCount", fixedUserCount)
			.set("packageName", tenantPackage.getPackageName());

		StringBuilder msg = new StringBuilder("产品包【" + tenantPackage.getPackageName() + "】分配成功");
		msg.append("，菜单 ").append(menuCount).append(" 个").append("，按钮 ").append(buttonCount).append(" 个");
		msg.append("，关联角色 ").append(roles.size()).append(" 个");
		if (fixedUserCount > 0) {
			msg.append("，自动修复 ").append(fixedUserCount).append(" 个用户的roleId");
		}

		resultData.set("message", msg.toString());
		return R.data(resultData);
	}

	/**
	 * 诊断租户菜单不显示问题（超级管理员专用）
	 */
	@GetMapping("/diagnose")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "诊断租户菜单", description = "诊断指定租户的菜单配置是否正确（仅超级管理员可访问）")
	public R<Kv> diagnose(@RequestParam String tenantId) {
		Kv result = Kv.create();
		List<String> issues = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		result.set("tenantId", tenantId);

		Tenant tenant = TenantUtil.ignore(() ->
			tenantService.getOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId))
		);
		if (tenant == null) {
			issues.add("❌ 租户不存在: tenantId=" + tenantId);
			result.set("issues", issues);
			return R.data(result);
		}
		result.set("tenant", Kv.init().set("id", tenant.getId()).set("tenantName", tenant.getTenantName()).set("packageId", tenant.getPackageId()));

		if (tenant.getPackageId() == null) {
			issues.add("❌ 租户未分配产品包 (packageId 为空)");
		} else {
			TenantPackage tenantPackage = tenantPackageService.getById(tenant.getPackageId());
			if (tenantPackage == null) {
				issues.add("❌ 产品包不存在: packageId=" + tenant.getPackageId());
			} else {
				result.set("packageInfo", Kv.init()
					.set("id", tenantPackage.getId())
					.set("packageName", tenantPackage.getPackageName())
					.set("packageCode", tenantPackage.getPackageCode())
					.set("status", tenantPackage.getStatus()));

				if (tenantPackage.getStatus() != null && tenantPackage.getStatus() != 1) {
					warnings.add("⚠️ 产品包状态非正常: status=" + tenantPackage.getStatus());
				}

				List<PackageMenu> packageMenus = TenantUtil.ignore(() ->
					packageMenuService.list(Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, tenant.getPackageId()))
				);
				result.set("packageMenuCount", packageMenus.size());
				if (packageMenus.isEmpty()) {
					issues.add("❌ 产品包中没有配置任何菜单 (blade_package_menu 表为空)");
				} else {
					result.set("packageMenuIds", packageMenus.stream().map(PackageMenu::getMenuId).collect(Collectors.toList()));
				}
			}
		}

		List<Role> roles = TenantUtil.ignore(() ->
			roleMapper.selectList(Wrappers.<Role>query().lambda().eq(Role::getTenantId, tenantId))
		);
		result.set("roleCount", roles.size());
		if (roles.isEmpty()) {
			issues.add("❌ 租户下没有任何角色 (blade_role 表为空，产品包分配也会失败)");
		} else {
			List<Kv> roleList = new ArrayList<>();
			for (Role role : roles) {
				Kv roleInfo = Kv.init()
					.set("id", role.getId())
					.set("roleName", role.getRoleName())
					.set("roleAlias", role.getRoleAlias())
					.set("status", role.getStatus());

				List<RoleMenu> roleMenus = TenantUtil.ignore(() ->
					roleMenuService.list(Wrappers.<RoleMenu>query().lambda().eq(RoleMenu::getRoleId, role.getId()))
				);
				roleInfo.set("menuCount", roleMenus.size());
				roleInfo.set("menuIds", roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList()));

				if (roleMenus.isEmpty()) {
					roleInfo.set("issue", "❌ 该角色没有分配任何菜单");
				}
				if (role.getStatus() != null && role.getStatus() != 1) {
					roleInfo.set("warning", "⚠️ 角色状态非正常");
				}

				roleList.add(roleInfo);
			}
			result.set("roles", roleList);
		}

		List<User> users = TenantUtil.ignore(() ->
			userMapper.selectList(Wrappers.<User>query().lambda().eq(User::getTenantId, tenantId).eq(User::getIsDeleted, 0))
		);
		result.set("userCount", users.size());
		if (users.isEmpty()) {
			warnings.add("⚠️ 租户下没有任何用户");
		} else {
			List<Kv> userList = new ArrayList<>();
			for (User user : users) {
				Kv userInfo = Kv.init()
					.set("id", user.getId())
					.set("account", user.getAccount())
					.set("name", user.getName())
					.set("roleId", user.getRoleId());

				if (Func.isEmpty(user.getRoleId())) {
					userInfo.set("issue", "❌ 用户的 roleId 为空！这是菜单不显示的最可能原因！");
				} else {
					Long userRoleId = Func.toLong(user.getRoleId());
					boolean roleExists = roles.stream().anyMatch(r -> r.getId().equals(userRoleId));
					if (!roleExists) {
						userInfo.set("issue", "❌ 用户的 roleId=" + user.getRoleId() + " 在租户角色中不存在！");
					} else {
						List<RoleMenu> userRoleMenus = TenantUtil.ignore(() ->
							roleMenuService.list(Wrappers.<RoleMenu>query().lambda().eq(RoleMenu::getRoleId, userRoleId))
						);
						userInfo.set("assignedMenuCount", userRoleMenus.size());
						if (userRoleMenus.isEmpty()) {
							userInfo.set("issue", "❌ 用户所属角色没有分配菜单");
						}
					}
				}

				userList.add(userInfo);
			}
			result.set("users", userList);
		}

		result.set("issues", issues);
		result.set("warnings", warnings);
		result.set("hasError", !issues.isEmpty());

		if (issues.isEmpty()) {
			result.set("conclusion", "✅ 未发现明显问题，建议检查前端缓存或重新登录");
		} else {
			result.set("conclusion", "❌ 发现 " + issues.size() + " 个问题，请根据上述 issue 修复");
		}

		return R.data(result);
	}

	/**
	 * 一键修复租户权限问题（超级管理员专用）
	 * 自动修复：用户roleId为空、角色无菜单权限、重新分配产品包等
	 */
	@PostMapping("/auto-fix")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Transactional(rollbackFor = Exception.class)
	@Operation(summary = "一键修复", description = "自动修复租户的roleId和权限关联问题")
	public R<Kv> autoFix(@RequestParam String tenantId) {
		Kv result = Kv.create();
		List<String> actions = new ArrayList<>();

		result.set("tenantId", tenantId);

		Tenant tenant = TenantUtil.ignore(() ->
			tenantService.getOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId))
		);
		if (tenant == null) {
			return R.fail("租户不存在: " + tenantId);
		}
		result.set("tenantName", tenant.getTenantName());

		List<Role> roles = TenantUtil.ignore(() ->
			roleMapper.selectList(Wrappers.<Role>query().lambda().eq(Role::getTenantId, tenantId))
		);
		result.set("roleCount", roles.size());

		int fixedUserCount = 0;
		if (!roles.isEmpty()) {
			Long firstRoleId = roles.get(0).getId();

			List<User> usersNeedFix = TenantUtil.ignore(() ->
				userMapper.selectList(Wrappers.<User>query()
					.lambda().eq(User::getTenantId, tenantId)
					.eq(User::getIsDeleted, 0)
					.and(w -> w.isNull(User::getRoleId).or().eq(User::getRoleId, "").or().eq(User::getRoleId, "0"))
				)
			);

			if (!usersNeedFix.isEmpty()) {
				for (User user : usersNeedFix) {
					user.setRoleId(Func.toStr(firstRoleId));
				}
				TenantUtil.ignore(() -> {
					for (User user : usersNeedFix) {
						userService.updateById(user);
					}
				});
				fixedUserCount = usersNeedFix.size();
				actions.add("✅ 修复 " + fixedUserCount + " 个用户的 roleId（设置为角色: " + roles.get(0).getRoleName() + "）");
			}

			if (tenant.getPackageId() != null) {
				List<Long> menuIds = tenantPackageService.getMenuIdsByPackageId(tenant.getPackageId());
				if (!menuIds.isEmpty()) {
					int menuCount = 0;
					List<Menu> menusInPackage = TenantUtil.ignore(() -> menuMapper.selectBatchIds(menuIds));
					if (menusInPackage != null) {
						menuCount = (int) menusInPackage.stream().filter(m -> m.getCategory() != null && m.getCategory() == 1).count();
					}

					int reassignedRoleCount = 0;
					for (Role role : roles) {
						List<RoleMenu> existingMenus = TenantUtil.ignore(() ->
							roleMenuService.list(Wrappers.<RoleMenu>query().lambda().eq(RoleMenu::getRoleId, role.getId()))
						);
						if (existingMenus.isEmpty()) {
							List<RoleMenu> roleMenus = new ArrayList<>();
							for (Long mid : menuIds) {
								RoleMenu rm = new RoleMenu();
								rm.setRoleId(role.getId());
								rm.setMenuId(mid);
								roleMenus.add(rm);
							}
							if (!roleMenus.isEmpty()) {
								TenantUtil.ignore(() -> roleMenuService.saveBatch(roleMenus));
							}
							reassignedRoleCount++;
						}
					}
					if (reassignedRoleCount > 0) {
						actions.add("✅ 为 " + reassignedRoleCount + " 个角色重新关联产品包权限（" + menuCount + " 个菜单）");
					} else {
						actions.add("ℹ️ 所有角色的权限已正确关联，无需重新分配");
					}
				} else {
					actions.add("⚠️ 产品包中没有配置任何菜单权限");
				}
			} else {
				actions.add("⚠️ 租户未分配产品包，无法自动关联菜单权限");
			}
		} else {
			actions.add("❌ 租户下没有任何角色，无法修复权限。请先为租户创建角色");
		}

		CacheUtil.clear(SYS_CACHE);

		result.set("fixedUserCount", fixedUserCount);
		result.set("actions", actions);
		result.set("success", !actions.stream().anyMatch(a -> a.startsWith("❌")));

		String conclusion = actions.stream().anyMatch(a -> a.startsWith("❌"))
			? "部分问题需要手动处理"
			: (fixedUserCount > 0 ? "已自动完成所有修复" : "未发现需要修复的问题");

		result.set("conclusion", conclusion);
		return R.data(result);
	}

}
