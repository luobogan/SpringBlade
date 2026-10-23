package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.online.UniverDocOnlineEditor;
import org.springblade.doc.online.WpsDocOnlineEditor;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.doc.service.IDocEngineSettingService;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.store.IDocFileStore;
import org.springblade.doc.vo.DocEditorConfig;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 内容引擎 - 多格式在线编辑 / 预览调度。
 *
 * <p>三种方案清晰分工、可共存（由文件扩展名 + {@code editor} 入参决定）：
 * <ul>
 *   <li>{@code editorType=univer}：Office 文档在线编辑（Univer，开源）；</li>
 *   <li>{@code editorType=wps}：Office 文档在线编辑（WPS kdocs jssdk，需配置 appId/secret）；</li>
 *   <li>{@code editorType=pdfjs}：PDF 文件只读预览（PDF.js，开源）。</li>
 * </ul>
 * 加载/保存 URL 均经 {@link DocUrlSigner} 签名（防枚举、带时效）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/doc/editor")
@RequiredArgsConstructor
@Tag(name = "内容引擎-在线编辑", description = "Univer / WPS / PDF.js 三方案调度 + 签名保存回调")
public class DocOnlineEditorController {

    private final UniverDocOnlineEditor univerEditor;
    private final WpsDocOnlineEditor wpsEditor;
    private final DocPermissionService docPermissionService;
    private final DocUrlSigner signer;
    private final IDocFileStore fileStore;
    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final IDocEngineSettingService engineSettingService;

    /** Office 文档扩展名（Univer / WPS 适用） */
    private static boolean isOffice(String ext) {
        if (ext == null) {
            return false;
        }
        switch (ext) {
            case "xlsx": case "xls": case "csv":
            case "docx": case "doc":
            case "pptx": case "ppt": case "wps": case "et":
                return true;
            default:
                return false;
        }
    }

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/config")
    @Operation(summary = "获取编辑器配置", description = "按文件类型+方案返回 univer/wps/pdfjs 配置；支持 fileId 预览指定附件（只读）")
    public R<DocEditorConfig> config(@RequestParam Long docId,
                                     @RequestParam(required = false) String mode,
                                     @RequestParam(required = false, defaultValue = "auto") String editor,
                                     @RequestParam(required = false) Long fileId) {
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        // 默认编辑方案（editor=auto 时据此选择；WPS 还需已配置 appId/appSecret 才生效）
        String defaultEngine = engineSettingService.get("default_engine", "univer");

        // 附件预览：按指定 fileId 构建（只读），与主文件编辑互不干扰
        if (fileId != null) {
            if (!docPermissionService.canRead(docId)) {
                throw new ServiceException("无权限查看该文档");
            }
            DocImageFile image = imageFileMapper.selectOne(
                Wrappers.<DocImageFile>lambdaQuery()
                    .eq(DocImageFile::getFileId, fileId)
                    .or().eq(DocImageFile::getId, fileId));
            String fileName = (image != null && image.getFileName() != null) ? image.getFileName() : doc.getDocSubject();
            String filePath = (image != null) ? image.getFilePath() : null;
            // 本地附件：springBlade/uploads/{docId}/{fileName}，网关白名单静态直链，前端原生预览（无云端依赖）
            if (filePath != null && !filePath.isBlank()) {
                DocEditorConfig cfg = new DocEditorConfig();
                cfg.setDocId(docId);
                cfg.setMode("view");
                cfg.setFileName(fileName);
                cfg.setEditorType("pdfjs");
                cfg.setReadOnly(true);
                cfg.setPreview(true);
                cfg.setIsExtFile(0);
                cfg.setDocumentUrl("/api/blade-doc/uploads/" + filePath);
                return R.data(cfg);
            }
            // 外部文件(OSS 直链)：直接返回 OSS 链接，由前端原生预览（规避本地存储 401 与跨域）
            if (image != null && image.getIsExtFile() != null && image.getIsExtFile() == 1
                && image.getOssFileUrl() != null && !image.getOssFileUrl().isBlank()) {
                DocEditorConfig cfg = new DocEditorConfig();
                cfg.setDocId(docId);
                cfg.setMode("view");
                cfg.setFileName(fileName);
                cfg.setEditorType("pdfjs");
                cfg.setReadOnly(true);
                cfg.setPreview(true);
                cfg.setIsExtFile(1);
                cfg.setDocumentUrl(image.getOssFileUrl());
                return R.data(cfg);
            }
            String ext = extOf(fileName);
            if ("pdf".equals(ext)) {
                return R.data(buildPdfConfig(docId, fileId, fileName, ext));
            }
            if (useWps(editor, defaultEngine)) {
                return R.data(wpsEditor.buildConfigFromFile(docId, "view", "wps", fileId));
            }
            // 默认 / auto / univer → Univer（附件只读预览）
            return R.data(univerEditor.buildConfigFromFile(docId, "view", "univer", fileId));
        }

        // 主文件（现有逻辑不变）
        Long mainFileId = doc.getMainFileId();
        String fileName = doc.getDocSubject();
        if (mainFileId != null) {
            DocImageFile image = imageFileMapper.selectOne(
                Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, mainFileId));
            if (image != null && image.getFileName() != null) {
                fileName = image.getFileName();
            }
        }
        String ext = extOf(fileName);

        // 权限：PDF 预览只需可读；Office 编辑需可管理
        if ("pdf".equals(ext)) {
            if (!docPermissionService.canRead(docId)) {
                throw new ServiceException("无权限查看该文档");
            }
            return R.data(buildPdfConfig(docId, mainFileId, fileName, ext));
        }
        if (!docPermissionService.canManage(docId)) {
            throw new ServiceException("无权限编辑该文档");
        }
        if (useWps(editor, defaultEngine)) {
            return R.data(wpsEditor.buildConfig(docId, mode, "wps"));
        }
        // 默认 / auto / univer → Univer（保持现有 Excel/docx 支持不变）
        return R.data(univerEditor.buildConfig(docId, mode, "univer"));
    }

    /** editor=wps 明确指定，或 editor=auto 且默认方案为 wps 且 WPS 已启用 → 走 WPS */
    private boolean useWps(String editor, String defaultEngine) {
        return wpsEditor.isEnabled()
            && ("wps".equals(editor) || ("auto".equalsIgnoreCase(editor) && "wps".equals(defaultEngine)));
    }

    private static String extOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocEditorConfig buildPdfConfig(Long docId, Long fileId, String fileName, String ext) {
        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode("view");
        cfg.setFileName(fileName);
        cfg.setFileExt(ext);
        cfg.setEditorType("pdfjs");
        cfg.setReadOnly(true);
        cfg.setPreview(true);
        if (fileId != null) {
            cfg.setDocumentUrl("/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId)));
        }
        return cfg;
    }

    /**
     * 保存回调（Univer / WPS 共用）：无用户会话，靠 {@code token} 鉴权（签名 + 时效）。
     * 签名资源前缀为 {@code univer:} 或 {@code wps:} + docId。
     * 保存后落盘为新版本附件（DocImageFile），并回写文档主文件ID。
     */
    @PreAuth("permitAll()")
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "编辑器保存回调", description = "签名 token 鉴权（无会话），另存为新版本")
    public R<DocImageFile> save(@RequestParam Long docId,
                                @RequestParam String token,
                                @RequestParam("file") MultipartFile file) {
        if (!signer.verify("univer:" + docId, token) && !signer.verify("wps:" + docId, token)) {
            throw new ServiceException("编辑回调签名无效或已过期");
        }
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("待保存文件为空");
        }
        try {
            byte[] content = file.getBytes();
            Long newFileId = fileStore.save(content);
            DocImageFile rec = new DocImageFile();
            rec.setDocId(docId);
            rec.setVersionId(newFileId);
            rec.setFileId(newFileId);
            rec.setFileName(file.getOriginalFilename());
            rec.setFileSize((long) content.length);
            rec.setDocFileType(2);
            imageFileMapper.insert(rec);
            // 回写主文件ID，使下次编辑加载最新版本
            doc.setMainFileId(newFileId);
            detailMapper.updateById(doc);
            return R.data(rec);
        } catch (IOException e) {
            throw new ServiceException("保存文件读取失败");
        }
    }
}
