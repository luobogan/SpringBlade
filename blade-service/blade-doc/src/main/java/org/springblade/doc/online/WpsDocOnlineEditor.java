package org.springblade.doc.online;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.sign.WpsSign;
import org.springblade.doc.vo.DocEditorConfig;
import org.springframework.stereotype.Component;

/**
 * WPS 在线编辑 SDK（WPS 文档中台 / kdocs jssdk）实现（{@link IDocOnlineEditor} 备选方案，对应 Office 文档编辑之二）。
 *
 * <p>与 Univer 共存：同为 Office 文档的可选在线编辑方案，由控制器据 {@code editor=wps} 入参选择。
 * 仅当 {@link DocProperties.Wps#getAppId()}/{@code appSecret} 均非空时才启用（见 {@link #isEnabled()}）。</p>
 *
 * <p>前端 jssdk 的 {@code fileId} 采用<b>稳定的文档级 id（=docId）</b>——WPS 中台据此作为逻辑文件标识，
 * 实际正文版本由 {@code WpsCallbackController} 的 upload 回调管理；签名规则见 {@link WpsSign}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WpsDocOnlineEditor implements IDocOnlineEditor {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocUrlSigner signer;
    private final DocProperties docProperties;
    private final IDocEngineSettingService settingService;

    /**
     * 是否启用 WPS 方案：需在设置中启用第三方平台=WPS，且 appId/appSecret 均非空。
     * 配置优先级：库表设置（doc_engine_setting）> application.yml。
     * 注意：若库表未配置 third_platform（历史部署沿用 application.yml），则按 application.yml 的 WPS 开关判断，保证平滑过渡。
     */
    public boolean isEnabled() {
        String platform = settingService.get("third_platform", null);
        if (platform != null && !"wps".equals(platform)) {
            return false; // 前端明确关闭第三方平台
        }
        DocProperties.Wps eff = getEffectiveWps();
        boolean hasCred = eff.getAppId() != null && !eff.getAppId().isBlank()
            && eff.getAppSecret() != null && !eff.getAppSecret().isBlank();
        // 库表未配置平台时，沿用 application.yml：WPS 已配即启用
        return hasCred;
    }

    /** 读取当前生效的 WPS 配置（库表设置优先，缺省回退到 application.yml） */
    public DocProperties.Wps getEffectiveWps() {
        DocProperties.Wps src = docProperties.getWps();
        DocProperties.Wps eff = new DocProperties.Wps();
        String appId = settingService.get("wps_app_id", null);
        eff.setAppId(appId == null ? src.getAppId() : appId);
        String appSecret = settingService.get("wps_app_secret", null);
        eff.setAppSecret(appSecret == null ? src.getAppSecret() : appSecret);
        String jssdkUrl = settingService.get("wps_jssdk_url", null);
        eff.setJssdkUrl(jssdkUrl == null ? src.getJssdkUrl() : jssdkUrl);
        String publicBase = settingService.get("wps_public_base", null);
        eff.setPublicBase(publicBase == null ? src.getPublicBase() : publicBase);
        return eff;
    }

    @Override
    public DocEditorConfig buildConfig(Long docId, String mode, String editorType) {
        DocProperties.Wps wps = getEffectiveWps();
        if (!isEnabled()) {
            throw new ServiceException("WPS 编辑方案未启用（未配置 app-id/app-secret）");
        }
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        String editMode = (mode == null || mode.isBlank()) ? "edit" : mode;
        boolean readOnly = "view".equalsIgnoreCase(editMode);

        String fileName = doc.getDocSubject();
        Long fileId = doc.getMainFileId();
        DocImageFile image = null;
        if (fileId != null) {
            image = imageFileMapper.selectOne(
                Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, fileId));
            if (image != null && image.getFileName() != null) {
                fileName = image.getFileName();
            }
        }
        long ts = System.currentTimeMillis() / 1000;
        BladeUser user = SecureUtil.getUser();
        String uid = user == null ? "0" : String.valueOf(user.getUserId());
        String permission = readOnly ? "read" : "write";

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(extOf(fileName));
        cfg.setEditorType("wps");
        cfg.setReadOnly(readOnly);
        // WPS 文档中台 jssdk 标准参数（fileId 用稳定的文档级 id）
        cfg.setWpsAppId(wps.getAppId());
        cfg.setWpsFileId(String.valueOf(docId));
        cfg.setWpsFileName(fileName);
        cfg.setWpsPermission(permission);
        cfg.setWpsUserId(uid);
        cfg.setWpsTimestamp(ts);
        cfg.setWpsToken(signer.sign("wps:" + docId));
        cfg.setWpsJsSdkUrl(wps.getJssdkUrl());
        // 我方自有加载/保存（jssdk 之外的回退/自有保存回调，签名 token 鉴权）
        if (fileId != null) {
            String contentUrl = "/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId));
            cfg.setDocumentUrl(contentUrl);
            cfg.setWpsFileUrl(contentUrl);
        }
        if (!readOnly) {
            cfg.setWpsSaveUrl("/doc/editor/save?docId=" + docId + "&token=" + signer.sign("wps:" + docId));
        }
        if (user != null) {
            cfg.setUserId(uid);
            cfg.setUserName(user.getUserName());
        }
        log.info("构建 WPS 编辑配置 docId={} mode={} appId={} permission={} fileId={}", docId, editMode, wps.getAppId(), permission, docId);
        return cfg;
    }

    @Override
    public DocEditorConfig buildConfigFromFile(Long docId, String mode, String editorType, Long fileId) {
        DocProperties.Wps wps = getEffectiveWps();
        if (!isEnabled()) {
            throw new ServiceException("WPS 编辑方案未启用（未配置 app-id/app-secret）");
        }
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        if (fileId == null) {
            throw new ServiceException("文件不存在");
        }
        String editMode = (mode == null || mode.isBlank()) ? "view" : mode;
        boolean readOnly = "view".equalsIgnoreCase(editMode);

        String fileName = doc.getDocSubject();
        DocImageFile image = imageFileMapper.selectOne(
            Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, fileId));
        if (image != null && image.getFileName() != null) {
            fileName = image.getFileName();
        }
        long ts = System.currentTimeMillis() / 1000;
        BladeUser user = SecureUtil.getUser();
        String uid = user == null ? "0" : String.valueOf(user.getUserId());
        String permission = "read";

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(extOf(fileName));
        cfg.setEditorType("wps");
        cfg.setReadOnly(true); // 附件预览一律只读
        cfg.setWpsAppId(wps.getAppId());
        cfg.setWpsFileId(String.valueOf(fileId));
        cfg.setWpsFileName(fileName);
        cfg.setWpsPermission(permission);
        cfg.setWpsUserId(uid);
        cfg.setWpsTimestamp(ts);
        cfg.setWpsToken(signer.sign("wps:" + fileId));
        cfg.setWpsJsSdkUrl(wps.getJssdkUrl());
        String contentUrl = "/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId));
        cfg.setDocumentUrl(contentUrl);
        cfg.setWpsFileUrl(contentUrl);
        if (user != null) {
            cfg.setUserId(uid);
            cfg.setUserName(user.getUserName());
        }
        log.info("构建 WPS 附件预览配置 docId={} fileId={} fileId={}", docId, fileId, fileId);
        return cfg;
    }

    private static String extOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
