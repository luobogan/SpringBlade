package org.springblade.mall.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.boot.file.BladeFile;
import org.springblade.core.boot.file.BladeFileProxyFactory;
import org.springblade.core.boot.file.ImageFileEntity;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.mall.entity.ImageFile;
import org.springblade.mall.mapper.ImageFileMapper;
import org.springblade.mall.service.ImageFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@Tag(name = "文件上传", description = "文件上传接口")
public class FileUploadController extends BladeController {

	private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

	@Value("${blade.prop.upload-domain:http://localhost:8085}")
	private String uploadDomain;

	@Autowired
	private ImageFileService imageFileService;

	@Autowired
	private ImageFileMapper imageFileMapper;

	@PostMapping("/admin/upload/image")
	public ResponseEntity<R<Map<String, Object>>> uploadImage(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "type", defaultValue = "product") String type) {
		try {
			log.debug("Upload type: {}", type);

			if (file.isEmpty()) {
				return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
			}

			String contentType = file.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
			}

			long fileSize = file.getSize();
			String fileContentType = file.getContentType();
			String tenantId = SecureUtil.getTenantId();

			BladeFile bladeFile = getFile(file, type);
			bladeFile.setTenantId(tenantId);
			BladeFileProxyFactory.UploadResult result = bladeFile.transferEnhanced();

			boolean isZip = result != null && result.getFilePath() != null && result.getFilePath().endsWith(".zip");
			boolean isEncrypt = result != null && result.getAesCode() != null;
			String aesCode = (result != null) ? result.getAesCode() : null;

			Long fileId = null;

			if ((result == null || result.getFileId() == null) && imageFileService != null) {
				ImageFileEntity entity = ImageFileService.buildEntity(
					bladeFile.getOriginalFileName(),
					fileContentType,
					bladeFile.getUploadPath(),
					fileSize,
					isZip,
					isEncrypt,
					aesCode,
					tenantId
				);
				fileId = imageFileService.saveFile(entity);
				log.info("文件已通过 MallImageFileService 保存到ImageFile表，fileId={}, tenantId={}", fileId, tenantId);
			} else if (result != null && result.getFileId() != null) {
				fileId = result.getFileId();
			}

			Map<String, Object> response = new HashMap<>();
			response.put("id", fileId);
			response.put("url", bladeFile.getUploadVirtualPath());
			response.put("filename", bladeFile.getOriginalFileName());
			response.put("filesize", fileSize);
			response.put("isZip", isZip);

			log.info("Image uploaded successfully: path={}, fileId={}, zip={}, encrypt={}, tenantId={}",
				bladeFile.getUploadVirtualPath(),
				fileId,
				isZip, isEncrypt, tenantId);

			return ResponseEntity.ok(R.data(response, "上传成功"));

		} catch (Exception e) {
			log.error("Upload image failed", e);
			return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
		}
	}

	@PostMapping("/admin/upload/file")
	public ResponseEntity<R<Map<String, Object>>> uploadFile(@RequestParam("file") MultipartFile file) {
		try {
			if (file.isEmpty()) {
				return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
			}

			long fileSize = file.getSize();
			String fileContentType = file.getContentType();
			String tenantId = SecureUtil.getTenantId();

			BladeFile bladeFile = getFile(file, "common");
			bladeFile.setTenantId(tenantId);
			BladeFileProxyFactory.UploadResult result = bladeFile.transferEnhanced();

			boolean isZip = result != null && result.getFilePath() != null && result.getFilePath().endsWith(".zip");
			boolean isEncrypt = result != null && result.getAesCode() != null;
			String aesCode = (result != null) ? result.getAesCode() : null;

			Long fileId = null;

			if ((result == null || result.getFileId() == null) && imageFileService != null) {
				ImageFileEntity entity = ImageFileService.buildEntity(
					bladeFile.getOriginalFileName(),
					fileContentType,
					bladeFile.getUploadPath(),
					fileSize,
					isZip,
					isEncrypt,
					aesCode,
					tenantId
				);
				fileId = imageFileService.saveFile(entity);
				log.info("文件已通过 MallImageFileService 保存到ImageFile表，fileId={}, tenantId={}", fileId, tenantId);
			} else if (result != null && result.getFileId() != null) {
				fileId = result.getFileId();
			}

			Map<String, Object> response = new HashMap<>();
			response.put("id", fileId);
			response.put("url", bladeFile.getUploadVirtualPath());
			response.put("filename", bladeFile.getOriginalFileName());
			response.put("filesize", fileSize);
			response.put("isZip", isZip);

			log.info("File uploaded successfully: path={}, zip={}, encrypt={}, tenantId={}",
				bladeFile.getUploadVirtualPath(), isZip, isEncrypt, tenantId);

			return ResponseEntity.ok(R.data(response, "上传成功"));

		} catch (Exception e) {
			log.error("Upload file failed", e);
			return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
		}
	}

	@PostMapping("/single")
	public ResponseEntity<R<Map<String, Object>>> uploadSingleFile(
			@RequestParam("file") MultipartFile file,
			@RequestParam("type") String type) {
		try {
			log.debug("File type: {}", type);

			if (file.isEmpty()) {
				return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
			}

			String contentType = file.getContentType();
			if ("avatar".equals(type)) {
				if (contentType == null || !contentType.startsWith("image/")) {
					return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
				}
			}

			long fileSize = file.getSize();
			String fileContentType = file.getContentType();
			String tenantId = SecureUtil.getTenantId();

			long maxSize = 5 * 1024 * 1024;
			if (fileSize > maxSize) {
				return ResponseEntity.badRequest().body(R.fail("文件大小不能超过5MB"));
			}

			BladeFile bladeFile = getFile(file, type);
			bladeFile.setTenantId(tenantId);
			BladeFileProxyFactory.UploadResult result = bladeFile.transferEnhanced();

			boolean isZip = result != null && result.getFilePath() != null && result.getFilePath().endsWith(".zip");
			boolean isEncrypt = result != null && result.getAesCode() != null;
			String aesCode = (result != null) ? result.getAesCode() : null;

			Long fileId = null;

			if ((result == null || result.getFileId() == null) && imageFileService != null) {
				ImageFileEntity entity = ImageFileService.buildEntity(
					bladeFile.getOriginalFileName(),
					fileContentType,
					bladeFile.getUploadPath(),
					fileSize,
					isZip,
					isEncrypt,
					aesCode,
					tenantId
				);
				fileId = imageFileService.saveFile(entity);
				log.info("文件已通过 MallImageFileService 保存到ImageFile表，fileId={}, tenantId={}", fileId, tenantId);
			} else if (result != null && result.getFileId() != null) {
				fileId = result.getFileId();
			}

			Map<String, Object> response = new HashMap<>();
			response.put("id", fileId);
			response.put("url", bladeFile.getUploadVirtualPath());
			response.put("filename", bladeFile.getOriginalFileName());
			response.put("filesize", fileSize);
			response.put("isZip", isZip);

			log.info("Single file uploaded successfully: path={}, zip={}, encrypt={}, tenantId={}",
				bladeFile.getUploadVirtualPath(), isZip, isEncrypt, tenantId);

			return ResponseEntity.ok(R.data(response, "上传成功"));

		} catch (Exception e) {
			log.error("Upload single file failed", e);
			return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
		}
	}

	@GetMapping("/file/download/{fileId}")
	public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable Long fileId) {
		try {
			ImageFile imageFile = imageFileMapper.selectById(fileId);
			if (imageFile == null) {
				return ResponseEntity.notFound().build();
			}

			String filePath = imageFile.getFilerealpath();
			if (filePath == null || filePath.isEmpty()) {
				return ResponseEntity.notFound().build();
			}

			File file = new File(filePath);
			if (!file.exists()) {
				log.warn("File not found on disk: {}", filePath);
				return ResponseEntity.notFound().build();
			}

			InputStream inputStream = new FileInputStream(file);
			org.springframework.core.io.InputStreamResource resource =
				new org.springframework.core.io.InputStreamResource(inputStream);

			String filename = imageFile.getImagefilename();
			if (filename == null || filename.isEmpty()) {
				filename = file.getName();
			}

			String contentType = imageFile.getImagefiletype();
			if (contentType == null || contentType.isEmpty()) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.contentLength(file.length())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.body(resource);

		} catch (IOException e) {
			log.error("File download failed: fileId={}", fileId, e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
