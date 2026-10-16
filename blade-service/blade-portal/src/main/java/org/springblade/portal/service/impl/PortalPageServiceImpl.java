package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalLayoutItem;
import org.springblade.portal.dto.PortalPageSaveDTO;
import org.springblade.portal.entity.PortalDefault;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalElementSetting;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.entity.PortalPageShare;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalDefaultMapper;
import org.springblade.portal.mapper.PortalDeptMapper;
import org.springblade.portal.mapper.PortalElementMapper;
import org.springblade.portal.mapper.PortalElementSettingMapper;
import org.springblade.portal.mapper.PortalPageMapper;
import org.springblade.portal.mapper.PortalPageShareMapper;
import org.springblade.portal.mapper.PortalPageStatMapper;
import org.springblade.portal.mapper.PortalPageVisitorMapper;
import org.springblade.portal.service.IPortalPageService;
import org.springblade.portal.service.IPortalThemeService;
import org.springblade.portal.service.IPortalUserDefaultService;
import org.springblade.portal.service.IPortalUserLayoutService;
import org.springblade.portal.vo.MyPortalVO;
import org.springblade.portal.vo.PortalElementVO;
import org.springblade.portal.vo.PortalPageBrief;
import org.springblade.portal.vo.PortalPageExportVO;
import org.springblade.portal.vo.PortalPageStatVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门户页面服务实现
 * <p>
 * 「我的门户」聚合口径（对标泛微：homepage + hpelement + hpElementSettingDetail）：
 * <pre>
 *   目标页解析优先级：portalCode 精确 &gt; 机构默认首页(按部门祖先链最具体) &gt; 全局默认页 &gt; 命中授权页
 *   元素个性化 = blade_portal_element（基准）被 blade_portal_element_setting（当前用户）覆盖
 * </pre>
 * 写操作（save/setDefault/remove）经 {@link IPortalLogService} 埋点审计（§13.4）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPageServiceImpl implements IPortalPageService {

	private final PortalPageMapper pageMapper;
	private final PortalElementMapper elementMapper;
	private final PortalPageShareMapper shareMapper;
	private final PortalPageStatMapper pageStatMapper;
	private final PortalPageVisitorMapper pageVisitorMapper;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private final PortalElementSettingMapper settingMapper;
	private final IPortalLogService logService;
	private final PortalDefaultMapper defaultMapper;
	private final PortalDeptMapper deptMapper;
	private final IPortalThemeService themeService;
	private final IPortalUserDefaultService userDefaultService;
	private final IPortalUserLayoutService userLayoutService;

	@Override
	public List<PortalPage> list() {
		QueryWrapper<PortalPage> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0).orderByAsc("sort").orderByAsc("id");
		return pageMapper.selectList(qw);
	}

	@Override
	public MyPortalVO getMyPortal(String portalCode) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		// BladeUser.deptId 为 String 类型，与 share_id（bigint）比较时统一按字符串比对
		String deptId = (user == null) ? null : user.getDeptId();
		// roleId 为逗号分隔角色串，用于角色可见(SHARE_ROLE)
		String roleIds = (user == null) ? null : user.getRoleId();
		// 分部口径：用户部门树最浅非零祖先，用于分部可见(SHARE_BRANCH)
		Long branchId = resolveBranchId(deptId);
		String tenantId = (user == null || user.getTenantId() == null || user.getTenantId().isBlank())
			? "000000" : user.getTenantId();

		QueryWrapper<PortalPage> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0).eq("status", 1).eq("page_type", 1); // 登录后门户
		boolean byCode = portalCode != null && !portalCode.isBlank();
		if (byCode) {
			qw.eq("page_code", portalCode.trim());
		}
		List<PortalPage> pages = pageMapper.selectList(qw.orderByAsc("sort").orderByAsc("id"));
		if (pages.isEmpty()) {
			return new MyPortalVO();
		}

		List<Long> pageIds = new ArrayList<>();
		for (PortalPage page : pages) {
			pageIds.add(page.getId());
		}
		List<PortalPageShare> shares = shareMapper.selectList(
			new QueryWrapper<PortalPageShare>().in("page_id", pageIds).eq("is_deleted", 0));

		// 授权收紧（§22 纠偏 #9/#10）：所有候选页（含默认页、机构默认页）都必须命中 share 才可见，
		// 不再让 is_default=1 或机构默认配置绕过 blade_portal_page_share 授权。
		List<PortalPage> visible = new ArrayList<>();
		for (PortalPage page : pages) {
			if (hitShare(page.getId(), shares, userId, deptId, roleIds, branchId)) {
				visible.add(page);
			}
		}
		if (visible.isEmpty()) {
			log.info("[portal] 当前用户未命中任何门户页面授权，userId={}", userId);
			return new MyPortalVO();
		}

		PortalPage target;
		if (byCode) {
			// 精确编码：该编码唯一（uk_page_code），取可见集合首个即为目标
			target = visible.get(0);
		} else {
			// landing 优先级：个人默认 > 机构默认(按部门祖先链最具体) > 全局默认 > 首个可见
			target = pickById(visible, userDefaultService.getUserDefaultPageId(userId));
			if (target == null) {
				target = pickById(visible, resolveDeptDefaultPageId(deptId));
			}
			if (target == null) {
				for (PortalPage page : visible) {
					if (isDefaultPage(page)) {
						target = page;
						break;
					}
				}
			}
			if (target == null) {
				target = visible.get(0);
			}
		}
		recordView(target.getId(), tenantId, userId);
		MyPortalVO vo = buildPortalVO(target, userId, tenantId);
		vo.setVisiblePages(toBriefList(visible));
		return vo;
	}

	/**
	 * 访问埋点：累计热度(view_count) + 当日 PV(page_stat)原子 +1 + 独立访客(visitor)去重记录。
	 * 失败仅记日志，绝不影响门户渲染。登录前门户 userId 为 null，不记 UV。
	 */
	private void recordView(Long pageId, String tenantId, Long userId) {
		if (pageId == null) {
			return;
		}
		try {
			String today = LocalDate.now().format(DATE_FMT);
			pageMapper.incrViewCount(pageId);
			pageStatMapper.upsertView(pageId, today, tenantId);
			if (userId != null) {
				pageVisitorMapper.insertIgnore(pageId, today, tenantId, userId);
			}
		} catch (Exception e) {
			log.warn("[portal] 门户页访问计数失败，pageId={}, msg={}", pageId, e.getMessage());
		}
	}

	/**
	 * 访问趋势：最近 days 天每日聚合 PV 与 UV（缺失日期补 0，保证图表连续）。
	 */
	@Override
	public List<PortalPageStatVO> dailyStats(int days) {
		BladeUser user = SecureUtil.getUser();
		String tenantId = (user == null || user.getTenantId() == null || user.getTenantId().isBlank())
			? "000000" : user.getTenantId();
		int n = Math.min(Math.max(days, 1), 180);
		LocalDate start = LocalDate.now().minusDays(n - 1);
		String fromDate = start.format(DATE_FMT);
		List<PortalPageStatVO> pvRows = pageStatMapper.dailySum(tenantId, fromDate);
		List<PortalPageStatVO> uvRows = pageVisitorMapper.dailyUv(tenantId, fromDate);
		Map<String, Long> pvMap = new HashMap<>();
		Map<String, Long> uvMap = new HashMap<>();
		for (PortalPageStatVO r : pvRows) {
			pvMap.put(r.getStatDate(), r.getViewCount() == null ? 0L : r.getViewCount());
		}
		for (PortalPageStatVO r : uvRows) {
			uvMap.put(r.getStatDate(), r.getUv() == null ? 0L : r.getUv());
		}
		List<PortalPageStatVO> result = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			String d = start.plusDays(i).format(DATE_FMT);
			PortalPageStatVO vo = new PortalPageStatVO(d, pvMap.getOrDefault(d, 0L));
			vo.setUv(uvMap.getOrDefault(d, 0L));
			result.add(vo);
		}
		return result;
	}

	/**
	 * 热门门户页：本租户访问量 Top N（按 view_count 倒序）。
	 */
	@Override
	public List<PortalPage> topPages(int limit) {
		int n = Math.min(Math.max(limit, 1), 50);
		return list().stream()
			.sorted(Comparator.comparing(
				(PortalPage p) -> p.getViewCount() == null ? 0L : p.getViewCount(),
				Comparator.reverseOrder()))
			.limit(n)
			.collect(Collectors.toList());
	}

	/**
	 * 可见页面 → 简要信息（多门户切换用）
	 */
	private List<PortalPageBrief> toBriefList(List<PortalPage> pages) {
		List<PortalPageBrief> briefs = new ArrayList<>();
		for (PortalPage p : pages) {
			PortalPageBrief b = new PortalPageBrief();
			b.setId(p.getId());
			b.setPageCode(p.getPageCode());
			b.setPageName(p.getPageName());
			b.setIsDefault(p.getIsDefault());
			briefs.add(b);
		}
		return briefs;
	}

	/**
	 * 从可见页集合里按 pageId 挑选（命中且存在才返回，否则 null）
	 */
	private PortalPage pickById(List<PortalPage> visible, Long pageId) {
		if (pageId == null) {
			return null;
		}
		for (PortalPage page : visible) {
			if (Objects.equals(page.getId(), pageId)) {
				return page;
			}
		}
		return null;
	}

	@Override
	public boolean save(PortalPageSaveDTO dto) {
		if (dto == null || dto.getPageCode() == null || dto.getPageCode().isBlank()) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();

		PortalPage page = (dto.getPageId() == null) ? null : pageMapper.selectById(dto.getPageId());
		boolean creating = (page == null);
		// 改前快照（用于审计前后值 diff）
		PortalPage old = null;
		if (!creating) {
			old = new PortalPage();
			BeanUtils.copyProperties(page, old);
		}
		if (creating) {
			page = new PortalPage();
			page.setIsDeleted(0);
			page.setStatus(1);
			page.setCreateTime(LocalDateTime.now());
			page.setCreateUser(userId);
		} else {
			page.setUpdateTime(LocalDateTime.now());
			page.setUpdateUser(userId);
		}
		page.setPageCode(dto.getPageCode().trim());
		page.setPageName(dto.getPageName());
		page.setSort(dto.getSort() == null ? 1 : dto.getSort());
		page.setTheme(dto.getTheme() == null || dto.getTheme().isBlank() ? "default" : dto.getTheme());
		page.setRemark(dto.getRemark());
		page.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
		page.setPageType(dto.getPageType() == null ? 1 : dto.getPageType());

		// 同一时刻仅允许一个默认页：保存时若置为默认，先清掉其他默认页标记
		if (page.getIsDefault() == 1) {
			clearOtherDefault(page.getId());
		}

		boolean ok = creating ? pageMapper.insert(page) > 0 : pageMapper.updateById(page) > 0;
		if (ok) {
			String type = creating ? PortalConstant.LOG_PAGE_CREATE : PortalConstant.LOG_PAGE_UPDATE;
			String summary = creating
				? ("新建门户页面 " + page.getPageCode())
				: ("编辑门户页面 " + page.getPageCode() + "；" + pageDiff(old, page));
			logService.record(type, "page", String.valueOf(page.getId()), page.getPageName(),
				summary, old == null ? null : toJson(old), toJson(page));
		}
		return ok;
	}

	@Override
	public boolean setDefault(Long pageId) {
		if (pageId == null) {
			return false;
		}
		PortalPage page = pageMapper.selectById(pageId);
		if (page == null) {
			return false;
		}
		PortalPage old = new PortalPage();
		BeanUtils.copyProperties(page, old);
		clearOtherDefault(pageId);
		page.setIsDefault(1);
		page.setUpdateTime(LocalDateTime.now());
		boolean ok = pageMapper.updateById(page) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_PAGE_DEFAULT, "page", String.valueOf(pageId),
				page.getPageName(), "设为默认门户 " + page.getPageCode(), toJson(old), toJson(page));
		}
		return ok;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean remove(Long pageId) {
		if (pageId == null) {
			return false;
		}
		PortalPage page = pageMapper.selectById(pageId);
		if (page == null) {
			return false;
		}
		String beforeJson = toJson(page);
		String pageName = page.getPageName();
		String pageCode = page.getPageCode();
		page.setIsDeleted(1);
		page.setStatus(-1);
		page.setUpdateTime(LocalDateTime.now());
		pageMapper.updateById(page);

		// 级联逻辑删除：元素 → 个性化 → 授权
		List<PortalElement> elements = elementMapper.selectList(
			new QueryWrapper<PortalElement>().eq("page_id", pageId).eq("is_deleted", 0));
		if (!elements.isEmpty()) {
			List<Long> elementIds = new ArrayList<>();
			for (PortalElement element : elements) {
				elementIds.add(element.getId());
			}
			PortalElement gone = new PortalElement();
			gone.setIsDeleted(1);
			gone.setStatus(-1);
			elementMapper.update(gone, new QueryWrapper<PortalElement>().eq("page_id", pageId).eq("is_deleted", 0));

			PortalElementSetting settingGone = new PortalElementSetting();
			settingGone.setIsDeleted(1);
			settingGone.setStatus(-1);
			settingMapper.update(settingGone,
				new QueryWrapper<PortalElementSetting>().in("element_id", elementIds).eq("is_deleted", 0));
		}
		PortalPageShare shareGone = new PortalPageShare();
		shareGone.setIsDeleted(1);
		shareGone.setStatus(-1);
		shareMapper.update(shareGone, new QueryWrapper<PortalPageShare>().eq("page_id", pageId).eq("is_deleted", 0));

		logService.record(PortalConstant.LOG_PAGE_DELETE, "page", String.valueOf(pageId),
			pageName, "删除门户页面 " + pageCode, beforeJson, null);
		return true;
	}

	@Override
	public PortalPageExportVO exportPage(Long pageId) {
		if (pageId == null) {
			return null;
		}
		PortalPage page = pageMapper.selectById(pageId);
		if (page == null || Integer.valueOf(1).equals(page.getIsDeleted())) {
			return null;
		}
		List<PortalElement> elements = elementMapper.selectList(
			new QueryWrapper<PortalElement>().eq("page_id", pageId).eq("is_deleted", 0).orderByAsc("id"));
		List<PortalPageShare> shares = shareMapper.selectList(
			new QueryWrapper<PortalPageShare>().eq("page_id", pageId).eq("is_deleted", 0));
		PortalPageExportVO vo = new PortalPageExportVO();
		vo.setPage(page);
		vo.setElements(elements);
		vo.setShares(shares);
		return vo;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Long importPage(PortalPageExportVO vo) {
		if (vo == null || vo.getPage() == null) {
			return null;
		}
		try {
			return doImportPage(vo);
		} catch (Exception e) {
			// 失败审计（REQUIRES_NEW 独立事务，回滚后仍可落库；对标 PortalLogInfo 失败记录）
			logService.recordFail(PortalConstant.LOG_PAGE_IMPORT_FAIL, "page",
				vo.getPage() == null ? null : vo.getPage().getPageCode(),
				"导入门户页面失败", e.getMessage());
			throw e;
		}
	}

	private Long doImportPage(PortalPageExportVO vo) {
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();

		PortalPage src = vo.getPage();
		String baseCode = (src.getPageCode() == null) ? "" : src.getPageCode().trim();
		if (baseCode.isBlank()) {
			baseCode = "import_" + System.currentTimeMillis();
		}
		String code = uniqueCode(baseCode);

		PortalPage page = new PortalPage();
		page.setTenantId(tenantId);
		page.setPageCode(code);
		page.setPageName(src.getPageName() == null ? code : src.getPageName());
		page.setIsDefault(0);
		page.setSort(src.getSort() == null ? 1 : src.getSort());
		page.setTheme(src.getTheme() == null || src.getTheme().isBlank() ? "default" : src.getTheme());
		page.setRemark("导入自 " + (src.getPageCode() == null ? "" : src.getPageCode()));
		page.setIsDeleted(0);
		page.setStatus(1);
		page.setCreateTime(now);
		page.setCreateUser(userId);
		pageMapper.insert(page);
		Long newPageId = page.getId();

		if (vo.getElements() != null) {
			for (PortalElement e : vo.getElements()) {
				PortalElement ne = new PortalElement();
				ne.setTenantId(tenantId);
				ne.setPageId(newPageId);
				ne.setElementType(e.getElementType());
				ne.setElementName(e.getElementName());
				ne.setShowTitle(e.getShowTitle());
				ne.setRowNo(e.getRowNo());
				ne.setColSpan(e.getColSpan());
				ne.setSortOrder(e.getSortOrder());
				ne.setConfigJson(e.getConfigJson());
				ne.setStyleJson(e.getStyleJson());
				ne.setIsDeleted(0);
				ne.setStatus(1);
				ne.setCreateTime(now);
				ne.setCreateUser(userId);
				elementMapper.insert(ne);
			}
		}
		if (vo.getShares() != null) {
			for (PortalPageShare s : vo.getShares()) {
				PortalPageShare ns = new PortalPageShare();
				ns.setTenantId(tenantId);
				ns.setPageId(newPageId);
				ns.setShareType(s.getShareType());
				ns.setShareId(s.getShareId());
				ns.setIsDeleted(0);
				ns.setStatus(1);
				ns.setCreateTime(now);
				ns.setCreateUser(userId);
				shareMapper.insert(ns);
			}
		}
		logService.record(PortalConstant.LOG_PAGE_IMPORT, "page", String.valueOf(newPageId),
			page.getPageName(), "导入门户页面 " + code + "（元素 "
				+ (vo.getElements() == null ? 0 : vo.getElements().size())
				+ " 个，授权 " + (vo.getShares() == null ? 0 : vo.getShares().size()) + " 条）",
			null, toJson(page));
		return newPageId;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long duplicate(Long pageId) {
		if (pageId == null) {
			return null;
		}
		PortalPageExportVO vo = exportPage(pageId);
		if (vo == null || vo.getPage() == null) {
			return null;
		}
		// 副本编码加 _copy 后缀（importPage 内部保证唯一）、名称加「副本」、不抢占默认页
		vo.getPage().setPageCode(vo.getPage().getPageCode() + "_copy");
		vo.getPage().setPageName(vo.getPage().getPageName() + " 副本");
		vo.getPage().setIsDefault(0);
		if (vo.getPage().getPageType() == null) {
			vo.getPage().setPageType(1);
		}
		return importPage(vo);
	}

	@Override
	public MyPortalVO getPreLoginPortal(String portalCode) {
		QueryWrapper<PortalPage> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0).eq("status", 1).eq("page_type", 0); // 登录前门户
		if (portalCode != null && !portalCode.isBlank()) {
			qw.eq("page_code", portalCode.trim());
		}
		List<PortalPage> pages = pageMapper.selectList(qw.orderByAsc("sort").orderByAsc("id"));
		if (pages.isEmpty()) {
			return new MyPortalVO();
		}
		// 登录前无登录用户：不做个性化、仅页面级共享元素(owner=0)，主题取页面 theme + 租户默认
		recordView(pages.get(0).getId(), "000000", null);
		return buildPortalVO(pages.get(0), null, "000000");
	}

	@Override
	public List<PortalPageShare> listShare(Long pageId) {
		if (pageId == null) {
			return new ArrayList<>();
		}
		return shareMapper.selectList(new QueryWrapper<PortalPageShare>()
			.eq("page_id", pageId).eq("is_deleted", 0)
			.orderByAsc("share_type").orderByAsc("share_id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveShare(Long pageId, List<PortalPageShare> shares) {
		if (pageId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();

		// 归一化目标集合：去非法项、全员(1)强制 share_id=0、按 type:id 去重
		List<String> wantKeys = new ArrayList<>();
		List<PortalPageShare> normalized = new ArrayList<>();
		if (shares != null) {
			for (PortalPageShare s : shares) {
				if (s == null || s.getShareType() == null) {
					continue;
				}
				int type = s.getShareType();
				if (type != PortalConstant.SHARE_ALL && type != PortalConstant.SHARE_DEPT
					&& type != PortalConstant.SHARE_USER && type != PortalConstant.SHARE_ROLE
					&& type != PortalConstant.SHARE_BRANCH) {
					continue;
				}
				long shareId = (type == PortalConstant.SHARE_ALL)
					? 0L : (s.getShareId() == null ? 0L : s.getShareId());
				if (type != PortalConstant.SHARE_ALL && shareId <= 0) {
					continue;
				}
				String key = type + ":" + shareId;
				if (wantKeys.contains(key)) {
					continue;
				}
				wantKeys.add(key);
				PortalPageShare ns = new PortalPageShare();
				ns.setShareType(type);
				ns.setShareId(shareId);
				normalized.add(ns);
			}
		}

		List<PortalPageShare> existing = shareMapper.selectList(
			new QueryWrapper<PortalPageShare>().eq("page_id", pageId).eq("is_deleted", 0));

		Set<String> existKeys = new HashSet<>();
		for (PortalPageShare e : existing) {
			existKeys.add((e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId()));
		}

		// 差量删除：存在但不在目标集合
		int removed = 0;
		for (PortalPageShare e : existing) {
			String key = (e.getShareType() == null ? 0 : e.getShareType()) + ":"
				+ (e.getShareId() == null ? 0L : e.getShareId());
			if (!wantKeys.contains(key)) {
				PortalPageShare gone = new PortalPageShare();
				gone.setIsDeleted(1);
				gone.setStatus(-1);
				gone.setUpdateUser(userId);
				gone.setUpdateTime(now);
				shareMapper.update(gone, new QueryWrapper<PortalPageShare>().eq("id", e.getId()));
				removed++;
			}
		}

		// 差量新增：目标集合里尚不存在的
		int added = 0;
		for (PortalPageShare s : normalized) {
			String key = s.getShareType() + ":" + s.getShareId();
			if (existKeys.contains(key)) {
				continue;
			}
			PortalPageShare ns = new PortalPageShare();
			ns.setTenantId(tenantId);
			ns.setPageId(pageId);
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

		logService.record(PortalConstant.LOG_PAGE_UPDATE, "page", String.valueOf(pageId),
			null, "更新页面可见范围：新增 " + added + " 条，移除 " + removed + " 条", null, null);
		return true;
	}

	/**
	 * 生成唯一页面编码（库内已存在则追加 _N 后缀）
	 */
	private String uniqueCode(String base) {
		String candidate = base;
		int i = 1;
		while (pageMapper.selectOne(
			new QueryWrapper<PortalPage>().eq("page_code", candidate).eq("is_deleted", 0)) != null) {
			candidate = base + "_" + i;
			i++;
		}
		return candidate;
	}

	/* ==================== 私有方法 ==================== */

	/**
	 * 组装门户 VO（页面信息 + 合并用户个性化后的元素）
	 */
	private MyPortalVO buildPortalVO(PortalPage page, Long userId, String tenantId) {
		MyPortalVO vo = new MyPortalVO();
		vo.setPageId(page.getId());
		vo.setPageCode(page.getPageCode());
		vo.setPageName(page.getPageName());
		vo.setTheme(page.getTheme());
		vo.setRemark(page.getRemark());
		vo.setResolvedTheme(themeService.resolveTheme(page.getTheme(), userId, tenantId));
		vo.setElements(buildElements(page.getId(), userId));
		return vo;
	}

	/**
	 * 解析机构默认首页：取用户部门祖先链中"最具体"（最靠近本部门）的默认页。
	 * <p>配在父部门时，其子孙部门用户同样生效。</p>
	 *
	 * @param deptId 当前用户部门ID（String）
	 * @return 命中的 pageId；无命中返回 null
	 */
	private Long resolveDeptDefaultPageId(String deptId) {
		Long deptIdLong = toLongOrNull(deptId);
		if (deptIdLong == null) {
			return null;
		}
		// 候选链：[本部门, 祖先从深到浅]（排除根 0）
		List<Long> chain = new ArrayList<>();
		chain.add(deptIdLong);
		String ancestors = deptMapper.getAncestors(deptIdLong);
		if (ancestors != null && !ancestors.isBlank()) {
			String[] parts = ancestors.split(",");
			for (int i = parts.length - 1; i >= 0; i--) {
				String p = parts[i].trim();
				if (p.isEmpty() || "0".equals(p)) {
					continue;
				}
				Long aid = toLongOrNull(p);
				if (aid != null && !chain.contains(aid)) {
					chain.add(aid);
				}
			}
		}
		List<PortalDefault> defs = defaultMapper.selectList(
			new QueryWrapper<PortalDefault>().in("dept_id", chain).eq("is_deleted", 0));
		if (defs.isEmpty()) {
			return null;
		}
		for (Long d : chain) {
			for (PortalDefault def : defs) {
				if (Objects.equals(def.getDeptId(), d)) {
					return def.getPageId();
				}
			}
		}
		return null;
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

	/**
	 * 实体转 JSON 快照（失败降级为 null，不影响审计主流程）
	 */
	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 审计快照序列化失败：{}", e.getMessage());
			return null;
		}
	}

	/**
	 * 页面变更字段摘要（旧 -> 新）
	 */
	private String pageDiff(PortalPage old, PortalPage now) {
		if (old == null) {
			return "新建";
		}
		List<String> changed = new ArrayList<>();
		if (!Objects.equals(old.getPageName(), now.getPageName())) {
			changed.add("pageName");
		}
		if (!Objects.equals(old.getTheme(), now.getTheme())) {
			changed.add("theme");
		}
		if (!Objects.equals(old.getIsDefault(), now.getIsDefault())) {
			changed.add("isDefault");
		}
		if (!Objects.equals(old.getSort(), now.getSort())) {
			changed.add("sort");
		}
		if (!Objects.equals(old.getRemark(), now.getRemark())) {
			changed.add("remark");
		}
		return changed.isEmpty() ? "无字段变化" : ("变更: " + String.join(", ", changed));
	}

	/**
	 * 构建页面元素 VO 列表（合并当前用户个性化）
	 */
	private List<PortalElementVO> buildElements(Long pageId, Long userId) {
		List<PortalElement> elements = elementMapper.selectList(
			new QueryWrapper<PortalElement>()
				.eq("page_id", pageId)
				.eq("is_deleted", 0)
				.eq("status", 1)
				// 页面级共享元素(owner=0) + 当前用户私有元素(owner=userId)，对标 UserAddElementInfo
				.and(w -> {
					w.eq("owner_user_id", 0);
					if (userId != null) {
						w.or().eq("owner_user_id", userId);
					}
				})
				.orderByAsc("row_no")
				.orderByAsc("sort_order")
				.orderByAsc("id"));
		if (elements.isEmpty()) {
			return new ArrayList<>();
		}

		List<Long> elementIds = new ArrayList<>();
		for (PortalElement element : elements) {
			elementIds.add(element.getId());
		}
		Map<Long, PortalElementSetting> settingMap = new LinkedHashMap<>();
		if (userId != null) {
			List<PortalElementSetting> settings = settingMapper.selectList(
				new QueryWrapper<PortalElementSetting>()
					.in("element_id", elementIds)
					.eq("user_id", userId)
					.eq("is_deleted", 0));
			for (PortalElementSetting setting : settings) {
				settingMap.put(setting.getElementId(), setting);
			}
		}

		// 用户个人布局覆盖（对标 hpcurrentuse）
		Map<Long, PortalLayoutItem> layoutMap = userLayoutService.getLayoutMap(userId, pageId);

		List<PortalElementVO> result = new ArrayList<>();
		for (PortalElement element : elements) {
			PortalElementVO vo = new PortalElementVO();
			vo.setId(element.getId());
			vo.setOwnerUserId(element.getOwnerUserId());
			vo.setElementType(element.getElementType());
			vo.setElementName(element.getElementName());
			vo.setRowNo(element.getRowNo());
			vo.setColSpan(element.getColSpan());
			vo.setSortOrder(element.getSortOrder());
			vo.setConfig(parseMap(element.getConfigJson()));

			Map<String, Object> style = parseMap(element.getStyleJson());
			int perPage = PortalConstant.DEFAULT_PER_PAGE;
			Integer showTitle = element.getShowTitle();

			PortalElementSetting setting = settingMap.get(element.getId());
			if (setting != null) {
				if (setting.getPerPage() != null && setting.getPerPage() > 0) {
					perPage = setting.getPerPage();
				}
				if (setting.getShowTitle() != null) {
					showTitle = setting.getShowTitle();
				}
				Map<String, Object> userStyle = parseMap(setting.getStyleJson());
				if (!userStyle.isEmpty()) {
					style.putAll(userStyle);
				}
			}
			vo.setStyle(style);
			vo.setShowTitle(showTitle);
			vo.setPerPage(perPage);

			// 应用用户个人布局覆盖（显隐/行/栅格/排序，对标 hpcurrentuse §18.2）
			PortalLayoutItem layout = layoutMap.get(element.getId());
			if (layout != null) {
				if (layout.getHidden() != null && layout.getHidden() == 1) {
					continue;
				}
				if (layout.getRowNo() != null && layout.getRowNo() > 0) {
					vo.setRowNo(layout.getRowNo());
				}
				if (layout.getColSpan() != null && layout.getColSpan() > 0) {
					vo.setColSpan(layout.getColSpan());
				}
				if (layout.getSortOrder() != null) {
					vo.setSortOrder(layout.getSortOrder());
				}
			}
			result.add(vo);
		}
		// 个性化后按 (行, 排序, id) 重排
		result.sort(java.util.Comparator
			.comparingInt((PortalElementVO v) -> v.getRowNo() == null ? 1 : v.getRowNo())
			.thenComparingInt(v -> v.getSortOrder() == null ? 0 : v.getSortOrder())
			.thenComparingLong(v -> v.getId() == null ? 0L : v.getId()));
		return result;
	}

	/**
	 * 解析配置 JSON（失败降级为空 Map，不影响整页渲染）
	 */
	private Map<String, Object> parseMap(String json) {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> map = JsonUtil.parse(json, Map.class);
			return (map == null) ? new LinkedHashMap<>() : map;
		} catch (Exception e) {
			log.warn("[portal] 解析配置JSON失败，降级为空对象，msg={}", e.getMessage());
			return new LinkedHashMap<>();
		}
	}

	/**
	 * 是否默认页
	 */
	private boolean isDefaultPage(PortalPage page) {
		return page.getIsDefault() != null && page.getIsDefault() == 1;
	}

	/**
	 * 页面是否命中授权（全员/本部门/本人/本角色）
	 */
	private boolean hitShare(Long pageId, List<PortalPageShare> shares, Long userId, String deptId, String roleIds, Long branchId) {
		for (PortalPageShare share : shares) {
			if (!Objects.equals(share.getPageId(), pageId)) {
				continue;
			}
			int type = share.getShareType() == null ? 0 : share.getShareType();
			if (type == PortalConstant.SHARE_ALL) {
				return true;
			}
			if (type == PortalConstant.SHARE_DEPT) {
				// share_id 为 bigint，deptId 为 String，统一按字符串比对避免类型不一致
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

	/**
	 * 解析用户所属分部：部门树最浅的非零祖先；自身即顶层部门（无祖先链）时分部=自身。
	 * 对标泛微 shareinnerhp type=6 分部（SpringBlade 无独立分部表，以顶层部门为分部口径）。
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
					return aid; // ancestors 由浅到深，首个非零即分部
				}
			}
		}
		return deptIdLong;
	}

	/**
	 * 角色命中：roleIds 为逗号分隔角色串，命中 shareId 即true
	 */
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
	 * 清除其他页面的默认标记（保证唯一默认页）
	 */
	private void clearOtherDefault(Long keepPageId) {
		PortalPage upd = new PortalPage();
		upd.setIsDefault(0);
		QueryWrapper<PortalPage> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0).eq("is_default", 1);
		if (keepPageId != null) {
			qw.ne("id", keepPageId);
		}
		pageMapper.update(upd, qw);
	}
}
