package org.springblade.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.config.DocProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * 内容引擎 - 文件上传（已统一到通用上传组件 blade-file）
 *
 * <p>双重校验：① 扩展名白名单（{@link DocProperties#getAllowedFileTypes()}）；
 * ② 大小上限（{@link DocProperties#getMaxUploadSize()} MB）。任一不满足即拒绝。</p>
 *
 * <p>压缩（ZIP）/加密（AES）/存储方式（本地、OSS）支持前端逐次指定，
 * 为空则使用服务端默认（blade.file.storage.default-*）。</p>
 */
@RestController
@RequestMapping("/doc/upload")
@RequiredArgsConstructor
@Tag(name = "内容引擎-上传", description = "文件上传（统一通用上传组件）")
public class DocUploadController {

	private final IFileStorageService fileStorageService;
	private final DocProperties properties;

	@PreAuth(DocConstant.HAS_AUTH)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "上传文件", description = "校验大小与扩展名白名单，返回 fileId")
	public R<Long> upload(@RequestParam("file") MultipartFile file,
						  @RequestParam(value = "needZip", required = false) Boolean needZip,
						  @RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
						  @RequestParam(value = "storage", required = false) FileStorageType storage) {
		if (file == null || file.isEmpty()) {
			throw new ServiceException("文件不能为空");
		}
		String original = file.getOriginalFilename();
		String ext = (original != null && original.contains("."))
			? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
			: "";
		if (!properties.getAllowedFileTypes().contains(ext)) {
			throw new ServiceException("不支持的文件类型: " + ext);
		}
		long maxBytes = (long) properties.getMaxUploadSize() * 1024 * 1024;
		if (file.getSize() > maxBytes) {
			throw new ServiceException("文件超过大小限制 " + properties.getMaxUploadSize() + "MB");
		}
		FileUploadOptions options = new FileUploadOptions()
			.dir("doc")
			.needZip(needZip)
			.needEncrypt(needEncrypt)
			.storage(storage)
			.comefrom("doc");
		FileUploadResult result = fileStorageService.upload(file, options);
		return R.data(result.getId());
	}
}