package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalElementTemplateSaveDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalElementTemplate;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalElementMapper;
import org.springblade.portal.mapper.PortalElementTemplateMapper;
import org.springblade.portal.service.IPortalElementTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 元素模板库服务实现（对标泛微 elementtemplate §15.3 B2，HPELEMENTTEMPLATE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalElementTemplateServiceImpl implements IPortalElementTemplateService {

	private final PortalElementTemplateMapper templateMapper;
	private final PortalElementMapper elementMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalElementTemplate> list(String elementType) {
		QueryWrapper<PortalElementTemplate> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (elementType != null && !elementType.isBlank()) {
			qw.eq("element_type", elementType.trim());
		}
		qw.orderByAsc("id");
		return templateMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalElementTemplateSaveDTO dto) {
		if (dto == null || isBlank(dto.getElementType()) || isBlank(dto.getTemplateName())) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalElementTemplate exist = (dto.getId() == null) ? null : templateMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = templateMapper.selectCount(new QueryWrapper<PortalElementTemplate>()
				.eq("tenant_id", tenantId)
				.eq("template_name", dto.getTemplateName().trim())
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalElementTemplate();
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
		exist.setElementType(dto.getElementType().trim());
		exist.setTemplateName(dto.getTemplateName().trim());
		exist.setConfigJson(toJson(dto.getConfig()));
		exist.setStyleJson(toJson(dto.getStyle()));
		exist.setRemark(dto.getRemark());

		boolean ok = creating ? templateMapper.insert(exist) > 0 : templateMapper.updateById(exist) > 0;
		if (ok) {
			logService.record(PortalConstant.LOG_ELEMENT_TEMPLATE_SAVE, "elementTemplate",
				String.valueOf(exist.getId()), exist.getTemplateName(),
				(creating ? "另存为元素模板 " : "编辑元素模板 ") + exist.getTemplateName(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalElementTemplate exist = templateMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalElementTemplate gone = new PortalElementTemplate();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		templateMapper.update(gone, new QueryWrapper<PortalElementTemplate>().eq("id", id));
		logService.record(PortalConstant.LOG_ELEMENT_TEMPLATE_DELETE, "elementTemplate", String.valueOf(id),
			exist.getTemplateName(), "删除元素模板 " + exist.getTemplateName(), null, null);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long instantiate(Long templateId, Long pageId, Integer rowNo, Integer colSpan) {
		if (templateId == null || pageId == null) {
			return null;
		}
		PortalElementTemplate template = templateMapper.selectById(templateId);
		if (template == null || Integer.valueOf(1).equals(template.getIsDeleted())) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		LocalDateTime now = LocalDateTime.now();

		PortalElement element = new PortalElement();
		element.setTenantId(template.getTenantId());
		element.setPageId(pageId);
		element.setOwnerUserId(0L);
		element.setElementType(template.getElementType());
		element.setElementName(template.getTemplateName());
		element.setShowTitle(1);
		element.setRowNo(rowNo == null || rowNo < 1 ? 1 : rowNo);
		element.setColSpan(colSpan == null || colSpan < 1 ? 12 : colSpan);
		element.setSortOrder(0);
		element.setConfigJson(template.getConfigJson());
		element.setStyleJson(template.getStyleJson());
		element.setIsDeleted(0);
		element.setStatus(1);
		element.setCreateUser(userId);
		element.setCreateDept(deptId);
		element.setCreateTime(now);
		element.setUpdateUser(userId);
		element.setUpdateTime(now);
		if (elementMapper.insert(element) <= 0) {
			return null;
		}
		logService.record(PortalConstant.LOG_ELEMENT_SAVE, "element", String.valueOf(element.getId()),
			element.getElementName(), "从模板实例化元素 " + template.getTemplateName() + " → 页面 " + pageId,
			null, null);
		return element.getId();
	}

	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			log.warn("[portal] 序列化元素模板配置失败：{}", e.getMessage());
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
