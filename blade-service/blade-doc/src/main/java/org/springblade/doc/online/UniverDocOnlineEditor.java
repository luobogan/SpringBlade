package org.springblade.doc.online;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.sign.DocUrlSigner;
import org.springblade.doc.vo.DocEditorConfig;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Component;

/**
 * Univer Office 在线编辑器实现（{@link IDocOnlineEditor} 主实现，对应 Office 文档编辑方案之一）。
 *
 * <p>组装 Univer 所需配置：文档类型(sheet/doc/slide)、签名加载 URL、签名保存回调、
 * 只读标志、协作用户信息。文档类型由正文文件扩展名推断，推断不出时默认 sheet。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniverDocOnlineEditor implements IDocOnlineEditor {

    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocUrlSigner signer;

    @Override
    public DocEditorConfig buildConfig(Long docId, String mode, String editorType) {
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
        String ext = extOf(fileName);

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(ext);
        cfg.setEditorType("univer");
        cfg.setDocType(resolveDocType(fileName));
        cfg.setReadOnly(readOnly);
        if (fileId != null) {
            cfg.setDocumentUrl("/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId)));
        }
        if (!readOnly) {
            cfg.setSaveUrl("/doc/editor/save?docId=" + docId + "&token=" + signer.sign("univer:" + docId));
        }
        BladeUser user = SecureUtil.getUser();
        if (user != null) {
            cfg.setUserId(String.valueOf(user.getUserId()));
            cfg.setUserName(user.getUserName());
        }
        log.info("构建 Univer 编辑配置 docId={} mode={} docType={} readOnly={}", docId, editMode, cfg.getDocType(), readOnly);
        return cfg;
    }

    @Override
    public DocEditorConfig buildConfigFromFile(Long docId, String mode, String editorType, Long fileId) {
        DocDetail doc = detailMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }
        if (fileId == null) {
            throw new ServiceException("文件不存在");
        }
        String editMode = (mode == null || mode.isBlank()) ? "view" : mode;
        boolean readOnly = "view".equalsIgnoreCase(editMode);

        // 文件名取自该附件文件记录，回退到文档标题
        String fileName = doc.getDocSubject();
        DocImageFile image = imageFileMapper.selectOne(
            Wrappers.<DocImageFile>lambdaQuery().eq(DocImageFile::getFileId, fileId));
        if (image != null && image.getFileName() != null) {
            fileName = image.getFileName();
        }

        DocEditorConfig cfg = new DocEditorConfig();
        cfg.setDocId(docId);
        cfg.setMode(editMode);
        cfg.setFileName(fileName);
        cfg.setFileExt(extOf(fileName));
        cfg.setEditorType("univer");
        cfg.setDocType(resolveDocType(fileName));
        cfg.setReadOnly(true); // 附件预览一律只读，避免误写回文档主文件
        cfg.setDocumentUrl("/doc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId)));
        BladeUser user = SecureUtil.getUser();
        if (user != null) {
            cfg.setUserId(String.valueOf(user.getUserId()));
            cfg.setUserName(user.getUserName());
        }
        log.info("构建 Univer 附件预览配置 docId={} fileId={} docType={}", docId, fileId, cfg.getDocType());
        return cfg;
    }

    private static String extOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 由文件名扩展名推断 Univer 文档类型（白名单，非用户输入拼接） */
    private String resolveDocType(String fileName) {
        if (fileName == null) {
            return "sheet";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv")) {
            return "sheet";
        }
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) {
            return "slide";
        }
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) {
            return "doc";
        }
        return "sheet";
    }
}
