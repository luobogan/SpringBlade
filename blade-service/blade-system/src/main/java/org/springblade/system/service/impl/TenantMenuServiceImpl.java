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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.RoleMenu;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.service.IMenuService;
import org.springblade.system.service.IRoleMenuService;
import org.springblade.system.service.ITenantMenuService;
import org.springblade.system.service.ITenantPackageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 租户菜单服务实现类
 *
 * @author SpringBlade
 */
@Slf4j
@Service
@AllArgsConstructor
public class TenantMenuServiceImpl implements ITenantMenuService {

	private final IMenuService menuService;
	private final IRoleMenuService roleMenuService;
	private final MenuMapper menuMapper;
	private final ITenantPackageService tenantPackageService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int copyMenusForTenant(String tenantId, Long packageId, List<Long> menuIds) {
		log.info("[TenantMenu] ====== 开始复制菜单: tenantId={}, packageId={}, menuIds数量={} ======", tenantId, packageId, menuIds != null ? menuIds.size() : 0);

		if (menuIds == null || menuIds.isEmpty()) {
			log.warn("[TenantMenu] 菜单ID列表为空，跳过复制: tenantId={}, packageId={}", tenantId, packageId);
			return 0;
		}

		// 获取超级管理员的菜单
		List<Menu> superAdminMenus = TenantUtil.ignore(() ->
			menuMapper.selectBatchIds(menuIds)
		);

		if (superAdminMenus == null || superAdminMenus.isEmpty()) {
			log.warn("[TenantMenu] 超级管理员菜单不存在: menuIds={}", menuIds);
			return 0;
		}

		// 递归补全所有父菜单（确保父菜单也被复制）
		superAdminMenus = enrichParentMenus(superAdminMenus);
		log.info("[TenantMenu] 补全父菜单后，总菜单数: {} (原始: {})", superAdminMenus.size(), menuIds.size());

		// 获取租户已有的菜单（按code匹配，而不是id）
		final Map<String, Menu> tenantMenuMap = new HashMap<>();
		TenantUtil.ignore(() -> {
			QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("tenant_id", tenantId);
			List<Menu> tenantMenus = menuMapper.selectList(queryWrapper);
			tenantMenuMap.putAll(tenantMenus.stream()
				.collect(Collectors.toMap(Menu::getCode, menu -> menu)));
		});

		// 构建菜单ID映射：旧菜单ID -> 新菜单ID
		Map<Long, Long> menuIdMap = new HashMap<>();
		List<Menu> newMenus = new ArrayList<>();
		List<Menu> updateMenus = new ArrayList<>();

		// 按层级顺序处理菜单（先父后子）
		List<Menu> sortedMenus = sortMenusByHierarchy(superAdminMenus);

		// 收集新产品包中的菜单code
		Set<String> newMenuCodes = new HashSet<>();
		for (Menu menu : sortedMenus) {
			if (menu.getCode() != null) {
				newMenuCodes.add(menu.getCode());
			}
		}

		// 删除租户已有的、但不在新产品包中的菜单
		List<Menu> menusToDelete = new ArrayList<>();
		for (Map.Entry<String, Menu> entry : tenantMenuMap.entrySet()) {
			String code = entry.getKey();
			Menu menu = entry.getValue();
			if (!newMenuCodes.contains(code)) {
				menusToDelete.add(menu);
			}
		}

		if (!menusToDelete.isEmpty()) {
			// 递归删除菜单及其子菜单
			for (Menu menuToDelete : menusToDelete) {
				deleteMenuAndChildren(menuToDelete.getId(), tenantId);
			}
			log.info("[TenantMenu] 删除租户多余菜单: tenantId={}, 删除数量={}", tenantId, menusToDelete.size());
		}

		// 第一遍：处理菜单并构建ID映射
		for (Menu menu : sortedMenus) {
			// 检查租户是否已存在该code的菜单
			Menu existingMenu = tenantMenuMap.get(menu.getCode());

			Menu tenantMenu;
			if (existingMenu != null) {
				// 更新已有菜单
				tenantMenu = existingMenu;
				tenantMenu.setCode(menu.getCode());
				tenantMenu.setName(menu.getName());
				tenantMenu.setAlias(menu.getAlias());
				tenantMenu.setPath(menu.getPath());
				tenantMenu.setSource(menu.getSource());
				tenantMenu.setSort(menu.getSort());
				tenantMenu.setCategory(menu.getCategory());
				tenantMenu.setAction(menu.getAction());
				tenantMenu.setIsOpen(menu.getIsOpen());
				tenantMenu.setIsComponent(menu.getIsComponent());
				tenantMenu.setRemark(menu.getRemark());
				tenantMenu.setComponent(menu.getComponent());
				tenantMenu.setComponentType(menu.getComponentType());
				tenantMenu.setRemoteUrl(menu.getRemoteUrl());
				tenantMenu.setCacheVersion(menu.getCacheVersion());
				tenantMenu.setStatus(menu.getStatus());
				tenantMenu.setTenantId(tenantId);
				tenantMenu.setIsDeleted(0);
				// 写入源菜单ID：指向超级管理员的原始菜单（用于产品包权限精确匹配）
				tenantMenu.setSourceMenuId(menu.getId());

				// 处理父菜单ID映射（先用原始ID，后续更新）
				tenantMenu.setParentId(menu.getParentId());

				updateMenus.add(tenantMenu);
			} else {
				// 插入新菜单
				tenantMenu = new Menu();
				tenantMenu.setCode(menu.getCode());
				tenantMenu.setName(menu.getName());
				tenantMenu.setAlias(menu.getAlias());
				tenantMenu.setPath(menu.getPath());
				tenantMenu.setSource(menu.getSource());
				tenantMenu.setSort(menu.getSort());
				tenantMenu.setCategory(menu.getCategory());
				tenantMenu.setAction(menu.getAction());
				tenantMenu.setIsOpen(menu.getIsOpen());
				tenantMenu.setIsComponent(menu.getIsComponent());
				tenantMenu.setRemark(menu.getRemark());
				tenantMenu.setComponent(menu.getComponent());
				tenantMenu.setComponentType(menu.getComponentType());
				tenantMenu.setRemoteUrl(menu.getRemoteUrl());
				tenantMenu.setCacheVersion(menu.getCacheVersion());
				tenantMenu.setStatus(menu.getStatus());
				tenantMenu.setTenantId(tenantId);
				tenantMenu.setId(null);
				tenantMenu.setIsDeleted(0);
				// 写入源菜单ID：指向超级管理员的原始菜单（用于产品包权限精确匹配）
				tenantMenu.setSourceMenuId(menu.getId());

				// 处理父菜单ID映射（先用原始ID，后续更新）
				tenantMenu.setParentId(menu.getParentId());

				newMenus.add(tenantMenu);
			}

			// 记录ID映射（对于新菜单，先用原始ID占位）
			if (tenantMenu.getId() != null) {
				menuIdMap.put(menu.getId(), tenantMenu.getId());
			} else {
				// 对于新菜单，先用原始ID占位
				menuIdMap.put(menu.getId(), menu.getId());
			}
		}

		// 第二遍：保存新菜单（此时新菜单已有ID）
		if (!newMenus.isEmpty()) {
			TenantUtil.ignore(() -> {
				menuService.saveBatch(newMenus);
			});
			log.info("[TenantMenu] 插入租户新菜单: tenantId={}, 插入数量={}", tenantId, newMenus.size());

			// 更新ID映射（将新菜单的原始ID映射到新生成的ID）
			// 通过遍历sortedMenus，找到与newMenu具有相同code和path的原始菜单
			for (Menu newMenu : newMenus) {
				if (newMenu.getId() != null) {
					for (Menu originalMenu : sortedMenus) {
						// 通过code和path匹配原始菜单
				if (originalMenu.getCode() != null && originalMenu.getCode().equals(newMenu.getCode())
					&& Objects.equals(originalMenu.getPath(), newMenu.getPath())) {
					// 原始菜单的ID（作为key）-> 新菜单的ID（作为value）
					menuIdMap.put(originalMenu.getId(), newMenu.getId());
					break;
				}
					}
				}
			}

			// 更新新菜单的父菜单ID（此时ID映射已完整）
			// 构建新菜单到原始菜单的映射
			Map<String, Menu> newMenuMap = new HashMap<>();
			for (Menu newMenu : newMenus) {
				if (newMenu.getId() != null) {
					String key = newMenu.getCode() + "_" + (newMenu.getPath() != null ? newMenu.getPath() : "null");
					newMenuMap.put(key, newMenu);
				}
			}

			// 更新新菜单的父菜单ID
			for (Menu menu : sortedMenus) {
				if (tenantMenuMap.get(menu.getCode()) == null) {
					// 新菜单，从newMenuMap中获取
					String key = menu.getCode() + "_" + (menu.getPath() != null ? menu.getPath() : "null");
					Menu newMenu = newMenuMap.get(key);
					if (newMenu != null) {
						// 处理父菜单ID映射（使用完整的ID映射）
						if (menu.getParentId() != null && menuIdMap.containsKey(menu.getParentId())) {
							newMenu.setParentId(menuIdMap.get(menu.getParentId()));
						} else {
							newMenu.setParentId(menu.getParentId());
						}
					}
				}
			}

			// 批量更新新菜单的父菜单ID（此时parent_id已正确映射）
			TenantUtil.ignore(() -> {
				menuService.updateBatchById(newMenus);
			});
			log.info("[TenantMenu] 更新租户新菜单父ID: tenantId={}, 更新数量={}", tenantId, newMenus.size());
		}

		// 第三遍：更新已有菜单的父菜单ID（此时ID映射已完整）
		// 清空updateMenus，避免重复更新
		updateMenus.clear();
		for (Menu menu : sortedMenus) {
			Menu tenantMenu = tenantMenuMap.get(menu.getCode());
			if (tenantMenu != null) {
				// 处理父菜单ID映射（使用完整的ID映射）
				if (menu.getParentId() != null && menuIdMap.containsKey(menu.getParentId())) {
					tenantMenu.setParentId(menuIdMap.get(menu.getParentId()));
				} else {
					tenantMenu.setParentId(menu.getParentId());
				}
				updateMenus.add(tenantMenu);
			}
		}

		// 批量更新已有菜单
		if (!updateMenus.isEmpty()) {
			TenantUtil.ignore(() -> {
				menuService.updateBatchById(updateMenus);
			});
			log.info("[TenantMenu] 更新租户菜单: tenantId={}, 更新数量={}", tenantId, updateMenus.size());
		}

		log.info("[TenantMenu] 成功处理菜单: tenantId={}, packageId={}, 新增={}, 更新={}, 总数={}",
			tenantId, packageId, newMenus.size(), updateMenus.size(), newMenus.size() + updateMenus.size());

		return newMenus.size() + updateMenus.size();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void copyRoleMenusForTenant(String tenantId, List<Long> roleIds, List<Long> menuIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			log.warn("[TenantMenu] 角色ID为空，跳过复制: tenantId={}, roleIds={}", tenantId, roleIds);
			return;
		}

		// 先删除租户角色的所有现有菜单关联
		for (Long roleId : roleIds) {
			QueryWrapper<RoleMenu> deleteQuery = new QueryWrapper<>();
			deleteQuery.eq("role_id", roleId);
			TenantUtil.ignore(() -> roleMenuService.remove(deleteQuery));
		}
		log.info("[TenantMenu] 删除租户角色现有菜单关联: tenantId={}, 角色数={}", tenantId, roleIds.size());

		// 如果菜单ID为空，直接返回
		if (menuIds == null || menuIds.isEmpty()) {
			log.warn("[TenantMenu] 菜单ID为空，跳过复制: tenantId={}", tenantId);
			return;
		}

		// 为租户角色创建新的菜单关联
		List<RoleMenu> newRoleMenus = new ArrayList<>();
		for (Long roleId : roleIds) {
			for (Long menuId : menuIds) {
				RoleMenu newRoleMenu = new RoleMenu();
				newRoleMenu.setRoleId(roleId);
				newRoleMenu.setMenuId(menuId);
				newRoleMenus.add(newRoleMenu);
			}
		}

		// 批量保存新角色菜单关联
		if (!newRoleMenus.isEmpty()) {
			TenantUtil.ignore(() -> {
				roleMenuService.saveBatch(newRoleMenus);
			});
			log.info("[TenantMenu] 插入租户角色菜单关联: tenantId={}, 新增关联={}", tenantId, newRoleMenus.size());
		}

		log.info("[TenantMenu] 成功处理角色菜单: tenantId={}, 角色数={}, 菜单数={}",
			tenantId, roleIds.size(), menuIds.size());
	}

	/**
	 * 递归删除菜单及其子菜单
	 */
	private void deleteMenuAndChildren(Long menuId, String tenantId) {
		if (menuId == null) {
			return;
		}

		// 获取子菜单
		QueryWrapper<Menu> childQuery = new QueryWrapper<>();
		childQuery.eq("tenant_id", tenantId);
		childQuery.eq("parent_id", menuId);
		List<Menu> childMenus = TenantUtil.ignore(() -> menuMapper.selectList(childQuery));

		// 递归删除子菜单
		if (childMenus != null && !childMenus.isEmpty()) {
			for (Menu childMenu : childMenus) {
				deleteMenuAndChildren(childMenu.getId(), tenantId);
			}
		}

		// 删除菜单角色关联
		QueryWrapper<RoleMenu> roleMenuQuery = new QueryWrapper<>();
		roleMenuQuery.eq("menu_id", menuId);
		TenantUtil.ignore(() -> roleMenuService.remove(roleMenuQuery));

		// 删除菜单
		TenantUtil.ignore(() -> menuService.removeById(menuId));
	}

	@Override
	public List<Long> getTenantMenuIds(String tenantId, List<Long> menuIds) {
		if (menuIds == null || menuIds.isEmpty()) {
			return new ArrayList<>();
		}

		// 获取超级管理员菜单的code列表
		List<Menu> superAdminMenus = TenantUtil.ignore(() -> menuMapper.selectBatchIds(menuIds));
		Set<String> menuCodes = superAdminMenus.stream()
			.filter(menu -> menu.getCode() != null)
			.map(Menu::getCode)
			.collect(Collectors.toSet());

		if (menuCodes.isEmpty()) {
			return new ArrayList<>();
		}

		// 通过code查询租户的菜单
		QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("tenant_id", tenantId);
		queryWrapper.in("code", menuCodes);
		queryWrapper.select("id");

		List<Menu> tenantMenus = TenantUtil.ignore(() -> menuMapper.selectList(queryWrapper));
		return tenantMenus.stream().map(Menu::getId).collect(Collectors.toList());
	}

		/**
 * 递归补全所有父菜单
 * 确保所有菜单的父菜单都被包含在列表中，以便正确建立层级关系
 */
private List<Menu> enrichParentMenus(List<Menu> menus) {
	if (menus == null || menus.isEmpty()) {
		return menus;
	}

	Set<Long> existingIds = new HashSet<>();
	for (Menu menu : menus) {
		existingIds.add(menu.getId());
	}

	List<Menu> result = new ArrayList<>(menus);
	Set<Long> parentIdsToFetch = new HashSet<>();

	// 收集所有需要查找的父菜单ID
	for (Menu menu : menus) {
		Long parentId = menu.getParentId();
		if (parentId != null && parentId != 0 && !existingIds.contains(parentId)) {
			parentIdsToFetch.add(parentId);
		}
	}

	// 递归查找父菜单
	while (!parentIdsToFetch.isEmpty()) {
		List<Long> idsToQuery = new ArrayList<>(parentIdsToFetch);
		parentIdsToFetch.clear();

		List<Menu> parentMenus = TenantUtil.ignore(() -> menuMapper.selectBatchIds(idsToQuery));
		if (parentMenus == null || parentMenus.isEmpty()) {
			break;
		}

		for (Menu parentMenu : parentMenus) {
			if (!existingIds.contains(parentMenu.getId())) {
				result.add(parentMenu);
				existingIds.add(parentMenu.getId());

				// 继续向上查找祖父菜单
				Long grandParentId = parentMenu.getParentId();
				if (grandParentId != null && grandParentId != 0 && !existingIds.contains(grandParentId)) {
					parentIdsToFetch.add(grandParentId);
				}
			}
		}
	}

	return result;
}

/**
 * 按层级顺序排序菜单（先父后子）
 */
private List<Menu> sortMenusByHierarchy(List<Menu> menus) {
	// 构建菜单ID到菜单的映射
	Map<Long, Menu> menuMap = menus.stream()
		.collect(Collectors.toMap(Menu::getId, menu -> menu));

	// 拓扑排序
	List<Menu> sorted = new ArrayList<>();
	Set<Long> processed = new HashSet<>();

	// 先处理顶级菜单（parentId为null或0）
	for (Menu menu : menus) {
		if ((menu.getParentId() == null || menu.getParentId() == 0) && !processed.contains(menu.getId())) {
			sortMenuRecursive(menu, menuMap, sorted, processed);
		}
	}

	// 处理剩余的菜单（可能是子菜单，但父菜单不在列表中）
	for (Menu menu : menus) {
		if (!processed.contains(menu.getId())) {
			sortMenuRecursive(menu, menuMap, sorted, processed);
		}
	}

	return sorted;
}

/**
 * 递归排序菜单
 */
private void sortMenuRecursive(Menu menu, Map<Long, Menu> menuMap, List<Menu> sorted, Set<Long> processed) {
	// 先处理父菜单
	if (menu.getParentId() != null && menu.getParentId() != 0 && menuMap.containsKey(menu.getParentId())) {
		Menu parent = menuMap.get(menu.getParentId());
		if (!processed.contains(parent.getId())) {
			sortMenuRecursive(parent, menuMap, sorted, processed);
		}
	}

	// 处理当前菜单
	if (!processed.contains(menu.getId())) {
		sorted.add(menu);
		processed.add(menu.getId());
	}
}
}
