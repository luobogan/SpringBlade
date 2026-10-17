package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalAccountSettingSaveDTO;
import org.springblade.portal.entity.PortalAccountSetting;
import org.springblade.portal.entity.PortalSetting;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalAccountSettingMapper;
import org.springblade.portal.service.IPortalAccountSettingService;
import org.springblade.portal.service.IPortalSettingService;
import org.springblade.portal.vo.AccountMoreVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户个人通用设置服务实现（对标泛微 accountmore §14.2 I）
 * <p>生效值优先级：个人值 &gt; 全局默认（对标 GetMyThemeCmd「个人 > 组织 > 全局」）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAccountSettingServiceImpl implements IPortalAccountSettingService {

	private final PortalAccountSettingMapper accountSettingMapper;
	private final IPortalSettingService settingService;
	private final IPortalLogService logService;

	@Override
	public PortalAccountSetting get(Long userId) {
		if (userId == null) {
			return null;
		}
		return accountSettingMapper.selectOne(new QueryWrapper<PortalAccountSetting>()
			.eq("user_id", userId).last("limit 1"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(Long userId, PortalAccountSettingSaveDTO dto) {
		if (userId == null || dto == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();
		String menuConfigJson = toJson(dto.getMenuConfig());

		PortalAccountSetting exist = accountSettingMapper.selectOne(
			new QueryWrapper<PortalAccountSetting>().eq("user_id", userId).last("limit 1"));
		boolean ok;
		if (exist == null) {
			PortalAccountSetting fresh = new PortalAccountSetting();
			fresh.setTenantId(tenantId);
			fresh.setUserId(userId);
			fresh.setFontSize(dto.getFontSize());
			fresh.setLayoutType(dto.getLayoutType());
			fresh.setMenuConfigJson(menuConfigJson);
			fresh.setIsDeleted(0);
			fresh.setStatus(1);
			fresh.setCreateUser(userId);
			fresh.setCreateTime(now);
			fresh.setUpdateUser(userId);
			fresh.setUpdateTime(now);
			ok = accountSettingMapper.insert(fresh) > 0;
		} else {
			// 显式 set：支持把个人值清空（NULL）回退到全局默认
			UpdateWrapper<PortalAccountSetting> uw = new UpdateWrapper<>();
			uw.set("font_size", dto.getFontSize())
				.set("layout_type", dto.getLayoutType())
				.set("menu_config_json", menuConfigJson)
				.set("is_deleted", 0)
				.set("status", 1)
				.set("update_user", userId)
				.set("update_time", now)
				.eq("id", exist.getId());
			ok = accountSettingMapper.update(null, uw) >= 0;
		}
		if (ok) {
			logService.record(PortalConstant.LOG_ACCOUNT_SETTING, "account", String.valueOf(userId),
				"个人通用设置", "fontSize=" + dto.getFontSize() + ", layoutType=" + dto.getLayoutType(),
				null, null);
		}
		return ok;
	}

	@Override
	public AccountMoreVO getEffective(Long userId) {
		AccountMoreVO vo = new AccountMoreVO();
		PortalAccountSetting personal = get(userId);
		PortalSetting global = settingService.get();

		vo.setGlobalFontSize(global.getFontSize());
		vo.setGlobalLayoutType(global.getLayoutType());
		vo.setQuickSearchEnabled(global.getQuickSearchEnabled());
		vo.setHiddenMyPortal(global.getHiddenMyPortal());
		vo.setShowFreqMenu(global.getShowFreqMenu());
		vo.setDefaultExpandLeft(global.getDefaultExpandLeft());

		// 个人值优先，NULL 回退全局（对标 accountmore 缺省回退）
		vo.setFontSize(personal != null && personal.getFontSize() != null
			? personal.getFontSize() : global.getFontSize());
		vo.setLayoutType(personal != null && personal.getLayoutType() != null
			? personal.getLayoutType() : global.getLayoutType());
		// 原始个人值（供设置表单回显「沿用全局」态）
		vo.setPersonalFontSize(personal == null ? null : personal.getFontSize());
		vo.setPersonalLayoutType(personal == null ? null : personal.getLayoutType());
		vo.setMenuConfig(parseMap(personal == null ? null : personal.getMenuConfigJson()));
		return vo;
	}

	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 序列化个人菜单配置失败：{}", e.getMessage());
			return null;
		}
	}

	private Map<String, Object> parseMap(String json) {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> map = JsonUtil.parse(json, Map.class);
			return (map == null) ? new LinkedHashMap<>() : map;
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}
}
