package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalGridLayoutSaveDTO;
import org.springblade.portal.entity.PortalGridLayout;
import org.springblade.portal.entity.PortalGridLayoutShare;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalGridLayoutMapper;
import org.springblade.portal.mapper.PortalGridLayoutShareMapper;
import org.springblade.portal.service.IPortalGridLayoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 特色门户（门户网格布局）服务实现（对标泛微 tsportal §16.2，hp_grid_layout）
 * <p>layout_type：login=登录前门户 / theme=登录后门户；
 * layout_use=启用；layout_default_entrance=默认入口（同类型内唯一）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalGridLayoutServiceImpl implements IPortalGridLayoutService {

	private final PortalGridLayoutMapper layoutMapper;
	private final PortalGridLayoutShareMapper shareMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalGridLayout> list(String layoutType) {
		QueryWrapper<PortalGridLayout> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (layoutType != null && !layoutType.isBlank()) {
			qw.eq("layout_type", layoutType.trim());
		}
		qw.orderByAsc("layout_type").orderByAsc("id");
		return layoutMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalGridLayoutSaveDTO dto) {
		if (dto == null || isBlank(dto.getLayoutType()) || isBlank(dto.getLayoutName())) {
			return false;
		}
		String layoutType = dto.getLayoutType().trim();
		if (!PortalConstant.GRID_LAYOUT_LOGIN.equals(layoutType)
			&& !PortalConstant.GRID_LAYOUT_THEME.equals(layoutType)) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalGridLayout exist = (dto.getId() == null) ? null : layoutMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = layoutMapper.selectCount(new QueryWrapper<PortalGridLayout>()
				.eq("tenant_id", tenantId)
				.eq("layout_type", layoutType)
				.eq("layout_name", dto.getLayoutName().trim())
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalGridLayout();
			exist.setTenantId(tenantId);
			exist.setLayoutVersion(1);
			exist.setIsDeleted(0);
			exist.setStatus(1);
			exist.setCreateUser(userId);
			exist.setCreateDept(deptId);
			exist.setCreateTime(now);
		} else {
			exist.setUpdateUser(userId);
			exist.setUpdateTime(now);
			// 配置变更递增版本号（对标 layout_version）
			if (!java.util.Objects.equals(exist.getLayoutConfig(), dto.getLayoutConfig())) {
				exist.setLayoutVersion((exist.getLayoutVersion() == null ? 1 : exist.getLayoutVersion()) + 1);
			}
		}
		exist.setLayoutType(layoutType);
		exist.setLayoutName(dto.getLayoutName().trim());
		exist.setLayoutTitle(dto.getLayoutTitle());
		exist.setLayoutConfig(dto.getLayoutConfig());
		exist.setLayoutBackground(dto.getLayoutBackground());
		exist.setLayoutUse(dto.getLayoutUse() == null ? 0 : dto.getLayoutUse());
		boolean makeDefault = Integer.valueOf(1).equals(dto.getLayoutDefaultEntrance());
		exist.setLayoutDefaultEntrance(makeDefault ? 1 : 0);
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? layoutMapper.insert(exist) > 0 : layoutMapper.updateById(exist) > 0;
		if (ok && makeDefault) {
			// 同类型内默认入口唯一：清掉同类型其他布局的默认标记
			clearOtherDefaultEntrance(tenantId, layoutType, exist.getId());
		}
		if (ok) {
			logService.record(PortalConstant.LOG_GRID_LAYOUT_SAVE, "gridLayout", String.valueOf(exist.getId()),
				exist.getLayoutName(),
				(creating ? "新建" : "编辑") + layoutType + "网格布局 " + exist.getLayoutName(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalGridLayout exist = layoutMapper.selectById(id);
		if (exist == null || Integer.valueOf(1).equals(exist.getIsDeleted())) {
			return false;
		}
		if (Integer.valueOf(1).equals(exist.getLayoutDefaultEntrance())) {
			// 默认入口不可删（对标：删除前需先取消默认）
			return false;
		}
		PortalGridLayout gone = new PortalGridLayout();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		layoutMapper.update(gone, new QueryWrapper<PortalGridLayout>().eq("id", id));
		// 级联清授权
		PortalGridLayoutShare shareGone = new PortalGridLayoutShare();
		shareGone.setIsDeleted(1);
		shareGone.setStatus(-1);
		shareGone.setUpdateTime(LocalDateTime.now());
		shareMapper.update(shareGone, new QueryWrapper<PortalGridLayoutShare>().eq("layout_id", id));
		logService.record(PortalConstant.LOG_GRID_LAYOUT_DELETE, "gridLayout", String.valueOf(id),
			exist.getLayoutName(), "删除网格布局 " + exist.getLayoutName(), null, null);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setUse(Long id, boolean use) {
		if (id == null) {
			return false;
		}
		PortalGridLayout exist = layoutMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalGridLayout target = new PortalGridLayout();
		target.setLayoutUse(use ? 1 : 0);
		target.setUpdateTime(LocalDateTime.now());
		layoutMapper.update(target, new QueryWrapper<PortalGridLayout>().eq("id", id));
		logService.record(PortalConstant.LOG_GRID_LAYOUT_SAVE, "gridLayout", String.valueOf(id),
			exist.getLayoutName(), (use ? "启用" : "停用") + "网格布局 " + exist.getLayoutName(), null, null);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setDefaultEntrance(Long id) {
		if (id == null) {
			return false;
		}
		PortalGridLayout exist = layoutMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		clearOtherDefaultEntrance(exist.getTenantId(), exist.getLayoutType(), id);
		PortalGridLayout target = new PortalGridLayout();
		target.setLayoutDefaultEntrance(1);
		target.setUpdateTime(LocalDateTime.now());
		layoutMapper.update(target, new QueryWrapper<PortalGridLayout>().eq("id", id));
		logService.record(PortalConstant.LOG_GRID_LAYOUT_SAVE, "gridLayout", String.valueOf(id),
			exist.getLayoutName(), "设网格布局为默认入口 " + exist.getLayoutName(), null, null);
		return true;
	}

	@Override
	public List<PortalGridLayoutShare> listShare(Long layoutId) {
		if (layoutId == null) {
			return new ArrayList<>();
		}
		return shareMapper.selectList(new QueryWrapper<PortalGridLayoutShare>()
			.eq("layout_id", layoutId).eq("is_deleted", 0).orderByAsc("id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveShare(Long layoutId, List<PortalGridLayoutShare> shares) {
		if (layoutId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		// 差量：先软删旧行，再按传入键集插入（与 quicksearch/plugin 的 share 同模式）
		PortalGridLayoutShare gone = new PortalGridLayoutShare();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateUser(userId);
		gone.setUpdateTime(now);
		shareMapper.update(gone, new QueryWrapper<PortalGridLayoutShare>()
			.eq("tenant_id", tenantId).eq("layout_id", layoutId).eq("is_deleted", 0));

		if (shares != null && !shares.isEmpty()) {
			Set<String> seen = new HashSet<>();
			for (PortalGridLayoutShare s : shares) {
				if (s == null || s.getShareType() == null) {
					continue;
				}
				int st = s.getShareType();
				long sid = (s.getShareId() == null) ? 0L : s.getShareId();
				if (st != PortalConstant.SHARE_ALL && st != PortalConstant.SHARE_DEPT && st != PortalConstant.SHARE_USER) {
					continue;
				}
				if (st != PortalConstant.SHARE_ALL && sid <= 0) {
					continue;
				}
				if (st == PortalConstant.SHARE_ALL) {
					sid = 0L;
				}
				String key = st + ":" + sid;
				if (!seen.add(key)) {
					continue;
				}
				PortalGridLayoutShare ns = new PortalGridLayoutShare();
				ns.setTenantId(tenantId);
				ns.setLayoutId(layoutId);
				ns.setShareType(st);
				ns.setShareId(sid);
				ns.setIsDeleted(0);
				ns.setStatus(1);
				ns.setCreateUser(userId);
				ns.setCreateTime(now);
				shareMapper.insert(ns);
			}
		}
		logService.record(PortalConstant.LOG_GRID_LAYOUT_SAVE, "gridLayout", String.valueOf(layoutId),
			"布局授权", "保存网格布局使用限制（" + (shares == null ? 0 : shares.size()) + " 条）", null, null);
		return true;
	}

	/** 同类型内默认入口唯一：把同类型其他布局的默认标记清掉 */
	private void clearOtherDefaultEntrance(String tenantId, String layoutType, Long keepId) {
		PortalGridLayout clear = new PortalGridLayout();
		clear.setLayoutDefaultEntrance(0);
		clear.setUpdateTime(LocalDateTime.now());
		UpdateWrapper<PortalGridLayout> uw = new UpdateWrapper<>();
		uw.eq("tenant_id", tenantId)
			.eq("layout_type", layoutType)
			.eq("is_deleted", 0)
			.ne("id", keepId)
			.eq("layout_default_entrance", 1);
		layoutMapper.update(clear, uw);
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
