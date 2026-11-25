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
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
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
	private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
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
		final String currentTenantId = TenantUtil.getTenantId();
		BladeUser currentUser = SecureUtil.getUser();
		Set<Long> sharedMenuIds = resolveSharedMenuIds(currentTenantId, roleId, currentUser);
		if (StringUtil.isBlank(roleId) && sharedMenuIds.isEmpty()) {
			log.warn("[Menu#routes] roleId 为空且无菜单共享授权，返回 null");
			return null;
		}

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
		List<Menu> roleIdMenus = StringUtil.isBlank(roleId)
			? new LinkedList<>() : baseMapper.roleMenuByRoleId(Func.toLongList(roleId));
		if (!sharedMenuIds.isEmpty()) {
			Set<Long> known = roleIdMenus.stream().filter(Objects::nonNull).map(Menu::getId).filter(Objects::nonNull).collect(Collectors.toSet());
			for (Long menuId : sharedMenuIds) {
				if (known.contains(menuId)) continue;
				filteredAllMenus.stream().filter(m -> m.getId() != null && menuId.equals(m.getId())).findFirst().ifPresent(roleIdMenus::add);
			}
		}
		log.info("[Menu#routes] ③roleMenuByRoleId+share(roleId={}): 数量={}", roleId, roleIdMenus.size());
		if (roleIdMenus.isEmpty()) {
			log.warn("[Menu#routes] ⚠️ 角色和共享授权均未关联菜单！roleId={}, tenantId={}", roleId, currentTenantId);
		}
		List<Menu> routes = new LinkedList<>(roleIdMenus);
		roleIdMenus.forEach(roleMenu -> recursion(filteredAllMenus, routes, roleMenu));
		roleMenus = routes;
		log.info("[Menu#routes] ④递归父级后: 数量={}", roleMenus.size());
		if (Func.isNotEmpty(topMenuId)) {
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

		// 角色级菜单显示名：按当前角色覆盖 name（未配置的角色沿用 blade_menu.name）
		applyRoleMenuNames(roleMenus, roleId, currentTenantId);

		log.info("[Menu#routes] 最终结果: tenantId={}, roleId={}, 菜单数={}", currentTenantId, roleId, roleMenus.size());
		return buildRoutes(filteredAllMenus, roleMenus);
	}

	/**
	 * 解析当前用户命中的菜单共享。
	 *
	 * <p>share_type：1角色 2人员 3部门 4分部 5岗位 6安全级别 7全员。</p>
	 */
	private Set<Long> resolveSharedMenuIds(String tenantId, String roleId, BladeUser user) {
		Set<Long> result = new LinkedHashSet<>();
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT menu_id,share_type,share_id,include_sub,showlevel,showlevel2 FROM blade_menu_share "
					+ "WHERE tenant_id=? AND status=1 AND is_deleted=0", tenantId);
			if (rows == null || rows.isEmpty()) return result;
			Long userId = user == null ? null : user.getUserId();
			Set<Long> roleIds = parseIds(roleId);
			roleIds.addAll(parseIds(user == null ? null : user.getRoleId()));
			Set<Long> deptIds = parseIds(user == null ? null : user.getDeptId());
			Set<Long> deptAncestors = new LinkedHashSet<>(deptIds);
			for (Long deptId : deptIds) resolveDeptAncestors(deptId, deptAncestors);
			Long branchId = resolveBranchId(deptAncestors);
			Set<Long> postIds = resolveUserPostIds(userId);
			Integer seclevel = resolveUserSeclevel(userId);
			for (Map<String, Object> row : rows) {
				Object menuObj = row.get("menu_id"), typeObj = row.get("share_type");
				if (!(menuObj instanceof Number) || !(typeObj instanceof Number)) continue;
				int type = ((Number) typeObj).intValue();
				boolean hit = false;
				if (type == 7) {
					hit = true;
				} else if (row.get("share_id") instanceof Number) {
					long shareId = ((Number) row.get("share_id")).longValue();
					if (type == 1) hit = roleIds.contains(shareId);
					else if (type == 2) hit = userId != null && userId == shareId;
					else if (type == 3) {
						int includeSub = row.get("include_sub") instanceof Number ? ((Number) row.get("include_sub")).intValue() : 0;
						hit = deptIds.contains(shareId) || (includeSub == 1 && deptAncestors.contains(shareId));
					} else if (type == 4) hit = branchId != null && branchId == shareId;
					else if (type == 5) hit = postIds.contains(shareId);
					else if (type == 8) hit = userId != null && matrixContainsUser(shareId, null, userId);
				} else if (type == 6 && seclevel != null) {
					int lower = row.get("showlevel") instanceof Number ? ((Number) row.get("showlevel")).intValue() : 0;
					int upper = row.get("showlevel2") instanceof Number ? ((Number) row.get("showlevel2")).intValue() : Integer.MAX_VALUE;
					hit = seclevel >= lower && seclevel <= upper;
				}
				if (hit && (row.get("showlevel") != null || row.get("showlevel2") != null)) {
					if (seclevel == null) hit = false;
					else {
						int lower = row.get("showlevel") instanceof Number ? ((Number) row.get("showlevel")).intValue() : 0;
						int upper = row.get("showlevel2") instanceof Number ? ((Number) row.get("showlevel2")).intValue() : Integer.MAX_VALUE;
						hit = seclevel >= lower && seclevel <= upper;
					}
				}
				if (hit) result.add(((Number) menuObj).longValue());
			}
		} catch (Exception e) {
			log.warn("[Menu#resolveSharedMenuIds] 查询菜单共享失败，按无共享处理. tenantId={}, err={}", tenantId, e.getMessage());
		}
		return result;
	}

	/** 判断当前用户是否出现在矩阵结果字段中（用于菜单共享的矩阵主体）。 */
	private boolean matrixContainsUser(Long matrixId, String valueField, Long userId) {
		if (matrixId == null || userId == null) return false;
		try {
			List<String> rows = jdbcTemplate.queryForList(
				"SELECT data_json FROM wf_matrix_data WHERE matrix_id=? AND is_deleted=0", String.class, matrixId);
			String needle = "," + userId + ",";
			for (String json : rows) {
				if (json != null && json.contains(String.valueOf(userId))) return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private Set<Long> parseIds(String csv) {
		Set<Long> result = new LinkedHashSet<>();
		if (StringUtil.isBlank(csv)) return result;
		for (String part : csv.split(",")) {
			if (StringUtil.isBlank(part)) continue;
			try { result.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) { }
		}
		return result;
	}

	private void resolveDeptAncestors(Long deptId, Set<Long> result) {
		if (deptId == null || deptId <= 0 || !result.add(deptId)) return;
		try {
			List<Long> parents = jdbcTemplate.queryForList(
				"SELECT parent_id FROM blade_dept WHERE id=? AND is_deleted=0", Long.class, deptId);
			if (parents != null && !parents.isEmpty() && parents.get(0) != null) {
				resolveDeptAncestors(parents.get(0), result);
			}
		} catch (Exception e) {
			log.warn("[Menu#resolveDeptAncestors] 查询部门父级失败. deptId={}, err={}", deptId, e.getMessage());
		}
	}

	/** SpringBlade 无独立分部表：用部门树最浅非零祖先作为分部口径 */
	private Long resolveBranchId(Set<Long> deptIds) {
		Long branch = null;
		int minDepth = Integer.MAX_VALUE;
		for (Long deptId : deptIds) {
			if (deptId == null || deptId <= 0) continue;
			List<Long> chain = new ArrayList<>();
			Long current = deptId;
			while (current != null && current > 0 && chain.size() < 50) {
				chain.add(current);
				try {
					List<Long> parents = jdbcTemplate.queryForList(
						"SELECT parent_id FROM blade_dept WHERE id=? AND is_deleted=0", Long.class, current);
					current = (parents == null || parents.isEmpty()) ? null : parents.get(0);
				} catch (Exception e) {
					current = null;
				}
			}
			Long candidate = chain.isEmpty() ? null : chain.get(chain.size() - 1);
			if (candidate != null && chain.size() < minDepth) {
				branch = candidate;
				minDepth = chain.size();
			}
		}
		return branch;
	}

	private Set<Long> resolveUserPostIds(Long userId) {
		if (userId == null) return new LinkedHashSet<>();
		try {
			List<String> rows = jdbcTemplate.queryForList(
				"SELECT post_id FROM blade_user WHERE id=? AND is_deleted=0", String.class, userId);
			return (rows == null || rows.isEmpty()) ? new LinkedHashSet<>() : parseIds(rows.get(0));
		} catch (Exception e) {
			return new LinkedHashSet<>();
		}
	}

	private Integer resolveUserSeclevel(Long userId) {
		if (userId == null) return null;
		try {
			List<Integer> rows = jdbcTemplate.queryForList(
				"SELECT security_level FROM blade_user WHERE id=? AND is_deleted=0", Integer.class, userId);
			return (rows == null || rows.isEmpty()) ? null : rows.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 角色级菜单显示名覆盖：查 blade_role_menu_name，按当前角色覆盖菜单 name。
	 *
	 * <p>多角色时按 roleId 在 token 中的顺序取第一个命中别名的角色；未配置别名的菜单保持原名。
	 * 表不存在/查询异常时静默跳过（不影响菜单主流程）。</p>
	 */
	private void applyRoleMenuNames(List<Menu> roleMenus, String roleId, String tenantId) {
		if (roleMenus == null || roleMenus.isEmpty() || StringUtil.isBlank(roleId)) return;
		try {
			List<Long> roleIds = Func.toLongList(roleId);
			if (roleIds.isEmpty()) return;
			String menuMarks = roleMenus.stream().map(Menu::getId).filter(Objects::nonNull)
				.distinct().map(String::valueOf).collect(Collectors.joining(","));
			if (menuMarks.isEmpty()) return;
			// 查当前租户下这些角色+菜单的别名配置（按 roleIds 顺序，Java 侧取第一个命中）
			// 参数化 IN，避免超长拼接导致 SQL 语法/长度问题
			String roleQ = String.join(",", java.util.Collections.nCopies(roleIds.size(), "?"));
			String[] menuIds = menuMarks.split(",");
			String menuQ = String.join(",", java.util.Collections.nCopies(menuIds.length, "?"));
			List<Object> args = new java.util.ArrayList<>();
			args.add(tenantId);
			args.addAll(roleIds);
			for (String mid : menuIds) args.add(Long.valueOf(mid));
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT role_id, menu_id, name_override FROM blade_role_menu_name "
					+ "WHERE tenant_id=? AND role_id IN (" + roleQ + ") AND menu_id IN (" + menuQ + ")",
				args.toArray());
			if (rows == null || rows.isEmpty()) return;
			Map<Long, Map<Long, String>> byRole = new HashMap<>();
			for (Map<String, Object> row : rows) {
				Object r = row.get("role_id"), m = row.get("menu_id"), n = row.get("name_override");
				if (!(r instanceof Number) || !(m instanceof Number) || n == null) continue;
				byRole.computeIfAbsent(((Number) r).longValue(), k -> new HashMap<>())
					.put(((Number) m).longValue(), String.valueOf(n));
			}
			for (Menu menu : roleMenus) {
				if (menu == null || menu.getId() == null) continue;
				for (Long rid : roleIds) {
					Map<Long, String> names = byRole.get(rid);
					String override = names == null ? null : names.get(menu.getId().longValue());
					if (StringUtil.isNotBlank(override)) {
						menu.setName(override);
						break;
					}
				}
			}
		} catch (Exception e) {
			log.warn("[Menu#routes] 角色菜单名覆盖失败，沿用原名. roleId={}, err={}", roleId, e.getMessage());
		}
	}

	/**
	 * 构建路由菜单
	 */
	private List<MenuVO> buildRoutes(List<Menu> allMenus, List<Menu> roleMenus) {
		List<Menu> routes = new LinkedList<>(roleMenus);
		roleMenus.forEach(roleMenu -> recursion(allMenus, routes, roleMenu));
		routes.sort(Comparator.comparing(Menu::getSort));
		MenuWrapper menuWrapper = new MenuWrapper();
		// 除一级菜单（category=1）外，一并放行「组件型菜单」（category=2 且 is_component=1）：
		// 它们是 /formmode/exceldesign/ExcelPreviewPage、/formmode/approval/ApprovalPage 这类
		// 「独立页」的路由载体。此前只保留 category=1，导致 /menu/routes 从不下发它们，
		// 前端只能靠 config/routes.ts 的静态声明兜底，违背「前端路由 100% 由 blade_menu 驱动」。
		// 注意：category=2 且 is_component=0 的是纯权限码/按钮，不生成路由，仍需排除；
		// 前端侧边栏会再过滤掉 category=2，故放开这里不会把独立页塞进左侧菜单。
		List<Menu> collect = routes.stream()
			.filter(x -> Func.equals(x.getCategory(), 1)
				|| (Func.equals(x.getCategory(), 2) && Func.equals(x.getIsComponent(), 1)))
			.collect(Collectors.toList());
		return menuWrapper.listNodeVO(distinctMenuById(collect));
	}

	/**
	 * 按菜单 id 去重，保留原有顺序。
	 * <p>
	 * {@code MenuWrapper.listNodeVO} 内部走 {@code ForestNodeMerger.merge} →
	 * {@code ForestNodeManager}，其构造用的是 Guava {@code Maps.uniqueIndex}，
	 * 一旦同一菜单出现两次就抛 `Multiple entries with same key`（整个接口 500）。
	 * 典型场景：某按钮自身 category=2，其下又挂了 category=2 的子按钮，
	 * 而 {@code MenuMapper.buttons} 的两条分支会把这条菜单各命中一次。
	 * 除 SQL 侧用 UNION 去重外，这里再做一层防御，避免其他入口（角色交叉授权、
	 * 多角色合并等）再次踩坑。
	 */
	private List<Menu> distinctMenuById(List<Menu> menus) {
		if (Func.isEmpty(menus)) {
			return menus;
		}
		List<Menu> distinct = new ArrayList<>(menus.size());
		Set<Long> seen = new HashSet<>(menus.size());
		for (Menu menu : menus) {
			if (menu == null) {
				continue;
			}
			Long id = menu.getId();
			if (id == null || seen.add(id)) {
				distinct.add(menu);
			}
		}
		return distinct;
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
		// 菜单共享：把当前用户命中的菜单（含 AAE 组件页）并入按钮数据，
		// 前端 loopMenuItem 才能按 parentId 找到组件页并注册 /formmode/formview/aae 路由。
		Set<Long> sharedMenuIds = resolveSharedMenuIds(currentTenantId, roleId, SecureUtil.getUser());
		if (!sharedMenuIds.isEmpty()) {
			Set<Long> exists = buttons.stream().filter(Objects::nonNull).map(Menu::getId).filter(Objects::nonNull).collect(Collectors.toSet());
			for (Menu menu : baseMapper.selectBatchIds(sharedMenuIds)) {
				if (menu != null && menu.getId() != null && !exists.contains(menu.getId())) buttons.add(menu);
			}
		}
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

		// 按 id 去重后再建树：同一菜单出现两次会让 ForestNodeManager 抛
		// "Multiple entries with same key" 导致接口 500（全站按钮权限为空）
		int beforeDistinct = buttons.size();
		buttons = distinctMenuById(buttons);
		if (buttons.size() != beforeDistinct) {
			log.warn("[Menu#buttons] ⚠️ 检测到重复菜单并已去重: {} → {}（roleId={}）", beforeDistinct, buttons.size(), roleId);
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
		// 菜单按租户各存一份副本，必须按当前用户租户过滤，否则会出现多个租户的同名重复菜单
		String tenantId = user.getTenantId();
		return ForestNodeMerger.merge(isAdmin(user) ? baseMapper.grantTree(tenantId) : baseMapper.grantTreeByRole(Func.toLongList(user.getRoleId()), tenantId));
	}

	@Override
	public List<MenuVO> grantTree(BladeUser user, String roleId) {
		// 始终返回当前租户的所有菜单，忽略 roleId
		List<MenuVO> tree = baseMapper.grantTree(user.getTenantId());
		return ForestNodeMerger.merge(tree);
	}

	@Override
	public List<MenuVO> grantDataScopeTree(BladeUser user) {
		String tenantId = user.getTenantId();
		return ForestNodeMerger.merge(isAdmin(user) ? baseMapper.grantDataScopeTree(tenantId) : baseMapper.grantDataScopeTreeByRole(Func.toLongList(user.getRoleId()), tenantId));
	}

	@Override
	public List<MenuVO> grantDataScopeTree(BladeUser user, String roleId) {
		// 始终返回当前租户的所有数据权限，忽略 roleId
		List<MenuVO> tree = baseMapper.grantDataScopeTree(user.getTenantId());
		return ForestNodeMerger.merge(tree);
	}

	@Override
	public List<MenuVO> grantApiScopeTree(BladeUser user) {
		String tenantId = user.getTenantId();
		return ForestNodeMerger.merge(isAdmin(user) ? baseMapper.grantApiScopeTree(tenantId) : baseMapper.grantApiScopeTreeByRole(Func.toLongList(user.getRoleId()), tenantId));
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
		String tenantId = user.getTenantId();
		return ForestNodeMerger.merge(isAdmin(user) ? baseMapper.grantTopTree(tenantId) : baseMapper.grantTopTreeByRole(Func.toLongList(user.getRoleId()), tenantId));
	}

	/**
	 * 是否超管租户（超管不做租户过滤，可见全部菜单）
	 *
	 * @param user 当前用户
	 * @return 是否超管租户
	 */
	private boolean isAdmin(BladeUser user) {
		return user != null && BladeConstant.ADMIN_TENANT_ID.equals(user.getTenantId());
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

		// 选中菜单 + 全部子孙菜单
		List<Long> merged = new ArrayList<>(new LinkedHashSet<>(ids));
		merged.addAll(getAllChildMenuIds(ids));
		final List<Long> allMenuIds = new ArrayList<>(new LinkedHashSet<>(merged));
		if (allMenuIds.isEmpty()) return 0;

		String marks = String.join(",", Collections.nCopies(allMenuIds.size(), "?"));
		TenantUtil.ignore(() -> {
			roleMenuService.remove(Wrappers.<RoleMenu>query().lambda().in(RoleMenu::getMenuId, allMenuIds));
			try {
				List<Object> args = new ArrayList<>(allMenuIds);
				jdbcTemplate.update("DELETE FROM blade_role_menu_name WHERE menu_id IN (" + marks + ")", args.toArray());
			} catch (Exception ignored) { }
			try {
				List<Object> args = new ArrayList<>(allMenuIds);
				jdbcTemplate.update("DELETE FROM blade_menu_share WHERE menu_id IN (" + marks + ")", args.toArray());
			} catch (Exception ignored) { }
			try {
				List<Object> args = new ArrayList<>(allMenuIds);
				jdbcTemplate.update("UPDATE blade_menu SET parent_id=0 WHERE parent_id IN (" + marks + ")", args.toArray());
			} catch (Exception ignored) { }
		});

		return baseMapper.deleteBatchIds(allMenuIds);
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
