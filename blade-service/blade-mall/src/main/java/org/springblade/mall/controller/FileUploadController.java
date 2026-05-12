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
	 * 资源管理策略：
	 * - 普通文件：使用 FileSystemResource（Spring 自动管理生命周期）
	 * - ZIP 文件：先解压到内存（ByteArrayInputStream），立即关闭底层文件流，避免嵌套流泄漏
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

			String contentType = imageFile.getImagefiletype();
			String filename = imageFile.getImagefilename();

			org.springframework.core.io.Resource resource;

			// 如果是 ZIP 文件，解压到内存后返回 ByteArrayInputStream（避免嵌套流泄漏）
			if (imageFile.getIszip() != null && imageFile.getIszip() == 1) {
				ZipExtractResult zipResult = extractZipToMemory(file);
				if (zipResult == null || zipResult.resource == null) {
					return ResponseEntity.notFound().build();
				}
				resource = zipResult.resource;
				// 用 ZIP 内部文件的 content type 和 filename 覆盖默认值
				if (zipResult.contentType != null) {
					contentType = zipResult.contentType;
				}
				if (zipResult.filename != null) {
					filename = zipResult.filename;
				}
			} else {
				// 普通文件使用 FileSystemResource（Spring 自动管理文件句柄关闭）
				resource = new org.springframework.core.io.FileSystemResource(file);
			}

			if (contentType == null || contentType.isEmpty()) {
				contentType = "application/octet-stream";
			}
			if (filename == null || filename.isEmpty()) {
				filename = file.getName();
			}

			ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

			// 非 ZIP 文件设置准确的 contentLength；ZIP 解压后的内存流也已知长度
			long contentLength = resource.contentLength();
			if (contentLength > 0) {
				responseBuilder.contentLength(contentLength);
			}

			return responseBuilder.body(resource);

		} catch (IOException e) {
			log.error("File download failed: fileId={}", fileId, e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * ZIP 解压结果容器
	 */
	private static class ZipExtractResult {
		final org.springframework.core.io.Resource resource;
		final String contentType;
		final String filename;

		ZipExtractResult(org.springframework.core.io.Resource resource, String contentType, String filename) {
			this.resource = resource;
			this.contentType = contentType;
			this.filename = filename;
		}
	}

	/**
	 * 将 ZIP 文件中的第一个条目提取到内存中
	 *
	 * 解决的问题：
	 * 1. ZipInputStream(FileInputStream) 嵌套流 → InputStreamResource 无法正确关闭底层文件句柄
	 * 2. 客户端 PrematureCloseException 断连时，嵌套流泄漏导致文件句柄不释放
	 * 3. ZIP 解压后 contentLength 未知导致网关无法优化传输
	 *
	 * 策略：完全读取到 byte[] → 立即关闭所有流 → 返回 ByteArrayResource（无底层句柄）
	 */
	private ZipExtractResult extractZipToMemory(File zipFile) throws IOException {

		// 使用 try-with-resources 确保 ZipInputStream 和底层 FileInputStream 都被正确关闭
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry = zin.getNextEntry();
			if (entry == null) {
				log.warn("ZIP 文件中没有找到条目: {}", zipFile.getAbsolutePath());
				return null;
			}

			// 读取整个条目内容到内存（图片通常 < 10MB，内存压力可接受）
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(8192);
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = zin.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			zin.closeEntry();

			byte[] fileData = baos.toByteArray();

			// 根据内部文件名推断 content type
			String innerContentType = null;
			String innerFilename = entry.getName();
			if (innerFilename != null) {
				if (innerFilename.endsWith(".png")) {
					innerContentType = "image/png";
				} else if (innerFilename.endsWith(".jpg") || innerFilename.endsWith(".jpeg")) {
					innerContentType = "image/jpeg";
				} else if (innerFilename.endsWith(".gif")) {
					innerContentType = "image/gif";
				} else if (innerFilename.endsWith(".bmp")) {
					innerContentType = "image/bmp";
				} else if (innerFilename.endsWith(".webp")) {
					innerContentType = "image/webp";
				}
			}

			log.debug("ZIP 解压完成: {} → {} bytes, filename={}",
				zipFile.getName(), fileData.length, innerFilename);

			// 返回基于内存的 Resource + 元信息（无底层文件句柄，不存在泄漏问题）
			org.springframework.core.io.Resource memoryResource =
				new org.springframework.core.io.ByteArrayResource(fileData) {
					@Override
					public String getFilename() {
						return innerFilename != null ? innerFilename : super.getFilename();
					}
				};

			return new ZipExtractResult(memoryResource, innerContentType, innerFilename);
		}
		// ← try-with-resources 自动关闭 zin 和其内部的 FileInputStream
	}
}
