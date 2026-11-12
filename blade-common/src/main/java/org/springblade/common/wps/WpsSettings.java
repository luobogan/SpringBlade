package org.springblade.common.wps;

import lombok.Data;

/**
 * WPS WebOffice 生效配置的不可变快照（单一数据源收口）。
 *
 * <p>由 {@link WpsSettingsResolver} 从 {@code doc_engine_setting} 库表 + {@link WpsProperties}
 * 兜底解析得到，供 blade-doc / blade-workflow 两侧共用。避免各服务各自维护
 * 「库表优先」回退逻辑导致口径不一致。</p>
 *
 * <p>不注册 Spring Bean：由各服务的 {@code WpsSettingStore} 每次读库现场构造，
 * 保证前端「保存设置」后立即生效，无需重启。</p>
 */
@Data
public class WpsSettings {

	/** 库表 setting_key 常量（统一定义，杜绝各处硬编码字符串） */
	public static final String K_DEFAULT_ENGINE = "default_engine";
	public static final String K_THIRD_PLATFORM = "third_platform";
	public static final String K_APP_ID = "wps_app_id";
	public static final String K_APP_SECRET = "wps_app_secret";
	public static final String K_JSSDK_URL = "wps_jssdk_url";
	public static final String K_PUBLIC_BASE = "wps_public_base";
	public static final String K_URL_SIGN_SECRET = "wps_url_sign_secret";
	public static final String K_URL_SIGN_TTL = "wps_url_sign_ttl_seconds";
	public static final String K_CALLBACK_AUTH_STRICT = "wps_callback_auth_strict";
	public static final String K_CALLBACK_AUTH_BIND_RESOURCE = "wps_callback_auth_bind_resource";

	/** WPS 开放平台应用 ID */
	private String appId = "";
	/** WPS 开放平台应用密钥 */
	private String appSecret = "";
	/** WebOffice umd.js 地址 */
	private String jssdkUrl = "";
	/** 公网回调 base（已去除尾部斜杠） */
	private String publicBase = "";
	/** 内容/回调 URL 签名密钥 */
	private String urlSignSecret = "";
	/** URL 签名有效期（秒） */
	private int urlSignTtlSeconds;
	/** 回调 token 硬校验（默认 true，与网关白名单安全耦合） */
	private boolean callbackAuthStrict = true;
	/** 辅助端点是否按 fileId 绑定验签 */
	private boolean callbackAuthBindResource;

	/** appId/appSecret 均非空才视为已配置 */
	public boolean isConfigured() {
		return hasText(appId) && hasText(appSecret);
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}
}
