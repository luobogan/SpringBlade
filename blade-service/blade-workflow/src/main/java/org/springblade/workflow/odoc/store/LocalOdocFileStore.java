package org.springblade.workflow.odoc.store;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.vo.FileDownloadContent;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.workflow.odoc.OdocUrlSigner;
import org.springblade.workflow.odoc.config.OdocProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 公文文件存储：已统一到通用上传组件（blade-file）
 *
 * <p>新写入的文件走统一组件（imagefile 元数据 + 统一目录 + 可选 ZIP/AES）；
 * 读取时若统一元数据查不到，回退到历史本地目录 {@code odoc.upload-path}/{fileId}.ext，
 * 保证改造前的公文文件仍可访问。</p>
 *
 * @author Blade
 */
@Slf4j
@Component
public class LocalOdocFileStore implements IOdocFileStore {

	private final OdocProperties properties;
	private final OdocUrlSigner signer;
	private final IFileStorageService fileStorageService;
	private final RestTemplate restTemplate = new RestTemplate();

	public LocalOdocFileStore(OdocProperties properties, OdocUrlSigner signer, IFileStorageService fileStorageService) {
		this.properties = properties;
		this.signer = signer;
		this.fileStorageService = fileStorageService;
	}

	private Path dir() {
		return Paths.get(properties.getUploadPath());
	}

	@Override
	public Long store(byte[] bytes, String fileName) {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalStateException("待写入文件为空: " + fileName);
		}
		FileUploadOptions options = new FileUploadOptions().dir("odoc").comefrom("odoc");
		FileUploadResult result = fileStorageService.store(bytes, fileName, options);
		return result.getId();
	}

	@Override
	public Long storeFromUrl(String sourceUrl, String fileName) {
		ResponseEntity<byte[]> resp = restTemplate.exchange(sourceUrl, HttpMethod.GET, null, byte[].class);
		byte[] bytes = resp.getBody();
		if (bytes == null || bytes.length == 0) {
			throw new IllegalStateException("从来源拉取文件为空: " + sourceUrl);
		}
		return store(bytes, fileName);
	}

	@Override
	public byte[] download(Long fileId) {
		try {
			FileDownloadContent content = fileStorageService.download(fileId);
			if (content != null && content.getContent() != null) {
				return content.getContent();
			}
		} catch (Exception e) {
			log.debug("统一文件服务读取失败，回退历史本地文件: fileId={}, msg={}", fileId, e.getMessage());
		}
		try {
			Path d = dir();
			if (!Files.exists(d)) {
				throw new IllegalStateException("文件不存在: " + fileId);
			}
			try (Stream<Path> s = Files.list(d)) {
				Optional<Path> hit = s.filter(p -> match(p.getFileName().toString(), fileId)).findFirst();
				Path f = hit.orElseThrow(() -> new IllegalStateException("文件不存在: " + fileId));
				return Files.readAllBytes(f);
			}
		} catch (IOException e) {
			throw new IllegalStateException("读取本地文件失败: " + fileId, e);
		}
	}

	@Override
	public String getDownloadUrl(Long fileId) {
		// 预览/下载统一走 blade-workflow 的 content 端点（内部转 fileStore.download）
		// 签名 token 防未授权枚举下载（E9 DOC-P0-4 同款）
		return "/odoc/file/" + fileId + "/content?token=" + signer.sign(String.valueOf(fileId));
	}

	@Override
	public String resolveFileName(Long fileId) {
		try {
			Optional<org.springblade.core.boot.file.store.FileMetadata> metadata = fileStorageService.metadata(fileId);
			if (metadata.isPresent() && metadata.get().getFilename() != null) {
				return metadata.get().getFilename();
			}
		} catch (Exception e) {
			log.debug("查询统一文件元数据失败: fileId={}, msg={}", fileId, e.getMessage());
		}
		try {
			Path d = dir();
			if (!Files.exists(d)) {
				return "";
			}
			try (Stream<Path> s = Files.list(d)) {
				return s.filter(p -> match(p.getFileName().toString(), fileId))
					.map(p -> p.getFileName().toString())
					.findFirst().orElse("");
			}
		} catch (IOException e) {
			return "";
		}
	}

	/** 文件名 = "{fileId}.{ext}" 或 "{fileId}" */
	private boolean match(String name, Long fileId) {
		String id = String.valueOf(fileId);
		return name.equals(id) || name.startsWith(id + ".");
	}

}