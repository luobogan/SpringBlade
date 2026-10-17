package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalThemeSaveDTO;
import org.springblade.portal.entity.BladeUserBrief;
import org.springblade.portal.entity.PortalTheme;
import org.springblade.portal.entity.PortalUserTheme;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.BladeUserMapper;
import org.springblade.portal.mapper.PortalThemeMapper;
import org.springblade.portal.mapper.PortalUserThemeMapper;
import org.springblade.portal.service.IPortalThemeService;
import org.springblade.portal.vo.ThemeUserVO;
import org.springblade.portal.vo.ThemeVO;
import org.springblade.portal.vo.UserThemeInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 门户主题服务实现（对标泛微 themestyle / themecenter）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalThemeServiceImpl implements IPortalThemeService {

	private static final java.time.format.DateTimeFormatter FMT =
		java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final PortalThemeMapper themeMapper;
	private final PortalUserThemeMapper userThemeMapper;
	private final BladeUserMapper bladeUserMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalTheme> list() {
		QueryWrapper<PortalTheme> qw = new QueryWrapper<>();
		qw.eq("tenant_id", tenantOf(SecureUtil.getUser())).eq("is_deleted", 0).orderByAsc("id");
		return themeMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long save(PortalThemeSaveDTO dto) {
		if (dto == null || isBlank(dto.getThemeName()) || isBlank(dto.getThemeKey())) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = tenantOf(user);
		Long userId = userIdOf(user);
		LocalDateTime now = LocalDateTime.now();

		boolean creating = (dto.getThemeId() == null);
		PortalTheme theme;
		if (creating) {
			PortalTheme exist = themeMapper.selectOne(new QueryWrapper<PortalTheme>()
				.eq("tenant_id", tenantId).eq("theme_key", dto.getThemeKey().trim()).eq("is_deleted", 0));
			if (exist != null) {
				return null; // 同租户 theme_key 冲突
			}
			theme = new PortalTheme();
			theme.setTenantId(tenantId);
			theme.setIsDeleted(0);
			theme.setStatus(1);
			theme.setCreateTime(now);
			theme.setCreateUser(userId);
		} else {
			theme = themeMapper.selectById(dto.getThemeId());
			if (theme == null) {
				return null;
			}
		}
		theme.setThemeName(dto.getThemeName().trim());
		theme.setThemeKey(dto.getThemeKey().trim());
		theme.setThemeConfig(JsonUtil.toJson(dto.getThemeConfig()));
		theme.setOpenTimer(dto.getOpenTimer() == null ? 0 : dto.getOpenTimer());
		theme.setTimerStart(dto.getTimerStart());
		theme.setTimerEnd(dto.getTimerEnd());
		theme.setUpdateTime(now);
		theme.setUpdateUser(userId);
		if (creating) {
			themeMapper.insert(theme);
		} else {
			themeMapper.updateById(theme);
		}
		return theme.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setDefault(Long themeId) {
		if (themeId == null) {
			return false;
		}
		PortalTheme theme = themeMapper.selectById(themeId);
		if (theme == null) {
			return false;
		}
		String tenantId = theme.getTenantId();
		PortalTheme reset = new PortalTheme();
		reset.setIsDefault(0);
		themeMapper.update(reset, new QueryWrapper<PortalTheme>()
			.eq("tenant_id", tenantId).eq("is_deleted", 0).eq("is_default", 1));
		theme.setIsDefault(1);
		theme.setUpdateTime(LocalDateTime.now());
		return themeMapper.updateById(theme) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long themeId) {
		if (themeId == null) {
			return false;
		}
		PortalTheme theme = themeMapper.selectById(themeId);
		if (theme == null || Integer.valueOf(1).equals(theme.getIsDefault())) {
			return false; // 不存在或默认主题不可删
		}
		theme.setIsDeleted(1);
		theme.setStatus(-1);
		theme.setUpdateTime(LocalDateTime.now());
		themeMapper.updateById(theme);
		// 清理用户偏好指向该主题的记录
		userThemeMapper.delete(new QueryWrapper<PortalUserTheme>().eq("theme_id", themeId));
		return true;
	}

	@Override
	public Long getUserThemeId(Long userId) {
		if (userId == null) {
			return null;
		}
		PortalUserTheme ut = userThemeMapper.selectOne(new QueryWrapper<PortalUserTheme>().eq("user_id", userId));
		return ut == null ? null : ut.getThemeId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setUserTheme(Long userId, Long themeId) {
		if (userId == null || themeId == null) {
			return false;
		}
		LocalDateTime now = LocalDateTime.now();
		PortalUserTheme ut = userThemeMapper.selectOne(new QueryWrapper<PortalUserTheme>().eq("user_id", userId));
		if (ut == null) {
			ut = new PortalUserTheme();
			ut.setTenantId(tenantOf(SecureUtil.getUser()));
			ut.setUserId(userId);
			ut.setThemeId(themeId);
			ut.setCreateTime(now);
			ut.setUpdateTime(now);
			userThemeMapper.insert(ut);
		} else {
			ut.setThemeId(themeId);
			ut.setUpdateTime(now);
			userThemeMapper.updateById(ut);
		}
		return true;
	}

	@Override
	public ThemeVO resolveTheme(String pageThemeKey, Long userId, String tenantId) {
		try {
			if (isBlank(tenantId)) {
				tenantId = "000000";
			}
			PortalTheme chosen = null;
			// 1) 用户偏好优先
			if (userId != null) {
				Long uid = getUserThemeId(userId);
				if (uid != null) {
					chosen = themeMapper.selectById(uid);
				}
			}
			// 2) 页面 theme_key 命中
			if (chosen == null && !isBlank(pageThemeKey)) {
				chosen = themeMapper.selectOne(new QueryWrapper<PortalTheme>()
					.eq("tenant_id", tenantId).eq("theme_key", pageThemeKey.trim()).eq("is_deleted", 0).eq("status", 1));
			}
			// 3) 定时生效主题（open_timer=1 且当前在 [start,end]，拉模式；优先于租户默认，§16.1/§18.2）
			if (chosen == null) {
				String now = LocalDateTime.now().format(FMT);
				List<PortalTheme> timerThemes = themeMapper.selectList(new QueryWrapper<PortalTheme>()
					.eq("tenant_id", tenantId).eq("is_deleted", 0).eq("status", 1).eq("open_timer", 1));
				for (PortalTheme t : timerThemes) {
					if (inWindow(now, t.getTimerStart(), t.getTimerEnd())) {
						chosen = t;
						break;
					}
				}
			}
			// 4) 租户默认主题
			if (chosen == null) {
				chosen = themeMapper.selectOne(new QueryWrapper<PortalTheme>()
					.eq("tenant_id", tenantId).eq("is_default", 1).eq("is_deleted", 0).eq("status", 1));
			}
			if (chosen == null) {
				return null;
			}
			ThemeVO vo = new ThemeVO();
			vo.setThemeKey(chosen.getThemeKey());
			vo.setThemeName(chosen.getThemeName());
			vo.setThemeConfig(parseMap(chosen.getThemeConfig()));
			return vo;
		} catch (Exception e) {
			// 主题表尚未初始化（迁移未执行）时降级为「无主题」，不影响门户主流程
			log.warn("[portal] 解析主题失败（主题表可能未初始化），降级无主题，msg={}", e.getMessage());
			return null;
		}
	}

	/* ==================== 批量指定（themeappoint §15.5 A3） ==================== */

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int appointByScope(Long themeId, int scopeType, List<Long> scopeIds) {
		if (themeId == null) {
			return -1;
		}
		PortalTheme theme = themeMapper.selectById(themeId);
		if (theme == null || Integer.valueOf(1).equals(theme.getIsDeleted())) {
			return -1;
		}
		if (scopeType != PortalConstant.SHARE_ALL && scopeType != PortalConstant.SHARE_DEPT
			&& scopeType != PortalConstant.SHARE_USER && scopeType != PortalConstant.SHARE_ROLE) {
			return -1;
		}
		if (scopeType != PortalConstant.SHARE_ALL && (scopeIds == null || scopeIds.isEmpty())) {
			return -1;
		}
		List<Long> userIds = bladeUserMapper.selectIdsByScope(theme.getTenantId(), scopeType, scopeIds);
		if (userIds == null || userIds.isEmpty()) {
			return 0;
		}
		int updated = 0;
		for (Long uid : userIds) {
			if (uid == null) {
				continue;
			}
			if (upsertUserTheme(uid, themeId)) {
				updated++;
			}
		}
		logService.record(PortalConstant.LOG_THEME_APPOINT, "theme", String.valueOf(themeId),
			theme.getThemeName(), "按范围批量指定主题「" + theme.getThemeName() + "」给 " + updated
			+ " 个用户（scopeType=" + scopeType + "）", null, null);
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int migrateTheme(Long fromThemeId, Long toThemeId) {
		if (fromThemeId == null || toThemeId == null || Objects.equals(fromThemeId, toThemeId)) {
			return -1;
		}
		PortalTheme from = themeMapper.selectById(fromThemeId);
		PortalTheme to = themeMapper.selectById(toThemeId);
		if (from == null || to == null || Integer.valueOf(1).equals(to.getIsDeleted())) {
			return -1;
		}
		List<PortalUserTheme> rows = userThemeMapper.selectList(new QueryWrapper<PortalUserTheme>()
			.eq("theme_id", fromThemeId));
		LocalDateTime now = LocalDateTime.now();
		for (PortalUserTheme ut : rows) {
			ut.setThemeId(toThemeId);
			ut.setUpdateTime(now);
			userThemeMapper.updateById(ut);
		}
		logService.record(PortalConstant.LOG_THEME_APPOINT, "theme", String.valueOf(toThemeId),
			to.getThemeName(), "主题整体迁移：「" + from.getThemeName() + "」→「" + to.getThemeName()
			+ "」，共 " + rows.size() + " 个用户", null, null);
		return rows.size();
	}

	/* ==================== 使用查询（themesearch §15.5 A4） ==================== */

	@Override
	public IPage<ThemeUserVO> listThemeUsers(Long themeId, long current, long size) {
		Page<ThemeUserVO> page = new Page<>(Math.max(current, 1), Math.min(Math.max(size, 1), 100));
		if (themeId == null) {
			return page;
		}
		List<PortalUserTheme> rows = userThemeMapper.selectList(new QueryWrapper<PortalUserTheme>()
			.eq("theme_id", themeId).orderByAsc("id"));
		long total = rows.size();
		page.setTotal(total);
		int from = (int) ((page.getCurrent() - 1) * page.getSize());
		if (from >= total) {
			return page;
		}
		int to = (int) Math.min(from + page.getSize(), total);
		List<PortalUserTheme> slice = rows.subList(from, to);
		List<Long> userIds = slice.stream().map(PortalUserTheme::getUserId).filter(Objects::nonNull).collect(Collectors.toList());
		Map<Long, BladeUserBrief> userMap = new LinkedHashMap<>();
		if (!userIds.isEmpty()) {
			for (BladeUserBrief u : bladeUserMapper.selectBriefByIds(userIds)) {
				if (u != null && u.getId() != null) {
					userMap.put(u.getId(), u);
				}
			}
		}
		List<ThemeUserVO> vos = new ArrayList<>();
		for (PortalUserTheme ut : slice) {
			ThemeUserVO vo = new ThemeUserVO();
			vo.setUserId(ut.getUserId());
			vo.setUpdateTime(ut.getUpdateTime() == null ? null
				: ut.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			BladeUserBrief u = userMap.get(ut.getUserId());
			if (u != null) {
				vo.setAccount(u.getAccount());
				vo.setRealName(u.getRealName());
				vo.setDeptId(u.getDeptId());
			}
			vos.add(vo);
		}
		page.setRecords(vos);
		return page;
	}

	@Override
	public UserThemeInfoVO getUserThemeInfo(Long userId) {
		UserThemeInfoVO vo = new UserThemeInfoVO();
		if (userId == null) {
			return vo;
		}
		vo.setUserId(userId);
		BladeUserBrief u = bladeUserMapper.selectBriefById(userId);
		if (u != null) {
			vo.setAccount(u.getAccount());
			vo.setRealName(u.getRealName());
			vo.setDeptId(u.getDeptId());
		}
		PortalUserTheme ut = userThemeMapper.selectOne(new QueryWrapper<PortalUserTheme>().eq("user_id", userId));
		if (ut != null && ut.getThemeId() != null) {
			vo.setHasExplicit(true);
			vo.setThemeId(ut.getThemeId());
			PortalTheme t = themeMapper.selectById(ut.getThemeId());
			if (t != null) {
				vo.setThemeName(t.getThemeName());
			}
		} else {
			vo.setHasExplicit(false);
		}
		// 生效主题（按 resolveTheme 优先级：偏好>定时>默认；页面维度不适用）
		ThemeVO effective = resolveTheme(null, userId, tenantOf(SecureUtil.getUser()));
		vo.setEffectiveThemeName(effective == null ? null : effective.getThemeName());
		return vo;
	}

	/** upsert 用户主题偏好（供 setUserTheme/appointByScope 共用） */
	private boolean upsertUserTheme(Long userId, Long themeId) {
		LocalDateTime now = LocalDateTime.now();
		PortalUserTheme ut = userThemeMapper.selectOne(new QueryWrapper<PortalUserTheme>().eq("user_id", userId));
		if (ut == null) {
			ut = new PortalUserTheme();
			ut.setTenantId(tenantOf(SecureUtil.getUser()));
			ut.setUserId(userId);
			ut.setThemeId(themeId);
			ut.setCreateTime(now);
			ut.setUpdateTime(now);
			return userThemeMapper.insert(ut) > 0;
		}
		if (Objects.equals(ut.getThemeId(), themeId)) {
			return false; // 已指向目标主题，不重复计数
		}
		ut.setThemeId(themeId);
		ut.setUpdateTime(now);
		return userThemeMapper.updateById(ut) > 0;
	}

	/**
	 * 当前时间是否落在定时窗口 [start,end]（统一格式 yyyy-MM-dd HH:mm:ss，字典序即时间序）
	 */
	private boolean inWindow(String now, String start, String end) {
		if (isBlank(start) || isBlank(end)) {
			return false;
		}
		return now.compareTo(start.trim()) >= 0 && now.compareTo(end.trim()) <= 0;
	}

	private Map<String, Object> parseMap(String json) {
		if (isBlank(json)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> map = JsonUtil.parse(json, Map.class);
			return map == null ? new LinkedHashMap<>() : map;
		} catch (Exception e) {
			log.warn("[portal] 解析主题配置失败，降级空对象，msg={}", e.getMessage());
			return new LinkedHashMap<>();
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private String tenantOf(BladeUser user) {
		if (user != null && !isBlank(user.getTenantId())) {
			return user.getTenantId();
		}
		return "000000";
	}

	private Long userIdOf(BladeUser user) {
		return user == null ? null : user.getUserId();
	}
}
