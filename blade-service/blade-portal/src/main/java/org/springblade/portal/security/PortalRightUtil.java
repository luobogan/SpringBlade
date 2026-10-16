package org.springblade.portal.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.portal.mapper.PortalRightMapper;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户权限判定（对标泛微 PortalRightUtil.checkUserRight，见 ecology-portal-config-analysis.md §13.4）
 * <p>判定口径：administrator 角色直通；否则直查 blade_role_menu 是否授予该权限码；
 * 结果按<b>请求级</b>缓存，避免同一请求内重复查库。查询异常按「无权限」处理（安全优先）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortalRightUtil {

	private static final String CACHE_ATTR = "PORTAL_RIGHT_CACHE";

	private final PortalRightMapper rightMapper;

	/**
	 * 是否拥有指定权限码
	 *
	 * @param permCode 权限码（PortalConstant.PERM_*）
	 * @return true=通过
	 */
	public boolean check(String permCode) {
		if (permCode == null || permCode.isBlank()) {
			return false;
		}
		BladeUser user = SecureUtil.getUser();
		if (user == null) {
			return false;
		}
		String roleIds = toCsv(user.getRoleId());
		if (roleIds.isEmpty()) {
			return false;
		}

		Map<String, Boolean> cache = requestCache();
		if (cache != null) {
			Boolean hit = cache.get(permCode);
			if (hit != null) {
				return hit;
			}
		}

		boolean ok;
		try {
			ok = rightMapper.countAdminRole(roleIds) > 0
				|| rightMapper.countGranted(roleIds, permCode) > 0;
		} catch (Exception e) {
			log.error("[portal-right] 权限查询异常，按无权限处理，code={}, err={}", permCode, e.getMessage());
			ok = false;
		}
		if (cache != null) {
			cache.put(permCode, ok);
		}
		return ok;
	}

	private static String toCsv(String roleId) {
		return (roleId == null) ? "" : roleId.trim();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Boolean> requestCache() {
		HttpServletRequest request = WebUtil.getRequest();
		if (request == null) {
			return null;
		}
		Object exist = request.getAttribute(CACHE_ATTR);
		if (exist instanceof Map) {
			return (Map<String, Boolean>) exist;
		}
		Map<String, Boolean> cache = new LinkedHashMap<>();
		request.setAttribute(CACHE_ATTR, cache);
		return cache;
	}
}
