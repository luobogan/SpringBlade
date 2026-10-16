package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.dto.PortalLayoutItem;
import org.springblade.portal.entity.PortalUserLayout;
import org.springblade.portal.mapper.PortalUserLayoutMapper;
import org.springblade.portal.service.IPortalUserLayoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人门户布局服务实现（对标泛微 hpcurrentuse §18.2/§23）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalUserLayoutServiceImpl implements IPortalUserLayoutService {

	private final PortalUserLayoutMapper layoutMapper;

	@Override
	public Map<Long, PortalLayoutItem> getLayoutMap(Long userId, Long pageId) {
		Map<Long, PortalLayoutItem> map = new LinkedHashMap<>();
		for (PortalLayoutItem it : getLayout(userId, pageId)) {
			if (it.getElementId() != null) {
				map.put(it.getElementId(), it);
			}
		}
		return map;
	}

	@Override
	public List<PortalLayoutItem> getLayout(Long userId, Long pageId) {
		if (userId == null || pageId == null) {
			return new ArrayList<>();
		}
		PortalUserLayout ul = layoutMapper.selectOne(new QueryWrapper<PortalUserLayout>()
			.eq("user_id", userId).eq("page_id", pageId).eq("is_deleted", 0));
		if (ul == null) {
			return new ArrayList<>();
		}
		return parseLayout(ul.getLayoutJson());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(Long userId, Long pageId, List<PortalLayoutItem> items) {
		if (userId == null || pageId == null) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		String tenantId = (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
			? user.getTenantId() : "000000";
		LocalDateTime now = LocalDateTime.now();
		String json = JsonUtil.toJson(items == null ? new ArrayList<>() : items);

		PortalUserLayout ul = layoutMapper.selectOne(new QueryWrapper<PortalUserLayout>()
			.eq("user_id", userId).eq("page_id", pageId));
		if (ul == null) {
			ul = new PortalUserLayout();
			ul.setTenantId(tenantId);
			ul.setUserId(userId);
			ul.setPageId(pageId);
			ul.setIsDeleted(0);
			ul.setStatus(1);
			ul.setCreateUser(userId);
			ul.setCreateTime(now);
			ul.setUpdateUser(userId);
			ul.setUpdateTime(now);
			ul.setLayoutJson(json);
			return layoutMapper.insert(ul) > 0;
		}
		ul.setTenantId(tenantId);
		ul.setLayoutJson(json);
		ul.setIsDeleted(0);
		ul.setStatus(1);
		ul.setUpdateUser(userId);
		ul.setUpdateTime(now);
		return layoutMapper.updateById(ul) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean reset(Long userId, Long pageId) {
		if (userId == null || pageId == null) {
			return false;
		}
		PortalUserLayout ul = layoutMapper.selectOne(new QueryWrapper<PortalUserLayout>()
			.eq("user_id", userId).eq("page_id", pageId).eq("is_deleted", 0));
		if (ul == null) {
			return true;
		}
		ul.setIsDeleted(1);
		ul.setStatus(-1);
		ul.setUpdateTime(LocalDateTime.now());
		return layoutMapper.updateById(ul) > 0;
	}

	/* ==================== 私有 ==================== */

	private List<PortalLayoutItem> parseLayout(String json) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<?> raw = JsonUtil.parse(json, List.class);
			List<PortalLayoutItem> items = new ArrayList<>();
			if (raw != null) {
				for (Object o : raw) {
					if (!(o instanceof Map)) {
						continue;
					}
					Map<?, ?> m = (Map<?, ?>) o;
					PortalLayoutItem it = new PortalLayoutItem();
					it.setElementId(toLong(m.get("elementId")));
					it.setRowNo(toInt(m.get("rowNo")));
					it.setColSpan(toInt(m.get("colSpan")));
					it.setSortOrder(toInt(m.get("sortOrder")));
					it.setHidden(toInt(m.get("hidden")));
					items.add(it);
				}
			}
			return items;
		} catch (Exception e) {
			log.warn("[portal] 解析个人布局失败，降级为空，msg={}", e.getMessage());
			return new ArrayList<>();
		}
	}

	private Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			try {
				return Long.parseLong(s.trim());
			} catch (Exception ignored) {
				return null;
			}
		}
		return null;
	}

	private Integer toInt(Object o) {
		Long l = toLong(o);
		return l == null ? null : l.intValue();
	}
}
