package org.springblade.doc.config;

import lombok.RequiredArgsConstructor;
import org.springblade.common.wps.WpsProperties;
import org.springblade.common.wps.WpsSettings;
import org.springblade.common.wps.WpsSettingsResolver;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springframework.stereotype.Component;

/**
 * WPS 生效配置读取入口（blade-doc）。
 *
 * <p>单一数据源：{@code doc_engine_setting} 库表；{@link WpsProperties} 仅作启动兜底。
 * 所有解析口径统一委托 {@link WpsSettingsResolver}，与 blade-workflow 侧完全一致。</p>
 *
 * <p>每次调用现场读库、不缓存，前端「编辑器设置」保存后立即生效。</p>
 */
@Component
@RequiredArgsConstructor
public class WpsSettingStore {

	private final IDocEngineSettingService settingService;
	private final WpsProperties fallback;

	public WpsSettings current() {
		return WpsSettingsResolver.resolve(key -> settingService.get(key, null), fallback);
	}
}
