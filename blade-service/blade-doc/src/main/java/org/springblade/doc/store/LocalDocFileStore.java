package org.springblade.doc.store;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.config.DocProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 本地文件存储：fileId（雪花）即文件名，读写 {@code blade.doc.upload-path} 目录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalDocFileStore implements IDocFileStore {

    private final DocProperties properties;

    @Override
    public Optional<byte[]> download(Long fileId) {
        if (fileId == null) {
            return Optional.empty();
        }
        Path p = Paths.get(properties.getUploadPath(), String.valueOf(fileId));
        if (!Files.isRegularFile(p)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(p));
        } catch (IOException e) {
            log.error("读取内容文件失败 fileId={}", fileId, e);
            return Optional.empty();
        }
    }

    @Override
    public Long save(byte[] content) {
        if (content == null || content.length == 0) {
            throw new ServiceException("文件内容为空");
        }
        Long fileId = IdWorker.getId();
        Path p = Paths.get(properties.getUploadPath(), String.valueOf(fileId));
        try {
            Files.createDirectories(p.getParent());
            Files.write(p, content);
            return fileId;
        } catch (IOException e) {
            log.error("写入内容文件失败 fileId={}", fileId, e);
            throw new ServiceException("文件写入失败");
        }
    }
}
