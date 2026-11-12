package org.springblade.common.wps;

import lombok.Data;

/**
 * WPS WebOffice 前端统一配置（后端计算、前端直接喂给 WebOfficeSDK.init）。
 *
 * <p>收编 {@code DocEditorConfig}(blade-doc) 的 WPS 专属字段与
 * {@code OdocEditorConfigVO}(blade-workflow) 的 OnlyOffice 配置，
 * 两个模块共用同一份契约，前端只需一套 {@code WpsEditor} 组件。</p>
 *
 * <p>纯 POJO，无 Schema/Servlet 依赖，便于放在 blade-common。</p>
 */
@Data
public class WpsEditorConfig {

	/** 编辑器方案，固定 wps（保留字段便于前端灰度分流） */
	private String editorType = "wps";

	/** edit / view */
	private String mode = "edit";

	/** 文件扩展名（小写，不含点），前端据此映射 WebOfficeSDK.OfficeType */
	private String fileExt;

	private String fileName;

	private Boolean readOnly = Boolean.FALSE;

	// ───────── WebOfficeSDK.init 参数 ─────────
	private String appId;

	/** 结构化 fileId：{domain}:{bizId} */
	private String fileId;

	/** 业务 token，回调经 X-WebOffice-Token 回传 */
	private String token;

	/** 签名时间戳（秒） */
	private Long timestamp;

	private String permission = "write";

	private String userId;

	private String userName;

	/** WebOffice umd.js 地址 */
	private String jssdkUrl;

	// ───────── 内容/保存 URL（WPS 中台直连，不经过回调入口） ─────────
	/** 正文内容下载 URL（带签名） */
	private String contentUrl;

	/** 三阶段保存的 PUT 上传 URL（带签名） */
	private String uploadUrl;

	/** 业务域：doc / odoc */
	private String domain;
}
