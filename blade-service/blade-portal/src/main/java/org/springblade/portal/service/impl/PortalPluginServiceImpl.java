package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalPluginSaveDTO;
import org.springblade.portal.entity.PortalPlugin;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalPluginMapper;
import org.springblade.portal.service.IPortalPluginService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门户插件管理服务实现（对标泛微 pluginmanage §14.2 G，hppluginsetting）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPluginServiceImpl implements IPortalPluginService {

	private final PortalPluginMapper pluginMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalPlugin> list() {
		return pluginMapper.selectList(new QueryWrapper<PortalPlugin>()
			.eq("is_deleted", 0).orderByAsc("order_num").orderByAsc("id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalPluginSaveDTO dto) {
		if (dto == null || isBlank(dto.getPid())) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalPlugin exist = (dto.getId() == null) ? null : pluginMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = pluginMapper.selectCount(new QueryWrapper<PortalPlugin>()
				.eq("tenant_id", tenantId).eq("pid", dto.getPid().trim()).eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalPlugin();
			exist.setTenantId(tenantId);
			exist.setIsDeleted(0);
			exist.setStatus(1);
			exist.setCreateUser(userId);
			exist.setCreateDept(deptId);
			exist.setCreateTime(now);
		} else {
			exist.setUpdateUser(userId);
			exist.setUpdateTime(now);
		}
		exist.setPid(dto.getPid().trim());
		exist.setPluginDesc(dto.getPluginDesc());
		exist.setFilePath(dto.getFilePath());
		exist.setIsUse(dto.getIsUse() == null ? 1 : dto.getIsUse());
		exist.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
		exist.setPType(isBlank(dto.getPType()) ? PortalConstant.PLUGIN_TYPE_JS : dto.getPType().trim());
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? pluginMapper.insert(exist) > 0 : pluginMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_PLUGIN_SAVE, "plugin", String.valueOf(exist.getId()),
				exist.getPid(), (creating ? "登记插件 " : "编辑插件 ") + exist.getPid(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalPlugin exist = pluginMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalPlugin gone = new PortalPlugin();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		pluginMapper.update(gone, new QueryWrapper<PortalPlugin>().eq("id", id));
		logService.record(PortalConstant.LOG_PLUGIN_DELETE, "plugin", String.valueOf(id),
			exist.getPid(), "删除插件登记 " + exist.getPid(), null, null);
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
