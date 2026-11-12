package org.springblade.common.wps;

import java.util.function.Function;

/**
 * WPS 生效配置解析器（单一数据源收口的「公用方法」）。
 *
 * <p>优先级：{@code doc_engine_setting} 库表非空值 &gt; {@link WpsProperties}（yml/Nacos 兜底）。
 * 库表值为 null、空串或全空白时一律回退，避免历史 {@code saveSetting} 写入 {@code ""}
 * 造成「界面显示已配置、实际取到空值」。</p>
 *
 * <p>{@code reader} 由各服务注入（blade-doc 走 MyBatis 设置服务、blade-workflow 走 JdbcTemplate
 * 读同一张表），公共模块因此无需引入 JDBC/MyBatis 依赖。</p>
 */
public final class WpsSettingsResolver {

	private WpsSettingsResolver() {
	}

	public static WpsSettings resolve(Function<String, String> reader, WpsProperties fallback) {
		WpsProperties fb = fallback == null ? new WpsProperties() : fallback;
		WpsSettings s = new WpsSettings();
		s.setAppId(pick(reader, WpsSettings.K_APP_ID, fb.getAppId()));
		s.setAppSecret(pick(reader, WpsSettings.K_APP_SECRET, fb.getAppSecret()));
		s.setJssdkUrl(pick(reader, WpsSettings.K_JSSDK_URL, fb.getJssdkUrl()));
		s.setPublicBase(stripTrailingSlash(pick(reader, WpsSettings.K_PUBLIC_BASE, fb.getPublicBase())));
		s.setUrlSignSecret(pick(reader, WpsSettings.K_URL_SIGN_SECRET, fb.getUrlSignSecret()));
		s.setUrlSignTtlSeconds(pickInt(reader, WpsSettings.K_URL_SIGN_TTL, fb.getUrlSignTtlSeconds(), fb));
		s.setCallbackAuthStrict(pickBool(reader, WpsSettings.K_CALLBACK_AUTH_STRICT, fb.isCallbackAuthStrict()));
		s.setCallbackAuthBindResource(pickBool(reader, WpsSettings.K_CALLBACK_AUTH_BIND_RESOURCE, fb.isCallbackAuthBindResource()));
		return s;
	}

	private static String pick(Function<String, String> reader, String key, String fallback) {
		String fromTable = reader == null ? null : reader.apply(key);
		return hasText(fromTable) ? fromTable.trim() : (fallback == null ? "" : fallback.trim());
	}

	private static int pickInt(Function<String, String> reader, String key, int fallback, WpsProperties fb) {
		String fromTable = reader == null ? null : reader.apply(key);
		if (hasText(fromTable)) {
			try {
				int v = Integer.parseInt(fromTable.trim());
				if (v > 0) {
					return v;
				}
			} catch (NumberFormatException ignored) {
				// 非法值回退到兜底，不抛异常阻断编辑器
			}
		}
		return fallback > 0 ? fallback : (fb.getUrlSignTtlSeconds() > 0 ? fb.getUrlSignTtlSeconds() : 300);
	}

	private static boolean pickBool(Function<String, String> reader, String key, boolean fallback) {
		String fromTable = reader == null ? null : reader.apply(key);
		if (!hasText(fromTable)) {
			return fallback;
		}
		String v = fromTable.trim();
		return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
	}

	private static String stripTrailingSlash(String s) {
		return s == null ? "" : s.replaceAll("/+$", "");
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}
}
