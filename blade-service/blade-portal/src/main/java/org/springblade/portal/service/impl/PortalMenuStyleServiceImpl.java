package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalMenuStyleSaveDTO;
import org.springblade.portal.entity.PortalMenuStyle;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalMenuStyleMapper;
import org.springblade.portal.service.IPortalMenuStyleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单样式库服务实现（对标泛微 menustylelib §14.2 E，hpMenuStyle menuh/menuv）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalMenuStyleServiceImpl implements IPortalMenuStyleService {

	private final PortalMenuStyleMapper styleMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalMenuStyle> list(String styleType) {
		QueryWrapper<PortalMenuStyle> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (styleType != null && !styleType.isBlank()) {
			qw.eq("style_type", styleType.trim());
		}
		qw.orderByAsc("id");
		return styleMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalMenuStyleSaveDTO dto) {
		if (dto == null || isBlank(dto.getStyleName()) || isBlank(dto.getStyleType())) {
			return false;
		}
		String styleType = dto.getStyleType().trim();
		if (!PortalConstant.MENU_STYLE_H.equals(styleType) && !PortalConstant.MENU_STYLE_V.equals(styleType)) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalMenuStyle exist = (dto.getId() == null) ? null : styleMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = styleMapper.selectCount(new QueryWrapper<PortalMenuStyle>()
				.eq("tenant_id", tenantId)
				.eq("style_name", dto.getStyleName().trim())
				.eq("style_type", styleType)
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalMenuStyle();
			exist.setTenantId(tenantId);
			exist.setCiteCount(0);
			exist.setIsDeleted(0);
			exist.setStatus(1);
			exist.setCreateUser(userId);
			exist.setCreateDept(deptId);
			exist.setCreateTime(now);
		} else {
			exist.setUpdateUser(userId);
			exist.setUpdateTime(now);
		}
		exist.setStyleName(dto.getStyleName().trim());
		exist.setStyleDesc(dto.getStyleDesc());
		exist.setStyleType(styleType);
		exist.setStyleConfig(toJson(dto.getStyleConfig()));
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? styleMapper.insert(exist) > 0 : styleMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_MENU_STYLE_SAVE, "menuStyle", String.valueOf(exist.getId()),
				exist.getStyleName(), (creating ? "新建菜单样式 " : "编辑菜单样式 ") + exist.getStyleName(),
				null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalMenuStyle exist = styleMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalMenuStyle gone = new PortalMenuStyle();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		styleMapper.update(gone, new QueryWrapper<PortalMenuStyle>().eq("id", id));
		logService.record(PortalConstant.LOG_MENU_STYLE_DELETE, "menuStyle", String.valueOf(id),
			exist.getStyleName(), "删除菜单样式 " + exist.getStyleName(), null, null);
		return true;
	}

	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 序列化菜单样式配置失败：{}", e.getMessage());
			return null;
		}
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
