package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.online.WpsDocOnlineEditor;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springblade.doc.vo.DocEngineConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容引擎 - 编辑器设置（默认编辑方案 Univer / WPS，及第三方平台接入配置）。
 */
@RestController
@RequestMapping("/doc/engine")
@RequiredArgsConstructor
@Tag(name = "内容引擎-编辑器设置", description = "默认编辑方案(Univer/WPS)与第三方平台接入配置")
public class DocEngineSettingController {

    private final IDocEngineSettingService settingService;
    private final WpsDocOnlineEditor wpsEditor;

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @GetMapping("/setting")
    @Operation(summary = "获取编辑器设置", description = "返回默认编辑方案、第三方平台配置与 WPS 是否可用")
    public R<DocEngineConfig> setting() {
        DocEngineConfig cfg = new DocEngineConfig();
        cfg.setDefaultEngine(settingService.get("default_engine", "univer"));
        // 库表未配置第三方平台时，按 application.yml 的 WPS 开关回显（保证前端显示与实际一致）
        String platform = settingService.get("third_platform", null);
        if (platform == null) {
            platform = wpsEditor.isEnabled() ? "wps" : "none";
        }
        cfg.setThirdPlatform(platform);
        cfg.setWpsEnabled(wpsEditor.isEnabled());
        // 回显当前生效配置（库表优先，缺省回退 application.yml）
        cfg.setWpsAppId(wpsEditor.getEffectiveWps().getAppId());
        cfg.setWpsAppSecret(wpsEditor.getEffectiveWps().getAppSecret());
        cfg.setWpsJssdkUrl(wpsEditor.getEffectiveWps().getJssdkUrl());
        return R.data(cfg);
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/setting")
    @Operation(summary = "保存编辑器设置", description = "only 支持 defaultEngine = univer / wps；thirdPlatform = none / wps")
    public R<Boolean> saveSetting(@RequestBody DocEngineConfig body) {
        String def = body == null ? null : body.getDefaultEngine();
        if (!"univer".equals(def) && !"wps".equals(def)) {
            throw new ServiceException("defaultEngine 仅支持 univer / wps");
        }
        String platform = body.getThirdPlatform();
        if (!"none".equals(platform) && !"wps".equals(platform)) {
            throw new ServiceException("thirdPlatform 仅支持 none / wps");
        }
        // 启用 WPS 平台时必须提供 appId / appSecret
        if ("wps".equals(platform)) {
            if (body.getWpsAppId() == null || body.getWpsAppId().isBlank()
                || body.getWpsAppSecret() == null || body.getWpsAppSecret().isBlank()) {
                throw new ServiceException("启用 WPS 平台需填写 appId 与 appSecret");
            }
        }
        settingService.set("default_engine", def);
        settingService.set("third_platform", platform == null ? "none" : platform);
        settingService.set("wps_app_id", body.getWpsAppId() == null ? "" : body.getWpsAppId());
        settingService.set("wps_app_secret", body.getWpsAppSecret() == null ? "" : body.getWpsAppSecret());
        settingService.set("wps_jssdk_url", body.getWpsJssdkUrl() == null ? "" : body.getWpsJssdkUrl());
        return R.data(true);
    }
}
