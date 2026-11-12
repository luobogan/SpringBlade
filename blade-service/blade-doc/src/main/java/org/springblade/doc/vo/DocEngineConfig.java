package org.springblade.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 内容引擎 - 编辑器设置视图（WPS 接入配置统一由本 VO 承载，落库 doc_engine_setting）。
 */
@Data
public class DocEngineConfig {

    @Schema(description = "默认编辑方案：univer / wps")
    private String defaultEngine;

    @Schema(description = "第三方平台：none / wps（前端是否启用第三方编辑方案）")
    private String thirdPlatform;

    @Schema(description = "WPS 是否可用（已启用且 appId/appSecret 非空）")
    private Boolean wpsEnabled;

    @Schema(description = "WPS 应用 ID")
    private String wpsAppId;

    @Schema(description = "WPS 应用密钥")
    private String wpsAppSecret;

    @Schema(description = "WPS jssdk 地址")
    private String wpsJssdkUrl;

    @Schema(description = "WPS 中台回调公网 base")
    private String wpsPublicBase;

    @Schema(description = "内容/回调 URL 签名密钥（doc 与 odoc 两侧共用）")
    private String wpsUrlSignSecret;

    @Schema(description = "URL 签名有效期（秒）")
    private Integer wpsUrlSignTtlSeconds;
}
