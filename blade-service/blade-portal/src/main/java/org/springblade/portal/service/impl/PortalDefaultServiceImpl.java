package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalDefault;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalDefaultMapper;
import org.springblade.portal.service.IPortalDefaultService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门户默认首页（按机构）服务实现
 * <p>对标泛微 defaultportal（§13.8）。写操作经 {@link IPortalLogService} 埋点审计。</p>
 */
@Service
@RequiredArgsConstructor
public class PortalDefaultServiceImpl implements IPortalDefaultService {

	private final PortalDefaultMapper defaultMapper;
	private final IPortalLogService logService;

	@Override
	public boolean set(Long deptId, Long pageId, String remark) {
		if (deptId == null || pageId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();

		// 含已逻辑删除行一并查出，便于复活（dept_id 唯一）
		PortalDefault exist = defaultMapper.selectOne(
			new QueryWrapper<PortalDefault>().eq("dept_id", deptId).last("limit 1"));
		Long oldPageId = (exist == null) ? null : exist.getPageId();
		boolean ok;
		if (exist == null) {
			PortalDefault d = new PortalDefault();
			d.setTenantId(tenantId);
			d.setDeptId(deptId);
			d.setPageId(pageId);
			d.setRemark(remark);
			d.setIsDeleted(0);
			d.setStatus(1);
			d.setCreateTime(now);
			d.setCreateUser(userId);
			ok = defaultMapper.insert(d) > 0;
		} else {
			UpdateWrapper<PortalDefault> uw = new UpdateWrapper<>();
			uw.set("page_id", pageId)
				.set("remark", remark)
				.set("is_deleted", 0)
				.set("status", 1)
				.set("update_time", now)
				.set("update_user", userId)
				.eq("id", exist.getId());
			ok = defaultMapper.update(null, uw) >= 0;
		}
		if (ok) {
			logService.record(PortalConstant.LOG_DEFAULT_APPOINT, "default", String.valueOf(deptId),
				"机构默认首页", "deptId=" + deptId + " 默认页 " + oldPageId + " -> " + pageId, null, null);
		}
		return ok;
	}

	@Override
	public boolean remove(Long deptId) {
		if (deptId == null) {
			return false;
		}
		PortalDefault exist = defaultMapper.selectOne(
			new QueryWrapper<PortalDefault>().eq("dept_id", deptId).eq("is_deleted", 0).last("limit 1"));
		if (exist == null) {
			return false;
		}
		UpdateWrapper<PortalDefault> uw = new UpdateWrapper<>();
		uw.set("is_deleted", 1).set("status", 0)
			.set("update_time", LocalDateTime.now())
			.eq("id", exist.getId());
		boolean ok = defaultMapper.update(null, uw) >= 0;
		if (ok) {
			logService.record(PortalConstant.LOG_DEFAULT_CLEAR, "default", String.valueOf(deptId),
				"机构默认首页", "清除 deptId=" + deptId + " 的默认首页（原 pageId=" + exist.getPageId() + "）", null, null);
		}
		return ok;
	}

	@Override
	public List<PortalDefault> list() {
		return defaultMapper.selectList(
			new QueryWrapper<PortalDefault>().eq("is_deleted", 0).orderByAsc("dept_id"));
	}
}
