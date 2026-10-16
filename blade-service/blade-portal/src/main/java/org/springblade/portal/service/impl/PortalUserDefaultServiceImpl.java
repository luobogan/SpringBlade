package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.entity.PortalUserDefault;
import org.springblade.portal.mapper.PortalUserDefaultMapper;
import org.springblade.portal.service.IPortalUserDefaultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户个人默认门户服务实现（对标泛微 hpcurrentuse §18.2）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalUserDefaultServiceImpl implements IPortalUserDefaultService {

	private final PortalUserDefaultMapper userDefaultMapper;

	@Override
	public Long getUserDefaultPageId(Long userId) {
		if (userId == null) {
			return null;
		}
		PortalUserDefault ud = userDefaultMapper.selectOne(new QueryWrapper<PortalUserDefault>()
			.eq("user_id", userId).eq("is_deleted", 0));
		return ud == null ? null : ud.getPageId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean set(Long userId, Long pageId) {
		if (userId == null || pageId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();
		PortalUserDefault ud = userDefaultMapper.selectOne(new QueryWrapper<PortalUserDefault>()
			.eq("user_id", userId));
		if (ud == null) {
			ud = new PortalUserDefault();
			ud.setTenantId(tenantId);
			ud.setUserId(userId);
			ud.setPageId(pageId);
			ud.setIsDeleted(0);
			ud.setStatus(1);
			ud.setCreateUser(userId);
			ud.setCreateTime(now);
			ud.setUpdateUser(userId);
			ud.setUpdateTime(now);
			return userDefaultMapper.insert(ud) > 0;
		}
		// 命中历史软删记录则复用；否则原地更新
		ud.setTenantId(tenantId);
		ud.setPageId(pageId);
		ud.setIsDeleted(0);
		ud.setStatus(1);
		ud.setUpdateUser(userId);
		ud.setUpdateTime(now);
		return userDefaultMapper.updateById(ud) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long userId) {
		if (userId == null) {
			return false;
		}
		PortalUserDefault ud = userDefaultMapper.selectOne(new QueryWrapper<PortalUserDefault>()
			.eq("user_id", userId).eq("is_deleted", 0));
		if (ud == null) {
			return true;
		}
		ud.setIsDeleted(1);
		ud.setStatus(-1);
		ud.setUpdateTime(LocalDateTime.now());
		return userDefaultMapper.updateById(ud) > 0;
	}
}
