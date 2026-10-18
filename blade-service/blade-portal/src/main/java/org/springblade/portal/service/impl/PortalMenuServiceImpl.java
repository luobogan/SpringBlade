package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMenuSaveDTO;
import org.springblade.portal.entity.BladeMenu;
import org.springblade.portal.entity.PortalMenu;
import org.springblade.portal.entity.PortalMenuShare;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.BladeMenuMapper;
import org.springblade.portal.mapper.PortalDeptMapper;
import org.springblade.portal.mapper.PortalMenuMapper;
import org.springblade.portal.mapper.PortalMenuShareMapper;
import org.springblade.portal.service.IPortalConfigService;
import org.springblade.portal.service.IPortalMenuService;
import org.springblade.portal.vo.PortalDeptMenuVO;
import org.springblade.portal.vo.PortalMenuNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门户菜单服务实现（对标泛微 portalmenu §14）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalMenuServiceImpl implements IPortalMenuService {

	private final PortalMenuMapper menuMapper;
	private final PortalMenuShareMapper shareMapper;
	private final PortalDeptMapper deptMapper;
	private final BladeMenuMapper bladeMenuMapper;
	private final IPortalLogService logService;
	private final IPortalConfigService configService;

	/** 菜单树缓存 TTL（毫秒；与配置项缓存同口径） */
	private static final long MENU_CACHE_TTL_MS = 60_000L;

	/** 菜单树进程内缓存（isOpenMenuCache 开启时生效；对标 MenuCache §14.3）：key=userId|类型, value=缓存项 */
	private final Map<String, MenuCacheEntry> menuTreeCache = new ConcurrentHashMap<>();

	/** 菜单树缓存项（过期时间戳 + 节点快照） */
	private static class MenuCacheEntry {
		private final long expireAt;
		private final List<PortalMenuNode> nodes;

		private MenuCacheEntry(long expireAt, List<PortalMenuNode> nodes) {
			this.expireAt = expireAt;
			this.nodes = nodes;
		}
	}

	@Override
	public List<PortalMenu> list(String menuType) {
		QueryWrapper<PortalMenu> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (menuType != null && !menuType.isBlank()) {
			qw.eq("menu_type", menuType.trim());
		}
		qw.orderByAsc("sort_order").orderByAsc("id");
		return menuMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalMenuSaveDTO dto) {
		if (dto == null || isBlank(dto.getMenuName()) || isBlank(dto.getMenuType())) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";

		long parentId = (dto.getParentId() == null) ? 0L : dto.getParentId();
		if (parentId < 0) {
			parentId = 0L;
		}
		// 父菜单存在性校验（parentId>0 时必须存在且同类型）
		if (parentId > 0) {
			PortalMenu parent = menuMapper.selectById(parentId);
			if (parent == null || Integer.valueOf(1).equals(parent.getIsDeleted())
				|| !dto.getMenuType().trim().equals(parent.getMenuType())) {
				return false;
			}
		}

		PortalMenu menu = (dto.getMenuId() == null) ? null : menuMapper.selectById(dto.getMenuId());
		boolean creating = (menu == null);
		LocalDateTime now = LocalDateTime.now();
		if (creating) {
			menu = new PortalMenu();
			menu.setTenantId(tenantId);
			menu.setParentId(parentId);
			menu.setIsDeleted(0);
			menu.setStatus(1);
			menu.setIsSystem(0);
			menu.setCreateUser(userId);
			menu.setCreateDept(deptId);
			menu.setCreateTime(now);
		} else {
			if (Integer.valueOf(1).equals(menu.getIsSystem())) {
				// 内置菜单仅允许改显隐与排序，不改结构
				menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
				menu.setSortOrder(dto.getSortOrder() == null ? menu.getSortOrder() : dto.getSortOrder());
				menu.setUpdateUser(userId);
				menu.setUpdateTime(now);
				boolean sysOk = menuMapper.updateById(menu) > 0;
				if (sysOk) {
					clearMenuCache();
				}
				return sysOk;
			}
			menu.setParentId(parentId);
			menu.setUpdateUser(userId);
			menu.setUpdateTime(now);
		}
		menu.setMenuName(dto.getMenuName().trim());
		menu.setMenuType(dto.getMenuType().trim());
		menu.setIcon(dto.getIcon());
		menu.setRouteUrl(dto.getRouteUrl());
		menu.setLinkMode(dto.getLinkMode() == null ? PortalConstant.LINK_MODE_INTERNAL : dto.getLinkMode());
		menu.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
		menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
		menu.setConfigJson(dto.getConfigJson());

		boolean ok = creating ? menuMapper.insert(menu) > 0 : menuMapper.updateById(menu) > 0;
		if (ok) {
			clearMenuCache();
			logService.record(creating ? PortalConstant.LOG_MENU_CREATE : PortalConstant.LOG_MENU_UPDATE,
				"menu", String.valueOf(menu.getId()), menu.getMenuName(),
				(creating ? "新建门户菜单 " : "编辑门户菜单 ") + menu.getMenuName(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long menuId) {
		if (menuId == null) {
			return false;
		}
		PortalMenu menu = menuMapper.selectById(menuId);
		if (menu == null || Integer.valueOf(1).equals(menu.getIsSystem())) {
			return false; // 不存在或内置不可删
		}
		// 收集该菜单及其全部子孙（同类型），一并逻辑删除
		List<PortalMenu> sameType = menuMapper.selectList(new QueryWrapper<PortalMenu>()
			.eq("menu_type", menu.getMenuType()).eq("is_deleted", 0));
		Set<Long> toDelete = new HashSet<>();
		collectDescendants(menuId, sameType, toDelete);
		toDelete.add(menuId);

		LocalDateTime now = LocalDateTime.now();
		for (Long id : toDelete) {
			PortalMenu gone = new PortalMenu();
			gone.setIsDeleted(1);
			gone.setStatus(-1);
			gone.setUpdateTime(now);
			menuMapper.update(gone, new QueryWrapper<PortalMenu>().eq("id", id));
			// 清理授权
			PortalMenuShare shareGone = new PortalMenuShare();
			shareGone.setIsDeleted(1);
			shareGone.setStatus(-1);
			shareMapper.update(shareGone, new QueryWrapper<PortalMenuShare>().eq("menu_id", id).eq("is_deleted", 0));
		}
		logService.record(PortalConstant.LOG_MENU_DELETE, "menu", String.valueOf(menuId),
			menu.getMenuName(), "删除门户菜单 " + menu.getMenuName() + "（含子菜单 " + (toDelete.size() - 1) + " 个）",
			null, null);
		clearMenuCache();
		return true;
	}

	@Override
	public List<PortalMenuNode> menuTree(String menuType) {
		if (isBlank(menuType)) {
			return new ArrayList<>();
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		// 缓存读取（isOpenMenuCache 开启时生效；管理端写操作后全量失效）
		String cacheKey = (userId == null ? "anon" : String.valueOf(userId)) + "|menu:" + menuType.trim();
		boolean cacheOn = configService.isEnabled(PortalConstant.CONFIG_MENU_CACHE);
		if (cacheOn) {
			MenuCacheEntry hit = menuTreeCache.get(cacheKey);
			if (hit != null && hit.expireAt > System.currentTimeMillis()) {
				return hit.nodes;
			}
		}
		String deptId = (user == null) ? null : user.getDeptId();
		String roleIds = (user == null) ? null : user.getRoleId();
		Long branchId = resolveBranchId(deptId);

		List<PortalMenu> all = menuMapper.selectList(new QueryWrapper<PortalMenu>()
			.eq("menu_type", menuType.trim()).eq("is_deleted", 0).eq("status", 1)
			.eq("visible", 1).orderByAsc("sort_order").orderByAsc("id"));
		if (all.isEmpty()) {
			return new ArrayList<>();
		}
		List<Long> ids = new ArrayList<>();
		for (PortalMenu m : all) {
			ids.add(m.getId());
		}
		List<PortalMenuShare> shares = shareMapper.selectList(
			new QueryWrapper<PortalMenuShare>().in("menu_id", ids).eq("is_deleted", 0));

		// 可见性：无授权记录=默认全员可见；有授权记录则须命中（全员/本部门/本人）
		Map<Long, PortalMenuNode> nodeMap = new LinkedHashMap<>();
		Set<Long> hasShare = new HashSet<>();
		for (PortalMenuShare s : shares) {
			if (s.getMenuId() != null) {
				hasShare.add(s.getMenuId());
			}
		}
		for (PortalMenu m : all) {
			if (hasShare.contains(m.getId()) && !hitShare(m.getId(), shares, userId, deptId, roleIds, branchId)) {
				continue;
			}
			nodeMap.put(m.getId(), toNode(m));
		}
		if (nodeMap.isEmpty()) {
			return new ArrayList<>();
		}

		// 组装树：父在可见集合内则挂载，否则视为顶级（避免丢节点）
		List<PortalMenuNode> roots = new ArrayList<>();
		for (PortalMenuNode node : nodeMap.values()) {
			Long pid = node.getParentId();
			PortalMenuNode parent = (pid != null && pid > 0) ? nodeMap.get(pid) : null;
			if (parent != null && !Objects.equals(parent.getId(), node.getId())) {
				parent.getChildren().add(node);
			} else {
				roots.add(node);
			}
		}
		if (cacheOn && !roots.isEmpty()) {
			menuTreeCache.put(cacheKey, new MenuCacheEntry(System.currentTimeMillis() + MENU_CACHE_TTL_MS, roots));
		}
		return roots;
	}

	@Override
	public List<PortalMenuNode> systemMenuTree(String region) {
		BladeUser user = SecureUtil.getUser();
		// 缓存读取（isOpenMenuCache 开启时生效；注意：系统菜单(blade_menu)的变更发生在
		// blade-system 侧，缓存最长 TTL 后自然过期，属可接受延迟）
		String sysCacheKey = (user == null || user.getUserId() == null ? "anon" : String.valueOf(user.getUserId()))
			+ "|sys:" + ((region == null) ? "" : region.trim());
		boolean cacheOn = configService.isEnabled(PortalConstant.CONFIG_MENU_CACHE);
		if (cacheOn) {
			MenuCacheEntry hit = menuTreeCache.get(sysCacheKey);
			if (hit != null && hit.expireAt > System.currentTimeMillis()) {
				return hit.nodes;
			}
		}
		String tenantId = (user == null || user.getTenantId() == null || user.getTenantId().isBlank())
			? "000000" : user.getTenantId();
		// region 生效（对标泛微 leftmenu/topmenu 分工 §14.1）：
		//   front = 前端菜单（左侧导航，返回完整树）
		//   top   = 后端/顶部菜单（仅返回一级节点，顶部导航只呈现一级入口）
		boolean topOnly = PortalConstant.MENU_TYPE_TOP.equalsIgnoreCase(
			(region == null) ? "" : region.trim());
		String nodeMenuType = topOnly ? PortalConstant.MENU_TYPE_TOP : PortalConstant.MENU_TYPE_FRONT;

		List<BladeMenu> all = bladeMenuMapper.selectNavMenus(tenantId);
		if (all.isEmpty()) {
			return new ArrayList<>();
		}
		// 超管见全部；其余按 角色授权(blade_role_menu) 过滤，并补齐祖先保证树可导航
		boolean isAdmin = user != null && user.getRoleName() != null
			&& user.getRoleName().toLowerCase().contains("administrator");
		Set<Long> allowed = null;
		if (!isAdmin) {
			List<Long> roleIds = parseLongList(user == null ? null : user.getRoleId());
			Set<Long> granted = new HashSet<>();
			if (!roleIds.isEmpty()) {
				granted.addAll(bladeMenuMapper.selectMenuIdsByRoleIds(roleIds));
			}
			allowed = new HashSet<>(granted);
			Map<Long, BladeMenu> byId = new HashMap<>();
			for (BladeMenu m : all) {
				byId.put(m.getId(), m);
			}
			for (Long g : granted) {
				BladeMenu cur = byId.get(g);
				while (cur != null && cur.getParentId() != null && cur.getParentId() > 0
					&& byId.containsKey(cur.getParentId()) && !allowed.contains(cur.getParentId())) {
					allowed.add(cur.getParentId());
					cur = byId.get(cur.getParentId());
				}
			}
		}
		// 过滤 + 组装树
		Map<Long, PortalMenuNode> nodeMap = new LinkedHashMap<>();
		for (BladeMenu m : all) {
			if (allowed != null && !allowed.contains(m.getId())) {
				continue;
			}
			// 顶部菜单只保留一级节点（parentId=0 或空）
			if (topOnly && m.getParentId() != null && m.getParentId() > 0) {
				continue;
			}
			PortalMenuNode n = new PortalMenuNode();
			n.setId(m.getId());
			n.setParentId(m.getParentId());
			n.setMenuName(m.getName());
			n.setRouteUrl(m.getPath());
			n.setMenuType(nodeMenuType);
			n.setLinkMode(PortalConstant.LINK_MODE_INTERNAL);
			n.setSortOrder(m.getSort());
			nodeMap.put(m.getId(), n);
		}
		List<PortalMenuNode> roots = new ArrayList<>();
		for (PortalMenuNode node : nodeMap.values()) {
			Long pid = node.getParentId();
			PortalMenuNode parent = (pid != null && pid > 0) ? nodeMap.get(pid) : null;
			if (parent != null && !Objects.equals(parent.getId(), node.getId()) && !topOnly) {
				parent.getChildren().add(node);
			} else {
				roots.add(node);
			}
		}
		if (cacheOn && !roots.isEmpty()) {
			menuTreeCache.put(sysCacheKey, new MenuCacheEntry(System.currentTimeMillis() + MENU_CACHE_TTL_MS, roots));
		}
		return roots;
	}

	/** 菜单树缓存全量失效（save/remove/saveShare 后调用；管理端低频操作，全量清理安全） */
	private void clearMenuCache() {
		menuTreeCache.clear();
	}

	private List<Long> parseLongList(String csv) {
		List<Long> out = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return out;
		}
		for (String s : csv.split(",")) {
			Long v = toLongOrNull(s);
			if (v != null) {
				out.add(v);
			}
		}
		return out;
	}

	@Override
	public List<PortalMenuShare> listShare(Long menuId) {
		if (menuId == null) {
			return new ArrayList<>();
		}
		return shareMapper.selectList(new QueryWrapper<PortalMenuShare>()
			.eq("menu_id", menuId).eq("is_deleted", 0)
			.orderByAsc("share_type").orderByAsc("share_id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveShare(Long menuId, List<PortalMenuShare> shares) {
		if (menuId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();

		List<String> wantKeys = new ArrayList<>();
		List<PortalMenuShare> normalized = new ArrayList<>();
		if (shares != null) {
			for (PortalMenuShare s : shares) {
				if (s == null || s.getShareType() == null) {
					continue;
				}
				int type = s.getShareType();
				if (type != PortalConstant.SHARE_ALL && type != PortalConstant.SHARE_DEPT
					&& type != PortalConstant.SHARE_USER && type != PortalConstant.SHARE_ROLE
					&& type != PortalConstant.SHARE_BRANCH) {
					continue;
				}
				long shareId = (type == PortalConstant.SHARE_ALL) ? 0L
					: (s.getShareId() == null ? 0L : s.getShareId());
				if (type != PortalConstant.SHARE_ALL && shareId <= 0) {
					continue;
				}
				String key = type + ":" + shareId;
				if (wantKeys.contains(key)) {
					continue;
				}
				wantKeys.add(key);
				PortalMenuShare ns = new PortalMenuShare();
				ns.setShareType(type);
				ns.setShareId(shareId);
				normalized.add(ns);
			}
		}

		List<PortalMenuShare> existing = shareMapper.selectList(
			new QueryWrapper<PortalMenuShare>().eq("menu_id", menuId).eq("is_deleted", 0));
		Set<String> existKeys = new HashSet<>();
		for (PortalMenuShare e : existing) {
			existKeys.add((e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId()));
		}

		int removed = 0;
		for (PortalMenuShare e : existing) {
			String key = (e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId());
			if (!wantKeys.contains(key)) {
				PortalMenuShare gone = new PortalMenuShare();
				gone.setIsDeleted(1);
				gone.setStatus(-1);
				gone.setUpdateUser(userId);
				gone.setUpdateTime(now);
				shareMapper.update(gone, new QueryWrapper<PortalMenuShare>().eq("id", e.getId()));
				removed++;
			}
		}
		int added = 0;
		for (PortalMenuShare s : normalized) {
			String key = s.getShareType() + ":" + s.getShareId();
			if (existKeys.contains(key)) {
				continue;
			}
			PortalMenuShare ns = new PortalMenuShare();
			ns.setTenantId(tenantId);
			ns.setMenuId(menuId);
			ns.setShareType(s.getShareType());
			ns.setShareId(s.getShareId());
			ns.setIsDeleted(0);
			ns.setStatus(1);
			ns.setCreateUser(userId);
			ns.setCreateTime(now);
			ns.setUpdateUser(userId);
			ns.setUpdateTime(now);
			shareMapper.insert(ns);
			added++;
		}
		logService.record(PortalConstant.LOG_MENU_SHARE, "menu", String.valueOf(menuId),
			null, "更新菜单可见范围：新增 " + added + " 条，移除 " + removed + " 条", null, null);
		clearMenuCache();
		return true;
	}

	@Override
	public List<PortalDeptMenuVO> deptMenuShare(Long deptId, String menuType) {
		if (deptId == null || isBlank(menuType)) {
			return new ArrayList<>();
		}
		String type = menuType.trim();
		List<PortalMenu> menus = menuMapper.selectList(new QueryWrapper<PortalMenu>()
			.eq("menu_type", type).eq("is_deleted", 0)
			.orderByAsc("sort_order").orderByAsc("id"));
		List<PortalDeptMenuVO> out = new ArrayList<>();
		if (menus.isEmpty()) {
			return out;
		}
		List<Long> ids = new ArrayList<>();
		for (PortalMenu m : menus) {
			ids.add(m.getId());
		}
		Map<Long, List<PortalMenuShare>> byMenu = new HashMap<>();
		for (PortalMenuShare s : shareMapper.selectList(
			new QueryWrapper<PortalMenuShare>().in("menu_id", ids).eq("is_deleted", 0))) {
			byMenu.computeIfAbsent(s.getMenuId(), k -> new ArrayList<>()).add(s);
		}
		String deptKey = String.valueOf(deptId).trim();

		for (PortalMenu m : menus) {
			List<PortalMenuShare> shares = byMenu.getOrDefault(m.getId(), Collections.emptyList());
			int realCount = 0;
			boolean deptHit = false;
			boolean limited = !shares.isEmpty();
			for (PortalMenuShare s : shares) {
				int st = s.getShareType() == null ? 0 : s.getShareType();
				if (st == PortalConstant.SHARE_LIMIT) {
					continue;
				}
				realCount++;
				if (st == PortalConstant.SHARE_ALL) {
					deptHit = true;
				} else if (st == PortalConstant.SHARE_DEPT
					&& s.getShareId() != null && String.valueOf(s.getShareId()).equals(deptKey)) {
					deptHit = true;
				}
			}
			PortalDeptMenuVO vo = new PortalDeptMenuVO();
			vo.setId(m.getId());
			vo.setParentId(m.getParentId());
			vo.setMenuName(m.getMenuName());
			vo.setIcon(m.getIcon());
			vo.setRouteUrl(m.getRouteUrl());
			vo.setMenuType(m.getMenuType());
			vo.setSortOrder(m.getSortOrder());
			vo.setVisible(m.getVisible());
			// 无授权记录=全员可见 → 该部门可见；已限定的菜单按是否命中本部门判定
			vo.setChecked(!limited || deptHit);
			vo.setLimited(limited);
			vo.setShareCount(realCount);
			out.add(vo);
		}
		return out;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveDeptShare(Long deptId, String menuType, List<Long> menuIds) {
		if (deptId == null || isBlank(menuType)) {
			return false;
		}
		String type = menuType.trim();
		List<PortalMenu> menus = menuMapper.selectList(new QueryWrapper<PortalMenu>()
			.eq("menu_type", type).eq("is_deleted", 0));
		if (menus.isEmpty()) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();
		Set<Long> target = new HashSet<>();
		if (menuIds != null) {
			for (Long id : menuIds) {
				if (id != null) {
					target.add(id);
				}
			}
		}

		List<Long> ids = new ArrayList<>();
		for (PortalMenu m : menus) {
			ids.add(m.getId());
		}
		Map<Long, List<PortalMenuShare>> byMenu = new HashMap<>();
		for (PortalMenuShare s : shareMapper.selectList(
			new QueryWrapper<PortalMenuShare>().in("menu_id", ids).eq("is_deleted", 0))) {
			byMenu.computeIfAbsent(s.getMenuId(), k -> new ArrayList<>()).add(s);
		}
		String deptKey = String.valueOf(deptId).trim();

		int added = 0;
		int removed = 0;
		int limited = 0;
		for (PortalMenu m : menus) {
			List<PortalMenuShare> shares = byMenu.getOrDefault(m.getId(), Collections.emptyList());
			boolean want = target.contains(m.getId());
			List<PortalMenuShare> deptRecs = new ArrayList<>();
			boolean hasLimit = false;
			int realCount = 0;
			for (PortalMenuShare s : shares) {
				int st = s.getShareType() == null ? 0 : s.getShareType();
				if (st == PortalConstant.SHARE_LIMIT) {
					hasLimit = true;
					continue;
				}
				realCount++;
				if (st == PortalConstant.SHARE_DEPT
					&& s.getShareId() != null && String.valueOf(s.getShareId()).equals(deptKey)) {
					deptRecs.add(s);
				}
			}
			if (want) {
				if (deptRecs.isEmpty()) {
					shareMapper.insert(newShare(m.getId(), PortalConstant.SHARE_DEPT, deptId, tenantId, userId, now));
					added++;
				}
			} else {
				for (PortalMenuShare gone : deptRecs) {
					PortalMenuShare off = new PortalMenuShare();
					off.setIsDeleted(1);
					off.setStatus(-1);
					off.setUpdateUser(userId);
					off.setUpdateTime(now);
					shareMapper.update(off, new QueryWrapper<PortalMenuShare>().eq("id", gone.getId()));
					removed++;
				}
				// 取消勾选必须落「限定标记」：否则 share 清空后菜单回落成全员可见，
				// 管理员看到的「该部门不可见」根本不生效（见 PortalConstant.SHARE_LIMIT）
				if (!hasLimit) {
					shareMapper.insert(newShare(m.getId(), PortalConstant.SHARE_LIMIT, 0L, tenantId, userId, now));
					limited++;
				}
			}
		}
		logService.record(PortalConstant.LOG_MENU_SHARE, "menu", "dept:" + deptId,
			null, "按部门更新菜单权限：授权 " + target.size() + " 项，新增 " + added
				+ " 条，移除 " + removed + " 条，限定标记 " + limited + " 个菜单", null, null);
		clearMenuCache();
		return true;
	}

	private PortalMenuShare newShare(Long menuId, Integer shareType, Long shareId,
									 String tenantId, Long userId, LocalDateTime now) {
		PortalMenuShare ns = new PortalMenuShare();
		ns.setTenantId(tenantId);
		ns.setMenuId(menuId);
		ns.setShareType(shareType);
		ns.setShareId(shareId);
		ns.setIsDeleted(0);
		ns.setStatus(1);
		ns.setCreateUser(userId);
		ns.setCreateTime(now);
		ns.setUpdateUser(userId);
		ns.setUpdateTime(now);
		return ns;
	}

	/* ==================== 私有方法 ==================== */

	private void collectDescendants(Long rootId, List<PortalMenu> all, Set<Long> out) {
		for (PortalMenu m : all) {
			if (Objects.equals(m.getParentId(), rootId) && !out.contains(m.getId())) {
				out.add(m.getId());
				collectDescendants(m.getId(), all, out);
			}
		}
	}

	private PortalMenuNode toNode(PortalMenu m) {
		PortalMenuNode n = new PortalMenuNode();
		n.setId(m.getId());
		n.setParentId(m.getParentId());
		n.setMenuName(m.getMenuName());
		n.setIcon(m.getIcon());
		n.setRouteUrl(m.getRouteUrl());
		n.setMenuType(m.getMenuType());
		n.setLinkMode(m.getLinkMode());
		n.setSortOrder(m.getSortOrder());
		n.setVisible(m.getVisible());
		return n;
	}

	private boolean hitShare(Long menuId, List<PortalMenuShare> shares, Long userId, String deptId, String roleIds, Long branchId) {
		for (PortalMenuShare share : shares) {
			if (!Objects.equals(share.getMenuId(), menuId)) {
				continue;
			}
			int type = share.getShareType() == null ? 0 : share.getShareType();
			// 限定标记哨兵：本身永不命中，只负责让 hasShare 为真
			// （菜单从「全员可见」切换为「须命中才可见」，见 PortalConstant.SHARE_LIMIT）
			if (type == PortalConstant.SHARE_LIMIT) {
				continue;
			}
			if (type == PortalConstant.SHARE_ALL) {
				return true;
			}
			if (type == PortalConstant.SHARE_DEPT) {
				if (deptId != null && share.getShareId() != null
					&& String.valueOf(share.getShareId()).equals(deptId.trim())) {
					return true;
				}
			} else if (type == PortalConstant.SHARE_USER) {
				if (userId != null && Objects.equals(share.getShareId(), userId)) {
					return true;
				}
			} else if (type == PortalConstant.SHARE_ROLE) {
				if (roleIdHit(roleIds, share.getShareId())) {
					return true;
				}
			} else if (type == PortalConstant.SHARE_BRANCH) {
				if (branchId != null && Objects.equals(share.getShareId(), branchId)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean roleIdHit(String roleIds, Long shareId) {
		if (roleIds == null || roleIds.isBlank() || shareId == null) {
			return false;
		}
		for (String r : roleIds.split(",")) {
			if (r.trim().equals(String.valueOf(shareId))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析用户所属分部：部门树最浅非零祖先；自身即顶层部门时分部=自身（对标 shareinnerhp type=6）。
	 */
	private Long resolveBranchId(String deptId) {
		Long deptIdLong = toLongOrNull(deptId);
		if (deptIdLong == null) {
			return null;
		}
		String ancestors = deptMapper.getAncestors(deptIdLong);
		if (ancestors != null && !ancestors.isBlank()) {
			for (String p : ancestors.split(",")) {
				String t = p.trim();
				if (t.isEmpty() || "0".equals(t)) {
					continue;
				}
				Long aid = toLongOrNull(t);
				if (aid != null) {
					return aid;
				}
			}
		}
		return deptIdLong;
	}

	private Long toLongOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}
}
