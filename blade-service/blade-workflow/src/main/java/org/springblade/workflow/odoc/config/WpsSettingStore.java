package org.springblade.workflow.odoc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsSettings;
import org.springblade.common.wps.WpsSettingsResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WPS 生效配置读取入口（blade-workflow）。
 *
 * <p>与 blade-doc 共用同一张 {@code doc_engine_setting} 表（dev 共库），因此公文本体
 * 无需再维护一份 {@code blade.wps.*} yml 配置。解析口径统一委托 {@link WpsSettingsResolver}。</p>
 *
 * <p>表不存在或查询异常时静默回退到 {@link WpsProperties} 兜底，避免影响非 WPS 链路；
 * 生产若拆库，应改为经 Feign 调 blade-doc 的设置读取接口。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WpsSettingStore {

	private final JdbcTemplate jdbcTemplate;
	private final WpsProperties fallback;

	public WpsSettings current() {
		return WpsSettingsResolver.resolve(this::raw, fallback);
	}

	private String raw(String key) {
		try {
			List<String> rows = jdbcTemplate.queryForList(
				"select setting_value from doc_engine_setting "
					+ "where setting_key = ? and is_deleted = 0 and status = 1 "
					+ "and (tenant_id = '000000' or tenant_id is null) limit 1",
				String.class, key);
			return rows.isEmpty() ? null : rows.get(0);
		} catch (Exception e) {
			log.debug("WPS 设置读取失败 key={}, 回退启动兜底: {}", key, e.getMessage());
			return null;
		}
	}
}
