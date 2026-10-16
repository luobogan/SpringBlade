package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalSettingSaveDTO;
import org.springblade.portal.entity.PortalAccountSetting;
import org.springblade.portal.entity.PortalSetting;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalAccountSettingMapper;
import org.springblade.portal.mapper.PortalSettingMapper;
import org.springblade.portal.service.IPortalSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 门户应用设置服务实现（对标泛微 portalsetting §16.3，租户单例 upsert）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSettingServiceImpl implements IPortalSettingService {

	private final PortalSettingMapper settingMapper;
	private final PortalAccountSettingMapper accountSettingMapper;
	private final IPortalLogService logService;

	@Override
	public PortalSetting get() {
		String tenantId = currentTenant();
		PortalSetting setting = settingMapper.selectOne(new QueryWrapper<PortalSetting>()
			.eq("tenant_id", tenantId).eq("is_deleted", 0).last("limit 1"));
		if (setting == null) {
			// 兜底：未播种时返回字段默认值实例（与建表默认值一致），不返回 null
			setting = new PortalSetting();
			setting.setTenantId(tenantId);
			setting.setNeedRefresh(0);
			setting.setRefreshScope(PortalConstant.SETTING_REFRESH_SCOPE_PAGE);
			setting.setRefreshMins(PortalConstant.DEFAULT_REFRESH_MINS);
			setting.setRsstype(PortalConstant.RSSTYPE_CLIENT);
			setting.setShowLastHp(1);
			setting.setShowNoRightElement(0);
			setting.setHiddenMyPortal(0);
			setting.setAutoSortTopMenu(0);
			setting.setDefaultExpandLeft(1);
			setting.setShowTopMenuFullName(0);
			setting.setShowFreqMenu(1);
			setting.setQuickSearchEnabled(1);
			setting.setLayoutType(PortalConstant.LAYOUT_STANDARD);
			setting.setFontSize(PortalConstant.FONT_DEFAULT);
		}
		return setting;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalSettingSaveDTO dto) {
		if (dto == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalSetting exist = settingMapper.selectOne(new QueryWrapper<PortalSetting>()
			.eq("tenant_id", tenantId).last("limit 1"));
		if (exist == null) {
			PortalSetting fresh = new PortalSetting();
			fresh.setTenantId(tenantId);
			fresh.setIsDeleted(0);
			fresh.setStatus(1);
			fresh.setCreateUser(userId);
			fresh.setCreateTime(now);
			apply(fresh, dto);
			fresh.setUpdateUser(userId);
			fresh.setUpdateTime(now);
			boolean ok = settingMapper.insert(fresh) > 0;
			if (ok) {
				logService.record(PortalConstant.LOG_SETTING_SAVE, "setting", tenantId,
					"门户应用设置", "初始化门户应用设置", null, null);
			}
			return ok;
		}
		apply(exist, dto);
		exist.setUpdateUser(userId);
		exist.setUpdateTime(now);
		boolean ok = settingMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_SETTING_SAVE, "setting", tenantId,
				"门户应用设置", "更新门户应用设置", null, null);
		}
		return ok;
	}

	@Override
	public int syncToAll() {
		BladeUser user = SecureUtil.getUser();
		String tenantId = currentTenant();
		PortalSetting setting = get();
		try {
			// 把全局字体/布局同步到所有已有个性化设置的用户（对标 synchronize/layoutSynchronize §16.3）
			UpdateWrapper<PortalAccountSetting> uw = new UpdateWrapper<>();
			uw.eq("tenant_id", tenantId)
				.eq("is_deleted", 0)
				.set("font_size", setting.getFontSize())
				.set("layout_type", setting.getLayoutType())
				.set("update_user", user == null ? null : user.getUserId())
				.set("update_time", LocalDateTime.now());
			int rows = accountSettingMapper.update(null, uw);
			logService.record(PortalConstant.LOG_SETTING_SYNC, "setting", tenantId,
				"门户应用设置", "同步字体/布局到 " + rows + " 个已个性化用户", null, null);
			return rows;
		} catch (Exception e) {
			log.error("[portal] 同步门户设置失败，tenant={}, err={}", tenantId, e.getMessage());
			return -1;
		}
	}

	private void apply(PortalSetting target, PortalSettingSaveDTO dto) {
		if (dto.getNeedRefresh() != null) {
			target.setNeedRefresh(dto.getNeedRefresh());
		}
		if (dto.getRefreshScope() != null) {
			target.setRefreshScope(dto.getRefreshScope());
		}
		if (dto.getRefreshMins() != null && dto.getRefreshMins() > 0) {
			target.setRefreshMins(dto.getRefreshMins());
		}
		if (dto.getRsstype() != null) {
			target.setRsstype(dto.getRsstype());
		}
		if (dto.getShowLastHp() != null) {
			target.setShowLastHp(dto.getShowLastHp());
		}
		if (dto.getShowNoRightElement() != null) {
			target.setShowNoRightElement(dto.getShowNoRightElement());
		}
		if (dto.getHiddenMyPortal() != null) {
			target.setHiddenMyPortal(dto.getHiddenMyPortal());
		}
		if (dto.getAutoSortTopMenu() != null) {
			target.setAutoSortTopMenu(dto.getAutoSortTopMenu());
		}
		if (dto.getDefaultExpandLeft() != null) {
			target.setDefaultExpandLeft(dto.getDefaultExpandLeft());
		}
		if (dto.getShowTopMenuFullName() != null) {
			target.setShowTopMenuFullName(dto.getShowTopMenuFullName());
		}
		if (dto.getShowFreqMenu() != null) {
			target.setShowFreqMenu(dto.getShowFreqMenu());
		}
		if (dto.getQuickSearchEnabled() != null) {
			target.setQuickSearchEnabled(dto.getQuickSearchEnabled());
		}
		if (dto.getLayoutType() != null) {
			target.setLayoutType(dto.getLayoutType());
		}
		if (dto.getFontSize() != null) {
			target.setFontSize(dto.getFontSize());
		}
	}

	private String currentTenant() {
		BladeUser user = SecureUtil.getUser();
		if (user != null && user.getTenantId() != null && !user.getTenantId().isBlank()) {
			return user.getTenantId();
		}
		return "000000";
	}
}
