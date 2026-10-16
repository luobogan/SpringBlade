package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.dto.PortalThemeSaveDTO;
import org.springblade.portal.entity.PortalTheme;
import org.springblade.portal.entity.PortalUserTheme;
import org.springblade.portal.mapper.PortalThemeMapper;
import org.springblade.portal.mapper.PortalUserThemeMapper;
import org.springblade.portal.service.IPortalThemeService;
import org.springblade.portal.vo.ThemeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
