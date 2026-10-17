package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalConfig;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalConfigMapper;
import org.springblade.portal.service.IPortalConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门户配置项服务实现（对标泛微 portalconfig §16.6 + PortalConfigUtil）
 * <p>进程内缓存：ConcurrentHashMap + TTL（默认 60s）；set 后立即 refresh。
 * 对标 StaticObj 进程内缓存思路，不引第三方缓存库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalConfigServiceImpl implements IPortalConfigService {

	/** 缓存 TTL（毫秒） */
	private static final long CACHE_TTL_MS = 60_000L;

	/** 进程内缓存：tenantId -> (缓存时间戳, 配置快照) */
	private final Map<String, long[] /* [expireAt] */> expireMap = new ConcurrentHashMap<>();
	private final Map<String, Map<String, String>> snapshotMap = new ConcurrentHashMap<>();

	private final PortalConfigMapper configMapper;
	private final IPortalLogService logService;

	@Override
	public List<PortalConfig> list() {
		return configMapper.selectList(new QueryWrapper<PortalConfig>()
			.eq("tenant_id", currentTenant()).eq("is_deleted", 0).orderByAsc("id"));
	}

	@Override
	public String getValue(String ccode) {
		if (ccode == null || ccode.isBlank()) {
			return null;
		}
		Map<String, String> snapshot = snapshot(currentTenant());
		return snapshot.get(ccode);
	}

	@Override
	public boolean isEnabled(String ccode) {
		return "1".equals(getValue(ccode));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setValue(String ccode, String vvalue) {
		if (ccode == null || ccode.isBlank() || vvalue == null) {
			return false;
		}
		String tenantId = currentTenant();
		BladeUser user = SecureUtil.getUser();
		Long userId = (user == null) ? null : user.getUserId();
		LocalDateTime now = LocalDateTime.now();

		PortalConfig exist = configMapper.selectOne(new QueryWrapper<PortalConfig>()
			.eq("tenant_id", tenantId).eq("ccode", ccode.trim()).last("limit 1"));
		if (exist == null) {
			return false;
		}
		PortalConfig target = new PortalConfig();
		target.setVvalue(vvalue);
		target.setUpdateUser(userId);
		target.setUpdateTime(now);
		int rows = configMapper.update(target, new UpdateWrapper<PortalConfig>()
			.eq("tenant_id", tenantId).eq("ccode", ccode.trim()));
		if (rows <= 0) {
			return false;
		}
		// 写库后立即失效缓存（对标 PortalConfigUtil.refresh）
		refresh();
		logService.record(PortalConstant.LOG_CONFIG_SAVE, "config", ccode,
			exist.getName(), "修改配置项 " + ccode + "：" + exist.getVvalue() + " → " + vvalue, null, null);
		return true;
	}

	@Override
	public void refresh() {
		expireMap.clear();
		snapshotMap.clear();
	}

	/** 取租户配置快照（带 TTL 缓存） */
	private Map<String, String> snapshot(String tenantId) {
		long now = System.currentTimeMillis();
		long[] expireAt = expireMap.get(tenantId);
		if (expireAt != null && expireAt[0] > now) {
			Map<String, String> cached = snapshotMap.get(tenantId);
			if (cached != null) {
				return cached;
			}
		}
		List<PortalConfig> rows = configMapper.selectList(new QueryWrapper<PortalConfig>()
			.eq("tenant_id", tenantId).eq("is_deleted", 0).select("ccode", "vvalue"));
		Map<String, String> snapshot = new ConcurrentHashMap<>();
		for (PortalConfig c : rows) {
			if (c.getCcode() != null) {
				snapshot.put(c.getCcode(), c.getVvalue());
			}
		}
		expireMap.put(tenantId, new long[] {now + CACHE_TTL_MS});
		snapshotMap.put(tenantId, snapshot);
		return snapshot;
	}

	private String currentTenant() {
		BladeUser user = SecureUtil.getUser();
		if (user != null && user.getTenantId() != null && !user.getTenantId().isBlank()) {
			return user.getTenantId();
		}
		return "000000";
	}
}
