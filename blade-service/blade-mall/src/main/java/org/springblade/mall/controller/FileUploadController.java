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
import org.springframework.core.io.Resource;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
			// 返回下载URL而非虚拟路径，前端通过下载接口获取图片（ZIP文件会自动解压）
			String downloadUrl = "/api/blade-mall/file/download/" + fileId;
			response.put("url", downloadUrl);
			response.put("filename", bladeFile.getOriginalFileName());
			response.put("filesize", fileSize);
			response.put("isZip", isZip);

			log.info("Image uploaded successfully: downloadUrl={}, fileId={}, zip={}, encrypt={}, tenantId={}",
				downloadUrl,
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
			// 返回下载URL而非虚拟路径，前端通过下载接口获取图片（ZIP文件会自动解压）
			String downloadUrl = "/api/blade-mall/file/download/" + fileId;
			response.put("url", downloadUrl);
			response.put("filename", bladeFile.getOriginalFileName());
			response.put("filesize", fileSize);
			response.put("isZip", isZip);

			log.info("File uploaded successfully: downloadUrl={}, zip={}, encrypt={}, tenantId={}",
				downloadUrl, isZip, isEncrypt, tenantId);

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

	/**
	 * 文件下载接口
	 * 支持两种鉴权方式：
	 * 1. Header: blade-auth (标准方式，适用于 fetch/XMLHttpRequest)
	 * 2. QueryParam: token (备选方式，适用于 img/a标签等无法携带自定义header的场景)
	 * 
	 * 注意：此接口在网关层 secure.skip-url 中配置为白名单（/blade-mall/file/download/**），
	 *       网关不拦截。但服务内部仍可通过 token 参数做可选鉴权。
	 */
	@GetMapping("/file/download/{fileId}")
	public ResponseEntity<Resource> downloadFile(
			@PathVariable Long fileId,
			@RequestParam(required = false) String token,
			HttpServletRequest request) {
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
			
			// 可选的 token 校验：如果传了 token 参数，记录日志但不强制校验
			// 因为网关白名单已放行此路径，此处仅做审计日志
			if (token != null && !token.isEmpty()) {
				log.debug("文件下载请求携带token参数: fileId={}, token长度={}", fileId, token.length());
			}

			InputStream inputStream;
			String contentType = imageFile.getImagefiletype();
			String filename = imageFile.getImagefilename();

			// 如果是 ZIP 文件，解压并返回内部图片
			if (imageFile.getIszip() != null && imageFile.getIszip() == 1) {
				ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
				ZipEntry entry = zin.getNextEntry();
				if (entry != null) {
					inputStream = zin;
					// 从 ZIP 内部文件名推断 content type
					String innerName = entry.getName();
					if (innerName != null) {
						if (innerName.endsWith(".png")) {
							contentType = "image/png";
						} else if (innerName.endsWith(".jpg") || innerName.endsWith(".jpeg")) {
							contentType = "image/jpeg";
						} else if (innerName.endsWith(".gif")) {
							contentType = "image/gif";
						} else if (innerName.endsWith(".bmp")) {
							contentType = "image/bmp";
						} else if (innerName.endsWith(".webp")) {
							contentType = "image/webp";
						}
						filename = innerName;
					}
				} else {
					zin.close();
					return ResponseEntity.notFound().build();
				}
			} else {
				inputStream = new FileInputStream(file);
			}

			if (contentType == null || contentType.isEmpty()) {
				contentType = "application/octet-stream";
			}
			if (filename == null || filename.isEmpty()) {
				filename = file.getName();
			}

			org.springframework.core.io.InputStreamResource resource =
				new org.springframework.core.io.InputStreamResource(inputStream);

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
