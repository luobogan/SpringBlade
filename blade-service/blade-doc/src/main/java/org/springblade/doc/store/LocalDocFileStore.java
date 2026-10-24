package org.springblade.doc.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.vo.FileDownloadContent;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.doc.config.DocProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 内容文件存储：已统一到通用上传组件（blade-file）
 *
 * <p>新写入的文件走统一组件（imagefile 元数据 + 统一目录 + 可选 ZIP/AES）；
 * 读取时若统一元数据查不到，回退到历史本地目录 {@code blade.doc.upload-path}/{fileId}，
 * 保证改造前的内容文件仍可访问。</p>
 *
 * @author Blade
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalDocFileStore implements IDocFileStore {

	private final IFileStorageService fileStorageService;
	private final DocProperties properties;

	@Override
	public Optional<byte[]> download(Long fileId) {
		if (fileId == null) {
			return Optional.empty();
		}
		try {
			FileDownloadContent content = fileStorageService.download(fileId);
			if (content != null && content.getContent() != null) {
				return Optional.of(content.getContent());
			}
		} catch (Exception e) {
			log.debug("统一文件服务读取失败，回退历史本地文件: fileId={}, msg={}", fileId, e.getMessage());
		}
		Path legacy = Paths.get(properties.getUploadPath(), String.valueOf(fileId));
		if (Files.isRegularFile(legacy)) {
			try {
				return Optional.of(Files.readAllBytes(legacy));
			} catch (IOException e) {
				log.warn("读取历史内容文件失败: fileId={}", fileId, e);
			}
		}
		return Optional.empty();
	}

	@Override
	public Long save(byte[] content) {
		return save(content, "content");
	}

	@Override
	public Long save(byte[] content, String fileName) {
		FileUploadOptions options = new FileUploadOptions().dir("doc");
		FileUploadResult result = fileStorageService.store(content, fileName, options);
		return result.getId();
	}

	@Override
	public boolean exists(Long fileId) {
		return download(fileId).isPresent();
	}

}