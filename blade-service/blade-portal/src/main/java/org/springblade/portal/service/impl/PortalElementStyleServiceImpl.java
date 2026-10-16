package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalElementStyleSaveDTO;
import org.springblade.portal.entity.PortalElementStyle;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalElementStyleMapper;
import org.springblade.portal.service.IPortalElementStyleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 元素样式库/头条样式库服务实现（对标泛微 §15.3 B3 elementstylelib / B4 headlinestyle）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalElementStyleServiceImpl implements IPortalElementStyleService {

	private final PortalElementStyleMapper styleMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalElementStyle> list(String styleKind) {
		QueryWrapper<PortalElementStyle> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (styleKind != null && !styleKind.isBlank()) {
			qw.eq("style_kind", styleKind.trim());
		}
		qw.orderByAsc("id");
		return styleMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalElementStyleSaveDTO dto) {
		if (dto == null || isBlank(dto.getStyleName()) || isBlank(dto.getStyleKind())) {
			return false;
		}
		String styleKind = dto.getStyleKind().trim();
		if (!PortalConstant.ELEMENT_STYLE_KIND_ELEMENT.equals(styleKind)
			&& !PortalConstant.ELEMENT_STYLE_KIND_HEADLINE.equals(styleKind)) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalElementStyle exist = (dto.getId() == null) ? null : styleMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = styleMapper.selectCount(new QueryWrapper<PortalElementStyle>()
				.eq("tenant_id", tenantId)
				.eq("style_kind", styleKind)
				.eq("style_name", dto.getStyleName().trim())
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalElementStyle();
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
		exist.setStyleKind(styleKind);
		exist.setStyleName(dto.getStyleName().trim());
		exist.setStyleDesc(dto.getStyleDesc());
		exist.setStyleConfig(toJson(dto.getStyleConfig()));
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? styleMapper.insert(exist) > 0 : styleMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_ELEMENT_STYLE_SAVE, "elementStyle", String.valueOf(exist.getId()),
				exist.getStyleName(), (creating ? "新建" : "编辑") + styleKind + "样式 " + exist.getStyleName(),
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
		PortalElementStyle exist = styleMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalElementStyle gone = new PortalElementStyle();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		styleMapper.update(gone, new QueryWrapper<PortalElementStyle>().eq("id", id));
		logService.record(PortalConstant.LOG_ELEMENT_STYLE_DELETE, "elementStyle", String.valueOf(id),
			exist.getStyleName(), "删除元素样式 " + exist.getStyleName(), null, null);
		return true;
	}

	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 序列化元素样式配置失败：{}", e.getMessage());
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
