package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.dto.PortalLoginTemplateSaveDTO;
import org.springblade.portal.entity.PortalLoginTemplate;
import org.springblade.portal.mapper.PortalLoginTemplateMapper;
import org.springblade.portal.service.IPortalLoginTemplateService;
import org.springblade.portal.vo.PortalLoginTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录前门户皮肤服务实现（对标泛微 SystemLoginTemplate §16.1）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalLoginTemplateServiceImpl implements IPortalLoginTemplateService {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final PortalLoginTemplateMapper templateMapper;

	@Override
	public List<PortalLoginTemplate> list() {
		return templateMapper.selectList(new QueryWrapper<PortalLoginTemplate>()
			.eq("tenant_id", tenantOf(SecureUtil.getUser())).eq("is_deleted", 0)
			.orderByDesc("is_current").orderByAsc("id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long save(PortalLoginTemplateSaveDTO dto) {
		if (dto == null || isBlank(dto.getTemplateName())) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = tenantOf(user);
		Long userId = userIdOf(user);
		LocalDateTime now = LocalDateTime.now();

		PortalLoginTemplate tpl = (dto.getTemplateId() == null) ? null : templateMapper.selectById(dto.getTemplateId());
		boolean creating = (tpl == null);
		if (creating) {
			PortalLoginTemplate exist = templateMapper.selectOne(new QueryWrapper<PortalLoginTemplate>()
				.eq("tenant_id", tenantId).eq("template_name", dto.getTemplateName().trim()).eq("is_deleted", 0));
			if (exist != null) {
				return null; // 同租户名称重复
			}
			tpl = new PortalLoginTemplate();
			tpl.setTenantId(tenantId);
			tpl.setIsDeleted(0);
			tpl.setStatus(1);
			tpl.setIsCurrent(0);
			tpl.setCreateUser(userId);
			tpl.setCreateTime(now);
		}
		tpl.setTemplateName(dto.getTemplateName().trim());
		tpl.setTemplateType(isBlank(dto.getTemplateType()) ? "default" : dto.getTemplateType().trim());
		tpl.setBackgroundConfig(toJson(dto.getBackgroundConfig()));
		tpl.setLogoConfig(toJson(dto.getLogoConfig()));
		tpl.setFormConfig(toJson(dto.getFormConfig()));
		tpl.setQrcodeConfig(toJson(dto.getQrcodeConfig()));
		tpl.setCopyrightConfig(toJson(dto.getCopyrightConfig()));
		tpl.setOpenTimer(dto.getOpenTimer() == null ? 0 : dto.getOpenTimer());
		tpl.setTimerStart(dto.getTimerStart());
		tpl.setTimerEnd(dto.getTimerEnd());
		tpl.setUpdateUser(userId);
		tpl.setUpdateTime(now);
		if (creating) {
			templateMapper.insert(tpl);
		} else {
			templateMapper.updateById(tpl);
		}
		return tpl.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setCurrent(Long templateId) {
		if (templateId == null) {
			return false;
		}
		PortalLoginTemplate tpl = templateMapper.selectById(templateId);
		if (tpl == null) {
			return false;
		}
		PortalLoginTemplate reset = new PortalLoginTemplate();
		reset.setIsCurrent(0);
		templateMapper.update(reset, new QueryWrapper<PortalLoginTemplate>()
			.eq("tenant_id", tpl.getTenantId()).eq("is_deleted", 0).eq("is_current", 1));
		tpl.setIsCurrent(1);
		tpl.setUpdateTime(LocalDateTime.now());
		return templateMapper.updateById(tpl) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long templateId) {
		if (templateId == null) {
			return false;
		}
		PortalLoginTemplate tpl = templateMapper.selectById(templateId);
		if (tpl == null || Integer.valueOf(1).equals(tpl.getIsCurrent())) {
			return false; // 不存在或当前启用中不可删
		}
		tpl.setIsDeleted(1);
		tpl.setStatus(-1);
		tpl.setUpdateTime(LocalDateTime.now());
		return templateMapper.updateById(tpl) > 0;
	}

	@Override
	public PortalLoginTemplateVO resolveCurrent() {
		try {
			String tenantId = tenantOf(SecureUtil.getUser());
			List<PortalLoginTemplate> all = templateMapper.selectList(new QueryWrapper<PortalLoginTemplate>()
				.eq("tenant_id", tenantId).eq("is_deleted", 0).eq("status", 1)
				.orderByDesc("is_current").orderByAsc("id"));
			if (all.isEmpty()) {
				return null;
			}
			String now = LocalDateTime.now().format(FMT);
			// 1) 定时生效优先（拉模式：当前时间落在 [start,end]）
			for (PortalLoginTemplate t : all) {
				if (Integer.valueOf(1).equals(t.getOpenTimer()) && inWindow(now, t.getTimerStart(), t.getTimerEnd())) {
					return toVO(t);
				}
			}
			// 2) 当前启用
			for (PortalLoginTemplate t : all) {
				if (Integer.valueOf(1).equals(t.getIsCurrent())) {
					return toVO(t);
				}
			}
			// 3) 兜底第一个
			return toVO(all.get(0));
		} catch (Exception e) {
			log.warn("[portal] 解析登录皮肤失败（表可能未初始化），降级无皮肤，msg={}", e.getMessage());
			return null;
		}
	}

	/* ==================== 私有 ==================== */

	private boolean inWindow(String now, String start, String end) {
		if (isBlank(start) || isBlank(end)) {
			return false;
		}
		// 统一格式 yyyy-MM-dd HH:mm:ss，字典序即时间序
		return now.compareTo(start.trim()) >= 0 && now.compareTo(end.trim()) <= 0;
	}

	private PortalLoginTemplateVO toVO(PortalLoginTemplate t) {
		PortalLoginTemplateVO vo = new PortalLoginTemplateVO();
		vo.setId(t.getId());
		vo.setTemplateName(t.getTemplateName());
		vo.setTemplateType(t.getTemplateType());
		vo.setBackgroundConfig(parseMap(t.getBackgroundConfig()));
		vo.setLogoConfig(parseMap(t.getLogoConfig()));
		vo.setFormConfig(parseMap(t.getFormConfig()));
		vo.setQrcodeConfig(parseMap(t.getQrcodeConfig()));
		vo.setCopyrightConfig(parseMap(t.getCopyrightConfig()));
		vo.setIsCurrent(t.getIsCurrent());
		return vo;
	}

	private String toJson(Object o) {
		if (o == null) {
			return null;
		}
		try {
			return JsonUtil.toJson(o);
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> parseMap(String json) {
		if (isBlank(json)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> map = JsonUtil.parse(json, Map.class);
			return map == null ? new LinkedHashMap<>() : map;
		} catch (Exception e) {
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
