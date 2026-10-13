package org.springblade.workflow.odoc.store;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
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
 * 本地文件存储（odoc 默认实现）：文件写到 {@code odoc.upload-path} 目录，
 * fileId（雪花）即文件名，免依赖 blade-resource OSS。
 *
 * <p>与本系统既有附件上传同目录（{@code .../springBlade/uploads}）；后续如需切换到
 * 对象存储，替换本实现即可（接口不变）。</p>
 */
@Slf4j
@Component
public class LocalOdocFileStore implements IOdocFileStore {

	private final OdocProperties properties;
	private final RestTemplate restTemplate = new RestTemplate();

	public LocalOdocFileStore(OdocProperties properties) {
		this.properties = properties;
	}

	private Path dir() {
		return Paths.get(properties.getUploadPath());
	}

	@Override
	public Long store(byte[] bytes, String fileName) {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalStateException("待写入文件为空: " + fileName);
		}
		try {
			Path d = dir();
			Files.createDirectories(d);
			Long fileId = IdWorker.getId();
			String ext = extOf(fileName);
			Path target = d.resolve(fileId + (ext.isEmpty() ? "" : "." + ext));
			Files.write(target, bytes);
			return fileId;
		} catch (IOException e) {
			throw new IllegalStateException("本地写文件失败: " + fileName, e);
		}
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
		return "/odoc/file/" + fileId + "/content";
	}

	@Override
	public String resolveFileName(Long fileId) {
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

	private String extOf(String name) {
		if (name == null) {
			return "";
		}
		int i = name.lastIndexOf('.');
		return i > 0 ? name.substring(i + 1).toLowerCase() : "";
	}
}
