package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.dto.PortalElementLibInstantiateDTO;
import org.springblade.portal.dto.PortalElementLibSaveDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.entity.PortalElementLib;
import org.springblade.portal.entity.PortalPage;
import org.springblade.portal.mapper.PortalElementLibMapper;
import org.springblade.portal.mapper.PortalElementMapper;
import org.springblade.portal.mapper.PortalPageMapper;
import org.springblade.portal.service.IPortalElementLibService;
import org.springblade.portal.vo.PortalPageBrief;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 门户独立元素库服务实现（对标 hpBaseElement §12.5 P3）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalElementLibServiceImpl implements IPortalElementLibService {

	private final PortalElementLibMapper libMapper;
	private final PortalElementMapper elementMapper;
	private final PortalPageMapper pageMapper;

	@Override
	public List<PortalElementLib> list() {
		return libMapper.selectList(new QueryWrapper<PortalElementLib>()
			.eq("tenant_id", tenantOf(SecureUtil.getUser())).eq("is_deleted", 0)
			.orderByAsc("status").orderByAsc("id"));
	}

	@Override
	public Long save(PortalElementLibSaveDTO dto) {
		if (dto == null || isBlank(dto.getElementType()) || isBlank(dto.getElementName())) {
			return null;
		}
		BladeUser user = SecureUtil.getUser();
		Long userId = userIdOf(user);
		LocalDateTime now = LocalDateTime.now();
		PortalElementLib lib = (dto.getLibId() == null) ? null : libMapper.selectById(dto.getLibId());
		boolean creating = (lib == null);
		if (creating) {
			lib = new PortalElementLib();
			lib.setTenantId(tenantOf(user));
			lib.setIsDeleted(0);
			lib.setStatus(1);
			lib.setCreateUser(userId);
			lib.setCreateTime(now);
		}
		lib.setElementType(dto.getElementType().trim());
		lib.setElementName(dto.getElementName().trim());
		lib.setConfigJson(dto.getConfigJson());
		lib.setStyleJson(dto.getStyleJson());
		lib.setRemark(dto.getRemark());
		lib.setUpdateUser(userId);
		lib.setUpdateTime(now);
		if (creating) {
			libMapper.insert(lib);
		} else {
			libMapper.updateById(lib);
		}
		return lib.getId();
	}

	@Override
	public boolean setArchived(Long libId, boolean archived) {
		if (libId == null) {
			return false;
		}
		PortalElementLib lib = libMapper.selectById(libId);
		if (lib == null) {
			return false;
		}
		lib.setStatus(archived ? 0 : 1);
		lib.setUpdateTime(LocalDateTime.now());
		return libMapper.updateById(lib) > 0;
	}

	@Override
	public boolean remove(Long libId) {
		if (libId == null) {
			return false;
		}
		PortalElementLib lib = libMapper.selectById(libId);
		if (lib == null) {
			return false;
		}
		// 被引用时禁止删除（避免页面元素悬空）
		Long refs = elementMapper.selectCount(new QueryWrapper<PortalElement>()
			.eq("lib_id", libId).eq("is_deleted", 0));
		if (refs != null && refs > 0) {
			return false;
		}
		lib.setIsDeleted(1);
		lib.setStatus(-1);
		lib.setUpdateTime(LocalDateTime.now());
		return libMapper.updateById(lib) > 0;
	}

	@Override
	public List<PortalPageBrief> references(Long libId) {
		List<PortalPageBrief> result = new ArrayList<>();
		if (libId == null) {
			return result;
		}
		List<PortalElement> elements = elementMapper.selectList(new QueryWrapper<PortalElement>()
			.eq("lib_id", libId).eq("is_deleted", 0));
		Set<Long> pageIds = new LinkedHashSet<>();
		for (PortalElement e : elements) {
			if (e.getPageId() != null) {
				pageIds.add(e.getPageId());
			}
		}
		for (Long pageId : pageIds) {
			PortalPage page = pageMapper.selectById(pageId);
			if (page == null) {
				continue;
			}
			PortalPageBrief brief = new PortalPageBrief();
			brief.setId(page.getId());
			brief.setPageCode(page.getPageCode());
			brief.setPageName(page.getPageName());
			brief.setIsDefault(page.getIsDefault());
			result.add(brief);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long instantiate(PortalElementLibInstantiateDTO dto) {
		if (dto == null || dto.getLibId() == null || dto.getPageId() == null) {
			return null;
		}
		PortalElementLib lib = libMapper.selectById(dto.getLibId());
		// 封存(status=0)后不可再被新引用
		if (lib == null || !Integer.valueOf(1).equals(lib.getStatus())) {
			return null;
		}
		int rowNo = (dto.getRowNo() == null || dto.getRowNo() <= 0) ? 1 : dto.getRowNo();
		int colSpan = (dto.getColSpan() == null || dto.getColSpan() <= 0) ? 12 : dto.getColSpan();
		// 同页同行 sort_order 追加到末尾
		int maxSort = 0;
		List<PortalElement> sameRow = elementMapper.selectList(new QueryWrapper<PortalElement>()
			.eq("page_id", dto.getPageId()).eq("row_no", rowNo).eq("is_deleted", 0));
		for (PortalElement e : sameRow) {
			if (e.getSortOrder() != null && e.getSortOrder() > maxSort) {
				maxSort = e.getSortOrder();
			}
		}
		BladeUser user = SecureUtil.getUser();
		LocalDateTime now = LocalDateTime.now();
		PortalElement element = new PortalElement();
		element.setTenantId(tenantOf(user));
		element.setPageId(dto.getPageId());
		element.setLibId(lib.getId());
		element.setOwnerUserId(0L);
		element.setElementType(lib.getElementType());
		element.setElementName(lib.getElementName());
		element.setShowTitle(1);
		element.setRowNo(rowNo);
		element.setColSpan(colSpan);
		element.setSortOrder(maxSort + 1);
		element.setConfigJson(lib.getConfigJson());
		element.setStyleJson(lib.getStyleJson());
		element.setIsDeleted(0);
		element.setStatus(1);
		element.setCreateUser(userIdOf(user));
		element.setCreateTime(now);
		elementMapper.insert(element);
		return element.getId();
	}

	/* ==================== 私有 ==================== */

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
