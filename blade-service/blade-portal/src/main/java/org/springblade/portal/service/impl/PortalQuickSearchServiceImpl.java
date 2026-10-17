package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalQuickSearchSaveDTO;
import org.springblade.portal.entity.PortalQuickSearch;
import org.springblade.portal.entity.PortalQuickSearchShare;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalQuickSearchMapper;
import org.springblade.portal.mapper.PortalQuickSearchShareMapper;
import org.springblade.portal.service.IPortalQuickSearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 快捷搜索管理服务实现（对标泛微 quicksearchmanage §14.2 H）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalQuickSearchServiceImpl implements IPortalQuickSearchService {

	private final PortalQuickSearchMapper searchMapper;
	private final PortalQuickSearchShareMapper shareMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalQuickSearch> list() {
		return searchMapper.selectList(new QueryWrapper<PortalQuickSearch>()
			.eq("is_deleted", 0).orderByAsc("order_num").orderByAsc("id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalQuickSearchSaveDTO dto) {
		if (dto == null || isBlank(dto.getModule()) || isBlank(dto.getType()) || isBlank(dto.getCustomName())) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalQuickSearch exist = (dto.getId() == null) ? null : searchMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			// 同租户 module+type 唯一
			long dup = searchMapper.selectCount(new QueryWrapper<PortalQuickSearch>()
				.eq("tenant_id", tenantId)
				.eq("module", dto.getModule().trim())
				.eq("type", dto.getType().trim())
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalQuickSearch();
			exist.setTenantId(tenantId);
			exist.setIsSys(0);
			exist.setIsDeleted(0);
			exist.setStatus(1);
			exist.setCreateUser(userId);
			exist.setCreateDept(deptId);
			exist.setCreateTime(now);
		} else {
			if (Integer.valueOf(1).equals(exist.getIsSys())) {
				// 内置类型仅允许改启用/排序/显示名
				exist.setCustomName(dto.getCustomName().trim());
				exist.setIsUse(dto.getIsUse() == null ? exist.getIsUse() : dto.getIsUse());
				exist.setOrderNum(dto.getOrderNum() == null ? exist.getOrderNum() : dto.getOrderNum());
				exist.setUpdateUser(userId);
				exist.setUpdateTime(now);
				return searchMapper.updateById(exist) > 0;
			}
			exist.setUpdateUser(userId);
			exist.setUpdateTime(now);
		}
		exist.setModule(dto.getModule().trim());
		exist.setType(dto.getType().trim());
		exist.setCustomName(dto.getCustomName().trim());
		exist.setIcon(dto.getIcon());
		exist.setUrl(dto.getUrl());
		exist.setLinkMode(dto.getLinkMode() == null ? PortalConstant.LINK_MODE_INTERNAL : dto.getLinkMode());
		exist.setIsUse(dto.getIsUse() == null ? 1 : dto.getIsUse());
		exist.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? searchMapper.insert(exist) > 0 : searchMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_QUICK_SEARCH_SAVE, "quickSearch", String.valueOf(exist.getId()),
				exist.getCustomName(),
				(creating ? "新建快捷搜索 " : "编辑快捷搜索 ") + exist.getCustomName(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalQuickSearch exist = searchMapper.selectById(id);
		if (exist == null || Integer.valueOf(1).equals(exist.getIsSys())) {
			return false; // 不存在或系统内置不可删
		}
		LocalDateTime now = LocalDateTime.now();
		PortalQuickSearch gone = new PortalQuickSearch();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(now);
		searchMapper.update(gone, new QueryWrapper<PortalQuickSearch>().eq("id", id));
		// 级联清理使用限制
		PortalQuickSearchShare shareGone = new PortalQuickSearchShare();
		shareGone.setIsDeleted(1);
		shareGone.setStatus(-1);
		shareGone.setUpdateTime(now);
		shareMapper.update(shareGone, new QueryWrapper<PortalQuickSearchShare>()
			.eq("search_id", id).eq("is_deleted", 0));
		logService.record(PortalConstant.LOG_QUICK_SEARCH_DELETE, "quickSearch", String.valueOf(id),
			exist.getCustomName(), "删除快捷搜索 " + exist.getCustomName(), null, null);
		return true;
	}

	@Override
	public List<PortalQuickSearchShare> listShare(Long searchId) {
		if (searchId == null) {
			return new ArrayList<>();
		}
		return shareMapper.selectList(new QueryWrapper<PortalQuickSearchShare>()
			.eq("search_id", searchId).eq("is_deleted", 0)
			.orderByAsc("share_type").orderByAsc("share_id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveShare(Long searchId, List<PortalQuickSearchShare> shares) {
		if (searchId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		// 归一化：5 种共享范围，全员强制 shareId=0，去重
		List<String> wantKeys = new ArrayList<>();
		List<PortalQuickSearchShare> normalized = new ArrayList<>();
		if (shares != null) {
			for (PortalQuickSearchShare s : shares) {
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
				PortalQuickSearchShare ns = new PortalQuickSearchShare();
				ns.setShareType(type);
				ns.setShareId(shareId);
				normalized.add(ns);
			}
		}

		List<PortalQuickSearchShare> existing = shareMapper.selectList(
			new QueryWrapper<PortalQuickSearchShare>().eq("search_id", searchId).eq("is_deleted", 0));
		Set<String> existKeys = new HashSet<>();
		for (PortalQuickSearchShare e : existing) {
			existKeys.add((e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId()));
		}

		int removed = 0;
		for (PortalQuickSearchShare e : existing) {
			String key = (e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId());
			if (!wantKeys.contains(key)) {
				PortalQuickSearchShare gone = new PortalQuickSearchShare();
				gone.setIsDeleted(1);
				gone.setStatus(-1);
				gone.setUpdateUser(userId);
				gone.setUpdateTime(now);
				shareMapper.update(gone, new QueryWrapper<PortalQuickSearchShare>().eq("id", e.getId()));
				removed++;
			}
		}
		int added = 0;
		for (PortalQuickSearchShare s : normalized) {
			String key = s.getShareType() + ":" + s.getShareId();
			if (existKeys.contains(key)) {
				continue;
			}
			PortalQuickSearchShare ns = new PortalQuickSearchShare();
			ns.setTenantId(tenantId);
			ns.setSearchId(searchId);
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
		logService.record(PortalConstant.LOG_QUICK_SEARCH_SHARE, "quickSearch", String.valueOf(searchId),
			null, "更新快捷搜索使用限制：新增 " + added + " 条，移除 " + removed + " 条", null, null);
		return true;
	}

	private String currentTenant() {
		BladeUser user = SecureUtil.getUser();
		if (user != null && user.getTenantId() != null && !user.getTenantId().isBlank()) {
			return user.getTenantId();
		}
		return "000000";
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
