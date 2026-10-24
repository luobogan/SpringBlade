package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 商城附件上传（已统一到通用上传组件 blade-file）
 *
 * <p>保留原有端点路径以兼容前端；实际存储、ZIP 打包、AES 加密、元数据落库
 * 全部由 {@link IFileStorageService} 承担，压缩/加密/存储方式支持前端逐次指定。</p>
 *
 * @author Blade
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "文件上传接口（统一通用上传组件）")
public class FileUploadController {

	private final IFileStorageService fileStorageService;

	@PostMapping("/admin/upload/image")
	@Operation(summary = "上传图片", description = "type=业务目录；needZip/needEncrypt/storage 可选")
	public ResponseEntity<R<Map<String, Object>>> uploadImage(
		@RequestParam("file") MultipartFile file,
		@RequestParam(value = "type", defaultValue = "product") String type,
		@RequestParam(value = "needZip", required = false) Boolean needZip,
		@RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
		@RequestParam(value = "storage", required = false) FileStorageType storage) {
		String contentType = file == null ? null : file.getContentType();
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
		}
		if (contentType == null || !contentType.startsWith("image/")) {
			return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
		}
		return doUpload(file, type, needZip, needEncrypt, storage);
	}

	@PostMapping("/admin/upload/file")
	@Operation(summary = "上传附件", description = "needZip/needEncrypt/storage 可选")
	public ResponseEntity<R<Map<String, Object>>> uploadFile(
		@RequestParam("file") MultipartFile file,
		@RequestParam(value = "needZip", required = false) Boolean needZip,
		@RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
		@RequestParam(value = "storage", required = false) FileStorageType storage) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
		}
		return doUpload(file, "common", needZip, needEncrypt, storage);
	}

	@PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "上传单个文件", description = "type=业务目录；needZip/needEncrypt/storage 可选")
	public ResponseEntity<R<Map<String, Object>>> uploadSingleFile(
		@RequestParam("file") MultipartFile file,
		@RequestParam("type") String type,
		@RequestParam(value = "needZip", required = false) Boolean needZip,
		@RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
		@RequestParam(value = "storage", required = false) FileStorageType storage) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
		}
		if ("avatar".equals(type)) {
			String contentType = file.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
			}
		}
		return doUpload(file, type, needZip, needEncrypt, storage);
	}

	private ResponseEntity<R<Map<String, Object>>> doUpload(MultipartFile file, String dir,
														   Boolean needZip, Boolean needEncrypt, FileStorageType storage) {
		try {
			FileUploadOptions options = new FileUploadOptions()
				.dir(dir)
				.storage(storage)
				.needZip(needZip)
				.needEncrypt(needEncrypt)
				.tenantId(tenantId())
				.comefrom("mall");
			FileUploadResult result = fileStorageService.upload(file, options);
			return ResponseEntity.ok(R.data(toLegacyResult(result), "上传成功"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
		}
	}

	/**
	 * 保持与改造前一致的返回结构，避免前端回归
	 */
	private Map<String, Object> toLegacyResult(FileUploadResult result) {
		Map<String, Object> data = new HashMap<>();
		data.put("id", result.getId());
		data.put("url", "/api/blade-mall/file/download/" + result.getId());
		data.put("filename", result.getFilename());
		data.put("filesize", result.getFilesize());
		data.put("isZip", result.isZip());
		data.put("isEncrypt", result.isEncrypt());
		data.put("storage", result.getStorage() == null ? null : result.getStorage().name());
		return data;
	}

	private String tenantId() {
		try {
			String tenantId = SecureUtil.getTenantId();
			return (tenantId == null || tenantId.isBlank()) ? "000000" : tenantId;
		} catch (Exception e) {
			return "000000";
		}
	}

}