package org.springblade.doc.online;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.wps.WpsEditorConfig;
import org.springblade.common.wps.WpsFileId;
import org.springblade.common.wps.WpsSettings;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.config.WpsSettingStore;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.vo.DocEditorConfig;
import org.springframework.stereotype.Component;

/**
 * WPS 在线编辑 SDK（WPS 文档中台 / kdocs jssdk）实现（{@link IDocOnlineEditor} 备选方案，对应 Office 文档编辑之二）。
 *
 * <p>与 Univer 共存：同为 Office 文档的可选在线编辑方案，由控制器据 {@code editor=wps} 入参选择。
 * 仅当 {@link DocProperties.Wps#getAppId()}/{@code appSecret} 均非空时才启用（见 {@link #isEnabled()}）。</p>
 *
 * <p>前端 jssdk 的 {@code fileId} 采用<b>稳定的文档级 id（=docId）</b>——WPS 中台据此作为逻辑文件标识，
 * 实际正文版本由 {@code WpsCallbackController} 的 upload 回调管理；回调鉴权 token 由
 * {@link DocUrlSigner}（底层 {@code WpsUrlSigner}）按 {@code wps:{fileId}} 签发，
 * 中台经 {@code X-WebOffice-Token} 回传后由 {@code WpsCallbackController#authGuard} 硬校验。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WpsDocOnlineEditor implements IDocOnlineEditor {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocUrlSigner signer;
    private final WpsSettingStore settings;
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
        return getEffectiveWps().isConfigured();
    }

    /** 读取当前生效的 WPS 配置：统一委托 WpsSettingStore（库表优先、空白回退兜底） */
    public WpsSettings getEffectiveWps() {
        return settings.current();
    }

    @Override
    public DocEditorConfig buildConfig(Long docId, String mode, String editorType) {
        WpsSettings wps = getEffectiveWps();
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

        // WPS 统一改造 P1-1.7：后端一律先组装公共契约 WpsEditorConfig，
        // 再投影到 DocEditorConfig 的 wps* 兼容字段（前端 P2.6 统一后删除投影）。
        WpsEditorConfig wc = new WpsEditorConfig();
        wc.setDomain(WpsFileId.DOMAIN_DOC);
        wc.setMode(editMode);
        wc.setFileName(fileName);
        wc.setFileExt(extOf(fileName));
        wc.setReadOnly(readOnly);
        wc.setAppId(wps.getAppId());
        // 存量会话沿用纯数字 fileId（WpsFileId.legacy），不改变已下发给 WPS 中台的形态
        wc.setFileId(String.valueOf(docId));
        wc.setPermission(permission);
        wc.setUserId(uid);
        wc.setTimestamp(ts);
        wc.setToken(signer.sign("wps:" + docId));
        wc.setJssdkUrl(wps.getJssdkUrl());
        if (fileId != null) {
            wc.setContentUrl("/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId)));
        }
        if (user != null) {
            wc.setUserName(user.getUserName());
        }

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(extOf(fileName));
        cfg.setEditorType("wps");
        cfg.setReadOnly(readOnly);
        applyPublicConfig(cfg, wc);
        // 自有保存回调（jssdk 之外的回退路径，签名 token 鉴权）
        if (!readOnly) {
            cfg.setWpsSaveUrl("/doc/editor/save?docId=" + docId + "&token=" + signer.sign("wps:" + docId));
        }
        if (wc.getContentUrl() != null) {
            cfg.setDocumentUrl(wc.getContentUrl());
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
        WpsSettings wps = getEffectiveWps();
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

        WpsEditorConfig wc = new WpsEditorConfig();
        wc.setDomain(WpsFileId.DOMAIN_DOC);
        wc.setMode(editMode);
        wc.setFileName(fileName);
        wc.setFileExt(extOf(fileName));
        wc.setReadOnly(true); // 附件预览一律只读
        wc.setAppId(wps.getAppId());
        wc.setFileId(String.valueOf(fileId));
        wc.setPermission(permission);
        wc.setUserId(uid);
        wc.setTimestamp(ts);
        wc.setToken(signer.sign("wps:" + fileId));
        wc.setJssdkUrl(wps.getJssdkUrl());
        wc.setContentUrl("/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId)));
        if (user != null) {
            wc.setUserName(user.getUserName());
        }

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(extOf(fileName));
        cfg.setEditorType("wps");
        cfg.setReadOnly(true);
        applyPublicConfig(cfg, wc);
        cfg.setDocumentUrl(wc.getContentUrl());
        if (user != null) {
            cfg.setUserId(uid);
            cfg.setUserName(user.getUserName());
        }
        log.info("构建 WPS 附件预览配置 docId={} fileId={}", docId, fileId);
        return cfg;
    }

    /** 公共契约 → 兼容投影（P2.6 前端统一后可整体删除） */
    private static void applyPublicConfig(DocEditorConfig cfg, WpsEditorConfig wc) {
        cfg.setWpsConfig(wc);
        cfg.setWpsAppId(wc.getAppId());
        cfg.setWpsFileId(wc.getFileId());
        cfg.setWpsFileName(wc.getFileName());
        cfg.setWpsPermission(wc.getPermission());
        cfg.setWpsUserId(wc.getUserId());
        cfg.setWpsTimestamp(wc.getTimestamp());
        cfg.setWpsToken(wc.getToken());
        cfg.setWpsJsSdkUrl(wc.getJssdkUrl());
        cfg.setWpsFileUrl(wc.getContentUrl());
    }

    private static String extOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
