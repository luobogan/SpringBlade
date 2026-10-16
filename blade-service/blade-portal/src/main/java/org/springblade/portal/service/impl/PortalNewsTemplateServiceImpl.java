package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalNewsTemplateSaveDTO;
import org.springblade.portal.entity.PortalNewsTemplate;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalNewsTemplateMapper;
import org.springblade.portal.service.IPortalNewsTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新闻模板库服务实现（对标泛微 newstemplate §15.4 C1，PAGENEWSTEMPLATE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalNewsTemplateServiceImpl implements IPortalNewsTemplateService {

	private final PortalNewsTemplateMapper templateMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalNewsTemplate> list(String templateType) {
		QueryWrapper<PortalNewsTemplate> qw = new QueryWrapper<>();
		qw.eq("is_deleted", 0);
		if (templateType != null && !templateType.isBlank()) {
			qw.eq("template_type", templateType.trim());
		}
		qw.orderByAsc("id");
		return templateMapper.selectList(qw);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(PortalNewsTemplateSaveDTO dto) {
		if (dto == null || isBlank(dto.getTemplateName()) || isBlank(dto.getTemplateType())) {
			return false;
		}
		String templateType = dto.getTemplateType().trim();
		if (!PortalConstant.NEWS_TEMPLATE_INNER.equals(templateType)
			&& !PortalConstant.NEWS_TEMPLATE_OUTER.equals(templateType)) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		String tenantId = currentTenant();
		LocalDateTime now = LocalDateTime.now();

		PortalNewsTemplate exist = (dto.getId() == null) ? null : templateMapper.selectById(dto.getId());
		boolean creating = (exist == null);
		if (creating) {
			long dup = templateMapper.selectCount(new QueryWrapper<PortalNewsTemplate>()
				.eq("tenant_id", tenantId)
				.eq("template_name", dto.getTemplateName().trim())
				.eq("is_deleted", 0));
			if (dup > 0) {
				return false;
			}
			exist = new PortalNewsTemplate();
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
		exist.setTemplateName(dto.getTemplateName().trim());
		exist.setTemplateType(templateType);
		exist.setLayoutHtml(dto.getLayoutHtml());
		exist.setRemark(dto.getRemark());

		boolean makeDefault = Integer.valueOf(1).equals(dto.getIsDefault());
		exist.setIsDefault(makeDefault ? 1 : 0);
		boolean ok = creating ? templateMapper.insert(exist) > 0 : templateMapper.updateById(exist) > 0;
		if (ok && makeDefault) {
			// 同类型内默认唯一：把同类型其他模板的默认标记清掉
			clearOtherDefault(tenantId, templateType, exist.getId());
		}
		if (ok) {
			logService.record(PortalConstant.LOG_NEWS_TEMPLATE_SAVE, "newsTemplate",
				String.valueOf(exist.getId()), exist.getTemplateName(),
				(creating ? "新建新闻模板 " : "编辑新闻模板 ") + exist.getTemplateName(), null, null);
		}
		return ok;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalNewsTemplate exist = templateMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalNewsTemplate gone = new PortalNewsTemplate();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		templateMapper.update(gone, new QueryWrapper<PortalNewsTemplate>().eq("id", id));
		logService.record(PortalConstant.LOG_NEWS_TEMPLATE_DELETE, "newsTemplate", String.valueOf(id),
			exist.getTemplateName(), "删除新闻模板 " + exist.getTemplateName(), null, null);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setDefault(Long id) {
		if (id == null) {
			return false;
		}
		PortalNewsTemplate exist = templateMapper.selectById(id);
		if (exist == null) {
			return false;
		}
		PortalNewsTemplate target = new PortalNewsTemplate();
		target.setIsDefault(1);
		target.setUpdateTime(LocalDateTime.now());
		templateMapper.update(target, new QueryWrapper<PortalNewsTemplate>().eq("id", id));
		clearOtherDefault(exist.getTenantId(), exist.getTemplateType(), id);
		logService.record(PortalConstant.LOG_NEWS_TEMPLATE_SAVE, "newsTemplate", String.valueOf(id),
			exist.getTemplateName(), "设新闻模板为默认 " + exist.getTemplateName(), null, null);
		return true;
	}

	private void clearOtherDefault(String tenantId, String templateType, Long keepId) {
		PortalNewsTemplate clear = new PortalNewsTemplate();
		clear.setIsDefault(0);
		clear.setUpdateTime(LocalDateTime.now());
		UpdateWrapper<PortalNewsTemplate> uw = new UpdateWrapper<>();
		uw.eq("tenant_id", tenantId)
			.eq("template_type", templateType)
			.eq("is_deleted", 0)
			.ne("id", keepId)
			.eq("is_default", 1);
		templateMapper.update(clear, uw);
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
