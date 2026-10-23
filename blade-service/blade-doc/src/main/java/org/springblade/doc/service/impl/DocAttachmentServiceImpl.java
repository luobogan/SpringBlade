package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.doc.config.DocProperties;
import org.springblade.doc.service.IDocAttachmentService;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 附件实现：读写均经权限判定（fail-closed），文件字节走 {@link IDocFileStore}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocAttachmentServiceImpl implements IDocAttachmentService {

    private final DocImageFileMapper imageFileMapper;
    private final DocPermissionService permissionService;
    private final IDocFileStore fileStore;
    private final DocProperties properties;

    @Override
    public List<DocImageFile> list(Long docId) {
        if (!permissionService.canRead(docId)) {
            throw new ServiceException("无权限查看该文档");
        }
        return imageFileMapper.selectList(Wrappers.<DocImageFile>lambdaQuery()
            .eq(DocImageFile::getDocId, docId)
            .orderByDesc(DocImageFile::getVersionId));
    }

    @Override
    public DocImageFile upload(Long docId, byte[] content, String fileName, Integer docFileType) {
        if (!permissionService.canManage(docId)) {
            throw new ServiceException("无权限上传该文档附件");
        }
        if (content == null || content.length == 0) {
            throw new ServiceException("文件内容为空");
        }
        // 本地附件：落盘到 springBlade/uploads/{docId}/{fileName}（带扩展名），预览走网关白名单静态直链（无需云端）
        String safeName = sanitize(fileName);
        Path dir = Paths.get(properties.getAttachmentPath(), String.valueOf(docId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ServiceException("附件目录创建失败");
        }
        Path target = dir.resolve(safeName);
        // 防目录穿越：解析后必须仍在 dir 之内
        if (!target.normalize().startsWith(dir.normalize())) {
            throw new ServiceException("非法文件名");
        }
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new ServiceException("文件写入失败");
        }
        DocImageFile f = new DocImageFile();
        f.setDocId(docId);
        f.setFileName(fileName);
        f.setFileSize((long) content.length);
        f.setDocFileType(docFileType == null ? 2 : docFileType);
        f.setFilePath(docId + "/" + safeName);
        imageFileMapper.insert(f);
        log.info("上传附件(本地) docId={} path={} name={} size={}", docId, f.getFilePath(), fileName, content.length);
        return f;
    }

    /** 去除路径分隔符与父目录引用，避免目录穿越 */
    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String n = name.replaceAll("[\\\\/]", "_").replaceAll("\\.{2,}", "_").trim();
        return n.isEmpty() ? "file" : n;
    }

    @Override
    public DocImageFile registerExternal(Long docId, String ossFileId, String ossFileUrl, String fileName, Long fileSize, Integer docFileType) {
        if (!permissionService.canManage(docId)) {
            throw new ServiceException("无权限上传该文档附件");
        }
        if (ossFileUrl == null || ossFileUrl.isBlank()) {
            throw new ServiceException("外部文件URL为空");
        }
        DocImageFile f = new DocImageFile();
        f.setDocId(docId);
        f.setOssFileId(ossFileId);
        f.setOssFileUrl(ossFileUrl);
        f.setFileName(fileName);
        f.setFileSize(fileSize);
        f.setDocFileType(docFileType == null ? 2 : docFileType);
        f.setIsExtFile(1);
        imageFileMapper.insert(f);
        log.info("登记系统通用附件 docId={} ossFileId={} name={} url={}", docId, ossFileId, fileName, ossFileUrl);
        return f;
    }

    @Override
    public DocImageFile rename(Long id, String fileName) {
        DocImageFile f = imageFileMapper.selectById(id);
        if (f == null) {
            throw new ServiceException("附件不存在");
        }
        if (!permissionService.canManage(f.getDocId())) {
            throw new ServiceException("无权限修改该文档附件");
        }
        f.setFileName(fileName);
        imageFileMapper.updateById(f);
        return f;
    }

    @Override
    public boolean delete(Long id) {
        DocImageFile f = imageFileMapper.selectById(id);
        if (f == null) {
            throw new ServiceException("附件不存在");
        }
        if (!permissionService.canManage(f.getDocId())) {
            throw new ServiceException("无权限删除该文档附件");
        }
        if (f.getFilePath() != null && !f.getFilePath().isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(properties.getAttachmentPath(), f.getFilePath()));
            } catch (IOException e) {
                log.warn("删除附件物理文件失败 path={}", f.getFilePath(), e);
            }
        }
        return imageFileMapper.deleteById(id) > 0;
    }
}
