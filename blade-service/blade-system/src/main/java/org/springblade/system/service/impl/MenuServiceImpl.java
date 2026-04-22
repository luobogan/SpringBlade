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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.system.dto.MenuDTO;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.PackageMenu;
import org.springblade.system.entity.RoleMenu;
import org.springblade.system.entity.RoleScope;
import org.springblade.system.entity.Tenant;
import org.springblade.system.entity.TopMenuSetting;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.service.IMenuService;
import org.springblade.system.service.IPackageMenuService;
import org.springblade.system.service.IRoleMenuService;
import org.springblade.system.service.IRoleScopeService;
import org.springblade.system.service.ITenantService;
import org.springblade.system.service.ITopMenuSettingService;
import org.springblade.system.vo.MenuVO;
import org.springblade.system.wrapper.MenuWrapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.springblade.common.constant.CommonConstant.API_SCOPE_CATEGORY;
import static org.springblade.common.constant.CommonConstant.DATA_SCOPE_CATEGORY;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Slf4j
@Service
@AllArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

	private final IRoleMenuService roleMenuService;
	private final IRoleScopeService roleScopeService;
	private final ITopMenuSettingService topMenuSettingService;
	private final IPackageMenuService packageMenuService;
	private final ITenantService tenantService;
	private final static String PARENT_ID = "parentId";

	@Override
	public IPage<MenuVO> selectMenuPage(IPage<MenuVO> page, MenuVO menu) {
		return page.setRecords(baseMapper.selectMenuPage(page, menu));
	}

	@Override
	public List<MenuVO> lazyMenuList(Long parentId, Map<String, Object> param) {
		if (Func.isEmpty(Func.toStr(param.get(PARENT_ID)))) {
			parentId = null;
		}
		return baseMapper.lazyMenuList(parentId, param);
	}

	@Override
	public List<MenuVO> routes(String roleId, Long topMenuId) {
		if (StringUtil.isBlank(roleId)) {
			log.warn("[Menu#routes] roleId 为空，返回 null");
			return null;
		}
		final String currentTenantId = TenantUtil.getTenantId();

		// 步骤1: 查询全部菜单
		List<Menu> allMenus = baseMapper.allMenu();
		log.info("[Menu#routes] ①allMenu(): 总数={}, tenantId={}", allMenus.size(), currentTenantId);

		// 过滤出当前租户的菜单
		final List<Menu> filteredAllMenus = BladeConstant.ADMIN_TENANT_ID.equals(currentTenantId)
			? allMenus
			: allMenus.stream()
				.filter(menu -> menu.getTenantId() != null && menu.getTenantId().equals(currentTenantId))
				.collect(Collectors.toList());
		log.info("[Menu#routes] ②filteredAllMenus(本租户菜单): 数量={}", filteredAllMenus.size());

		List<Menu> roleMenus;
		if (Func.isEmpty(topMenuId)) {
			List<Menu> roleIdMenus = baseMapper.roleMenuByRoleId(Func.toLongList(roleId));
			log.info("[Menu#routes] ③roleMenuByRoleId(roleId={}): 数量={}", roleId, roleIdMenus.size());
			if (roleIdMenus.isEmpty()) {
				log.warn("[Menu#routes] ⚠️ 角色没有关联任何菜单！roleId={}, tenantId={}", roleId, currentTenantId);
			}
			List<Menu> routes = new LinkedList<>(roleIdMenus);
			roleIdMenus.forEach(roleMenu -> recursion(filteredAllMenus, routes, roleMenu));
			roleMenus = routes;
			log.info("[Menu#routes] ④递归父级后: 数量={}", roleMenus.size());
		} else {
			List<Menu> roleIdMenus = baseMapper.roleMenuByRoleId(Func.toLongList(roleId));
			List<Menu> routes = new LinkedList<>(roleIdMenus);
			roleIdMenus.forEach(roleMenu -> recursion(filteredAllMenus, routes, roleMenu));
			List<Menu> topIdMenus = baseMapper.roleMenuByTopMenuId(topMenuId);
			roleMenus = topIdMenus.stream()
				.filter(x -> routes.stream().anyMatch(route -> route.getId().longValue() == x.getId().longValue()))
				.collect(Collectors.toList());
		}

		// 租户ID二次过滤
		if (!BladeConstant.ADMIN_TENANT_ID.equals(currentTenantId)) {
			int beforeFilter = roleMenus.size();
			roleMenus = roleMenus.stream()
				.filter(menu -> menu.getTenantId() != null && menu.getTenantId().equals(currentTenantId))
				.collect(Collectors.toList());
			log.info("[Menu#routes] ⑤租户ID二次过滤: {} → {}", beforeFilter, roleMenus.size());
		}

		// 产品包过滤
		if (!BladeConstant.ADMIN_TENANT_ID.equals(currentTenantId)) {
			int beforePackage = roleMenus.size();
			roleMenus = filterByTenantPackage(roleMenus, currentTenantId);
			log.info("[Menu#routes] ⑥产品包过滤: {} → {}", beforePackage, roleMenus.size());
		}

		log.info("[Menu#routes] 最终结果: tenantId={}, roleId={}, 菜单数={}", currentTenantId, roleId, roleMenus.size());
		return buildRoutes(filteredAllMenus, roleMenus);
	}

	/**
	 * 构建路由菜单
	 */
	private List<MenuVO> buildRoutes(List<Menu> allMenus, List<Menu> roleMenus) {
		List<Menu> routes = new LinkedList<>(roleMenus);
		roleMenus.forEach(roleMenu -> recursion(allMenus, routes, roleMenu));
		routes.sort(Comparator.comparing(Menu::getSort));
		MenuWrapper menuWrapper = new MenuWrapper();
		List<Menu> collect = routes.stream().filter(x -> Func.equals(x.getCategory(), 1)).collect(Collectors.toList());
		return menuWrapper.listNodeVO(collect);
	}

	/**
	 * 按租户产品包过滤菜单：
	 * 1. 根据租户ID查找其分配的产品包
	 * 2. 获取产品包授权的原始菜单ID集合（blade_package_menu.menu_id）
	 * 3. 通过租户副本的 sourceMenuId 字段做精确 ID 匹配（一次查询即可）
	 *
	 * 数据链路说明：
	 *   blade_package_menu.menu_id → 超级管理员原始菜单ID (如 1123598815738675201)
	 *   blade_menu.source_menu_id  → 租户副本记录原始菜单ID (同上)
	 *   匹配方式：packageMenuIds.contains(menu.getSourceMenuId())  ← 精确Long匹配
	 */
	private List<Menu> filterByTenantPackage(List<Menu> roleMenus, String tenantId) {
		try {
			Tenant tenant = TenantUtil.ignore(() ->
				tenantService.getOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId))
			);
			if (tenant == null || tenant.getPackageId() == null) {
				log.info("[Menu#filter] 租户未分配产品包，跳过过滤: tenant={}", tenantId);
				return roleMenus;
			}

			Long packageId = tenant.getPackageId();
			if (packageId == null || packageId <= 0) {
				return roleMenus;
			}

			Set<Long> packageMenuIds = TenantUtil.ignore(() -> {
				List<PackageMenu> pkgMenus = packageMenuService.list(
					Wrappers.<PackageMenu>query().lambda().eq(PackageMenu::getPackageId, packageId)
				);
				return pkgMenus.stream()
					.map(PackageMenu::getMenuId)
					.collect(Collectors.toSet());
			});

			if (packageMenuIds.isEmpty()) {
				log.warn("[Menu#filter] ⚠️ 产品包无任何菜单: tenant={}, packageId={}", tenantId, packageId);
				return roleMenus;
			}

			long nullCount = roleMenus.stream().filter(m -> m.getSourceMenuId() == null).count();
			if (nullCount > 0) {
				log.error("[Menu#filter] ❌ {}个菜单sourceMenuId为空！请删除旧副本后重新分配产品包", nullCount);
			}

			List<Menu> filtered = roleMenus.stream()
				.filter(menu -> menu.getSourceMenuId() != null && packageMenuIds.contains(menu.getSourceMenuId()))
				.collect(Collectors.toList());

			log.info("[Menu#filter]: tenant={}, packageId={}, 授权数={}, 输入={}, 空sourceId={}, 匹配={}",
				tenantId, packageId, packageMenuIds.size(), roleMenus.size(), nullCount, filtered.size());

			return filtered;
		} catch (Exception e) {
			log.warn("[Menu] 租户产品包过滤异常，返回原始菜单: {}", e.getMessage(), e);
			return roleMenus;
		}
	}

	public void recursion(List<Menu> allMenus, List<Menu> routes, Menu roleMenu) {
		Optional<Menu> menu = allMenus.stream().filter(x -> Func.equals(x.getId(), roleMenu.getParentId())).findFirst();
		if (menu.isPresent() && !routes.contains(menu.get())) {
			routes.add(menu.get());
			recursion(allMenus, routes, menu.get());
		}
	}

	@Override
	public List<MenuVO> buttons(String roleId) {
		final String currentTenantId = TenantUtil.getTenantId();
		List<Menu> buttons = baseMapper.buttons(Func.toLongList(roleId));
		log.info("[Menu#buttons] ①buttons(roleId={}): 原始数量={}, tenantId={}", roleId, buttons.size(), currentTenantId);

		if (buttons.isEmpty()) {
			log.warn("[Menu#buttons] ⚠️ 角色没有按钮权限！roleId={}, tenantId={}", roleId, currentTenantId);
		}

		// 租户ID过滤：只保留当前租户的菜单
		if (!BladeConstant.ADMIN_TENANT_ID.equals(currentTenantId)) {
			int beforeFilter = buttons.size();
			buttons = buttons.stream()
				.filter(menu -> menu.getTenantId() != null && menu.getTenantId().equals(currentTenantId))
				.collect(Collectors.toList());
			log.info("[Menu#buttons] ②租户ID过滤: {} → {}", beforeFilter, buttons.size());
		}

		// 产品包过滤
		if (!BladeConstant.ADMIN_TENANT_ID.equals(currentTenantId)) {
			int beforePackage = buttons.size();
			buttons = filterByTenantPackage(buttons, currentTenantId);
			log.info("[Menu#buttons] ③产品包过滤: {} → {}", beforePackage, buttons.size());
		}

		log.info("[Menu#buttons] 最终: tenantId={}, roleId={}, 按钮数={}", currentTenantId, roleId, buttons.size());

		MenuWrapper menuWrapper = new MenuWrapper();
		return menuWrapper.listNodeVO(buttons);
	}

	@Override
	public List<MenuVO> tree() {
		return ForestNodeMerger.merge(baseMapper.tree());
	}

	@Override
	public List<MenuVO> grantTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantTree(user.getTenantId()) : baseMapper.grantTreeByRole(user.getTenantId(), Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<MenuVO> grantTree(BladeUser user, String roleId) {
		// 始终返回当前租户的所有菜单，忽略 roleId
		List<MenuVO> tree = baseMapper.grantTree(user.getTenantId());
		return ForestNodeMerger.merge(tree);
	}

	@Override
	public List<MenuVO> grantDataScopeTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantDataScopeTree(user.getTenantId()) : baseMapper.grantDataScopeTreeByRole(user.getTenantId(), Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<MenuVO> grantDataScopeTree(BladeUser user, String roleId) {
		// 始终返回当前租户的所有数据权限，忽略 roleId
		List<MenuVO> tree = baseMapper.grantDataScopeTree(user.getTenantId());
		return ForestNodeMerger.merge(tree);
	}

	@Override
	public List<MenuVO> grantApiScopeTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantApiScopeTree(user.getTenantId()) : baseMapper.grantApiScopeTreeByRole(user.getTenantId(), Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<MenuVO> grantApiScopeTree(BladeUser user, String roleId) {
		// 始终返回当前租户的所有API权限，忽略 roleId
		List<MenuVO> tree = baseMapper.grantApiScopeTree(user.getTenantId());
		return ForestNodeMerger.merge(tree);
	}

	@Override
	public List<String> roleTreeKeys(String roleIds) {
		if (Func.isEmpty(roleIds)) {
			return new ArrayList<>();
		}
		List<RoleMenu> roleMenus = roleMenuService.list(Wrappers.<RoleMenu>query().lambda().in(RoleMenu::getRoleId, Func.toLongList(roleIds)));
		return roleMenus.stream().map(roleMenu -> Func.toStr(roleMenu.getMenuId())).collect(Collectors.toList());
	}

	@Override
	public List<String> dataScopeTreeKeys(String roleIds) {
		if (Func.isEmpty(roleIds)) {
			return new ArrayList<>();
		}
		List<RoleScope> roleScopes = roleScopeService.list(Wrappers.<RoleScope>query().lambda().eq(RoleScope::getScopeCategory, DATA_SCOPE_CATEGORY).in(RoleScope::getRoleId, Func.toLongList(roleIds)));
		return roleScopes.stream().map(roleScope -> Func.toStr(roleScope.getScopeId())).collect(Collectors.toList());
	}

	@Override
	public List<String> apiScopeTreeKeys(String roleIds) {
		if (Func.isEmpty(roleIds)) {
			return new ArrayList<>();
		}
		List<RoleScope> roleScopes = roleScopeService.list(Wrappers.<RoleScope>query().lambda().eq(RoleScope::getScopeCategory, API_SCOPE_CATEGORY).in(RoleScope::getRoleId, Func.toLongList(roleIds)));
		return roleScopes.stream().map(roleScope -> Func.toStr(roleScope.getScopeId())).collect(Collectors.toList());
	}

	@Override
	public List<Kv> authRoutes(BladeUser user) {
		List<MenuDTO> routes = baseMapper.authRoutes(Func.toLongList(user.getRoleId()));
		List<Kv> list = new ArrayList<>();
		routes.forEach(route -> list.add(Kv.init().set(route.getPath(), Kv.init().set("authority", Func.toStrArray(route.getAlias())))));
		return list;
	}

	@Override
	public List<MenuVO> grantTopTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantTopTree(user.getTenantId()) : baseMapper.grantTopTreeByRole(user.getTenantId(), Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<String> topTreeKeys(String topMenuIds) {
		List<TopMenuSetting> settings = topMenuSettingService.list(Wrappers.<TopMenuSetting>query().lambda().in(TopMenuSetting::getTopMenuId, Func.toLongList(topMenuIds)));
		return settings.stream().map(setting -> Func.toStr(setting.getMenuId())).collect(Collectors.toList());
	}

	@Override
	public int removeRecursive(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return 0;
		}

		int deletedCount = 0;

		// 递归获取所有子菜单ID
		List<Long> allMenuIds = getAllChildMenuIds(ids);

		if (!allMenuIds.isEmpty()) {
			// 删除角色菜单关联
			TenantUtil.ignore(() -> {
				roleMenuService.remove(Wrappers.<RoleMenu>query().lambda().in(RoleMenu::getMenuId, allMenuIds));
			});

			// 删除菜单
			deletedCount = baseMapper.deleteBatchIds(allMenuIds);
		}

		return deletedCount;
	}

	/**
	 * 递归获取所有子菜单ID
	 */
	private List<Long> getAllChildMenuIds(List<Long> parentIds) {
		if (parentIds == null || parentIds.isEmpty()) {
			return new ArrayList<>();
		}

		// 获取直接子菜单
		List<Menu> childMenus = baseMapper.selectList(Wrappers.<Menu>query().lambda().in(Menu::getParentId, parentIds));

		if (childMenus.isEmpty()) {
			return new ArrayList<>();
		}

		// 收集所有子菜单ID
		List<Long> allChildIds = childMenus.stream().map(Menu::getId).collect(Collectors.toList());

		// 递归获取孙子菜单
		List<Long> grandChildIds = getAllChildMenuIds(allChildIds);
		allChildIds.addAll(grandChildIds);

		return allChildIds;
	}

}
