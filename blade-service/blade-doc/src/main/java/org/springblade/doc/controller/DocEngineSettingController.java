package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.common.wps.WpsSettings;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.config.WpsSettingStore;
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
 * 内容引擎 - 编辑器设置：WPS 接入配置的唯一写入口。
 *
 * <p>所有值落 {@code doc_engine_setting} 库表，由 {@link WpsSettingStore} 供
 * blade-doc / blade-workflow 两侧共用（每次现场读库，保存后无需重启）。</p>
 *
 * <p>安全开关（callback-auth-strict / callback-auth-bind-resource）刻意不在此暴露：
 * 网关已放行 /v3/3rd/**，若允许页面关闭硬校验会导致回调端点裸奔。</p>
 */
@RestController
@RequestMapping("/doc/engine")
@RequiredArgsConstructor
@Tag(name = "内容引擎-编辑器设置", description = "默认编辑方案与 WPS 接入配置（唯一配置源）")
public class DocEngineSettingController {

    private final IDocEngineSettingService settingService;
    private final WpsSettingStore settings;
    private final WpsDocOnlineEditor wpsEditor;

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @GetMapping("/setting")
    @Operation(summary = "获取编辑器设置")
    public R<DocEngineConfig> setting() {
        WpsSettings eff = settings.current();
        DocEngineConfig cfg = new DocEngineConfig();
        cfg.setDefaultEngine(settingService.get("default_engine", "univer"));
        String platform = settingService.get("third_platform", null);
        cfg.setThirdPlatform(platform == null ? (wpsEditor.isEnabled() ? "wps" : "none") : platform);
        cfg.setWpsEnabled(wpsEditor.isEnabled());
        cfg.setWpsAppId(eff.getAppId());
        cfg.setWpsAppSecret(eff.getAppSecret());
        cfg.setWpsJssdkUrl(eff.getJssdkUrl());
        cfg.setWpsPublicBase(eff.getPublicBase());
        cfg.setWpsUrlSignSecret(eff.getUrlSignSecret());
        cfg.setWpsUrlSignTtlSeconds(eff.getUrlSignTtlSeconds());
        return R.data(cfg);
    }

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/setting")
    @Operation(summary = "保存编辑器设置")
    public R<Boolean> saveSetting(@RequestBody DocEngineConfig body) {
        if (body == null) {
            throw new ServiceException("参数不能为空");
        }
        String def = body.getDefaultEngine();
        if (!"univer".equals(def) && !"wps".equals(def)) {
            throw new ServiceException("defaultEngine 仅支持 univer / wps");
        }
        String platform = body.getThirdPlatform();
        if (!"none".equals(platform) && !"wps".equals(platform)) {
            throw new ServiceException("thirdPlatform 仅支持 none / wps");
        }
        if ("wps".equals(platform)) {
            if (isBlank(body.getWpsAppId()) || isBlank(body.getWpsAppSecret())) {
                throw new ServiceException("启用 WPS 平台需填写 appId 与 appSecret");
            }
        }
        settingService.set(WpsSettings.K_DEFAULT_ENGINE, def);
        settingService.set(WpsSettings.K_THIRD_PLATFORM, platform);
        settingService.set(WpsSettings.K_APP_ID, str(body.getWpsAppId()));
        settingService.set(WpsSettings.K_APP_SECRET, str(body.getWpsAppSecret()));
        settingService.set(WpsSettings.K_JSSDK_URL, str(body.getWpsJssdkUrl()));
        settingService.set(WpsSettings.K_PUBLIC_BASE, str(body.getWpsPublicBase()));
        settingService.set(WpsSettings.K_URL_SIGN_SECRET, str(body.getWpsUrlSignSecret()));
        if (body.getWpsUrlSignTtlSeconds() != null && body.getWpsUrlSignTtlSeconds() > 0) {
            settingService.set(WpsSettings.K_URL_SIGN_TTL, String.valueOf(body.getWpsUrlSignTtlSeconds()));
        }
        return R.data(true);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String str(String s) {
        return s == null ? "" : s.trim();
    }
}
