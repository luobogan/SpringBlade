package org.springblade.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 内容引擎 - 编辑器设置视图。
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
}
