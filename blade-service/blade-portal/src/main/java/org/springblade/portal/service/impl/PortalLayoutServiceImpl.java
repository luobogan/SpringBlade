package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.dto.PortalLayoutInstantiateDTO;
import org.springblade.portal.dto.PortalLayoutTemplateSaveDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalLayout;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.mapper.PortalElementMapper;
import org.springblade.portal.mapper.PortalLayoutMapper;
import org.springblade.portal.service.IPortalLayoutService;
import org.springblade.portal.service.IPortalPageService;
import org.springblade.portal.vo.PortalPageExportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 门户布局模板服务实现（对标泛微 portallayout §12.5 P2）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalLayoutServiceImpl implements IPortalLayoutService {

	private final PortalLayoutMapper layoutMapper;
	private final PortalElementMapper elementMapper;
	private final IPortalPageService pageService;

	@Override
	public List<PortalLayout> list() {
		return layoutMapper.selectList(new QueryWrapper<PortalLayout>()
			.eq("tenant_id", tenantOf(SecureUtil.getUser())).eq("is_deleted", 0)
			.orderByAsc("layout_type").orderByAsc("id"));
	}

	@Override
	public Long saveFromPage(PortalLayoutTemplateSaveDTO dto) {
		if (dto == null || dto.getPageId() == null || isBlank(dto.getLayoutName())) {
			return null;
		}
		// 仅存页面级共享元素(owner=0)作为模板
		List<PortalElement> elements = elementMapper.selectList(new QueryWrapper<PortalElement>()
			.eq("page_id", dto.getPageId()).eq("is_deleted", 0).eq("owner_user_id", 0)
			.orderByAsc("row_no").orderByAsc("sort_order").orderByAsc("id"));
		if (elements.isEmpty()) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		LocalDateTime now = LocalDateTime.now();
		PortalLayout layout = new PortalLayout();
		layout.setTenantId(tenantOf(user));
		layout.setLayoutName(dto.getLayoutName().trim());
		layout.setLayoutType(isBlank(dto.getLayoutType()) ? "cus" : dto.getLayoutType().trim());
		layout.setElementsJson(JsonUtil.toJson(elements));
		layout.setRemark(dto.getRemark());
		layout.setIsDeleted(0);
		layout.setStatus(1);
		layout.setCreateUser(user == null ? null : user.getUserId());
		layout.setCreateTime(now);
		layout.setUpdateTime(now);
		layoutMapper.insert(layout);
		return layout.getId();
	}

	@Override
	public boolean remove(Long layoutId) {
		if (layoutId == null) {
			return false;
		}
		PortalLayout layout = layoutMapper.selectById(layoutId);
		if (layout == null) {
			return false;
		}
		layout.setIsDeleted(1);
		layout.setStatus(-1);
		layout.setUpdateTime(LocalDateTime.now());
		return layoutMapper.updateById(layout) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long instantiate(PortalLayoutInstantiateDTO dto) {
		if (dto == null || dto.getLayoutId() == null || isBlank(dto.getPageCode())) {
			return null;
		}
		PortalLayout layout = layoutMapper.selectById(dto.getLayoutId());
		if (layout == null) {
			return null;
		}
		List<PortalElement> elements = parseElements(layout.getElementsJson());
		if (elements.isEmpty()) {
			return null;
		}
		// 复用导入链路：构造导出 VO → importPage 创建新页 + 复制元素
		PortalPage page = new PortalPage();
		page.setPageCode(dto.getPageCode().trim());
		page.setPageName(isBlank(dto.getPageName()) ? dto.getPageCode().trim() : dto.getPageName());
		page.setTheme("default");
		page.setIsDefault(0);
		page.setSort(1);
		PortalPageExportVO vo = new PortalPageExportVO();
		vo.setPage(page);
		vo.setElements(elements);
		vo.setShares(new ArrayList<>());
		return pageService.importPage(vo);
	}

	/* ==================== 私有 ==================== */

	private List<PortalElement> parseElements(String json) {
		List<PortalElement> result = new ArrayList<>();
		if (isBlank(json)) {
			return result;
		}
		try {
			List<?> raw = JsonUtil.parse(json, List.class);
			if (raw != null) {
				for (Object o : raw) {
					if (!(o instanceof Map)) {
						continue;
					}
					Map<?, ?> m = (Map<?, ?>) o;
					PortalElement e = new PortalElement();
					e.setElementType(str(m.get("elementType")));
					e.setElementName(str(m.get("elementName")));
					e.setShowTitle(intv(m.get("showTitle"), 1));
					e.setRowNo(intv(m.get("rowNo"), 1));
					e.setColSpan(intv(m.get("colSpan"), 12));
					e.setSortOrder(intv(m.get("sortOrder"), 0));
					e.setConfigJson(str(m.get("configJson")));
					e.setStyleJson(str(m.get("styleJson")));
					result.add(e);
				}
			}
		} catch (Exception e) {
			log.warn("[portal] 解析布局模板元素失败，msg={}", e.getMessage());
		}
		return result;
	}

	private String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private Integer intv(Object o, int def) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			try {
				return Integer.parseInt(s.trim());
			} catch (Exception ignored) {
				return def;
			}
		}
		return def;
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
}
