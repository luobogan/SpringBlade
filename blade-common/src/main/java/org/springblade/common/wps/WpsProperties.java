package org.springblade.common.wps;

import lombok.Data;

/**
 * WPS WebOffice 统一配置（{@code blade.wps.*}）。
 *
 * <p>由「知识管理(blade-doc)」与「收发文(blade-workflow/odoc)」共用同一份 appId/密钥与
 * 回调 base：WPS 开放平台一个应用只登记一个回调地址，两侧必须共用。生产须由 Nacos
 * 下发高熵密钥，禁止明文入库/入仓。</p>
 *
 * <p>本类为纯 POJO，不注册 Spring Bean：各服务自行以
 * {@code @Bean @ConfigurationProperties(prefix = "blade.wps")} 装配，避免公共模块
 * 引入自动装配副作用。</p>
 */
@Data
public class WpsProperties {

	/** WPS 开放平台应用 ID */
	private String appId = "";

	/** WPS 开放平台应用密钥（服务端签名/回调校验） */
	private String appSecret = "";

	/** WebOffice umd.js 地址（前端 public 本地部署或 CDN） */
	private String jssdkUrl = "/web-office-sdk-solution.umd.js";

	/** WPS 中台回调与内容 URL 的公网 base（留空则按请求推断） */
	private String publicBase = "";

	/** 内容/回调 URL 签名密钥：两侧必须一致，否则跨服务签发的 URL 无法互验 */
	private String urlSignSecret = "";

	/** URL 签名有效期（秒） */
	private int urlSignTtlSeconds = 300;

	/** 是否由本服务承载 {@code /v3/3rd/**} 回调入口（单入口架构下只有一个服务为 true） */
	private boolean callbackEnabled = false;

	/** 回调 token 硬校验开关；联调期可临时关闭，生产必须为 true */
	private boolean callbackAuthStrict = true;

	/**
	 * 辅助端点（{@code /users}、{@code /object/**}）是否把 token 绑定到 fileId 做 HMAC 验签。
	 *
	 * <p>默认 false：此时只校验 token 形状 + 时效（见 {@code WpsCallbackSupport.tokenShapeValid}）。
	 * 置 true 后要求 WPS 中台回调带 {@code X-WebOffice-File-Id}（或 query {@code file_id}），
	 * 且该 fileId 必须与前端 init 签发 token 时的 wireFileId 逐字一致。WPS 中台回调头形态
	 * 尚未在真实联调中确认，故默认关闭；联调确认后再开启，避免误拒全部辅助端点回调。</p>
	 */
	private boolean callbackAuthBindResource = false;

	/** 是否启用 WPS 方案：appId/appSecret 均非空才视为已配置 */
	public boolean isConfigured() {
		return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
	}
}
