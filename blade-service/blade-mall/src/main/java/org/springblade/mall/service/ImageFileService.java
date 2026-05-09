package org.springblade.mall.service;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.ImageFileEntity;
import org.springblade.mall.entity.ImageFile;
import org.springblade.mall.mapper.ImageFileMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;

/**
 * 文件上传服务（blade-mall实现）
 * 负责将文件元数据保存到ImageFile表
 *
 * @author Blade
 */
@Slf4j
@Service
public class ImageFileService {

	private static ImageFileService instance;

	@Autowired
	private ImageFileMapper imageFileMapper;

	@PostConstruct
	private void init() {
		instance = this;
	}

	public static ImageFileService getInstance() {
		return instance;
	}

	/**
	 * 检查是否可用
	 */
	public boolean isAvailable() {
		return imageFileMapper != null;
	}

	/**
	 * 保存文件记录到数据库
	 *
	 * @param entity 文件实体
	 * @return 文件ID
	 */
	public Long saveFile(ImageFileEntity entity) {
		if (imageFileMapper == null) {
			log.warn("ImageFileMapper未初始化，无法保存文件记录到数据库");
			return null;
		}

		ImageFile imageFile = new ImageFile();
		BeanUtils.copyProperties(entity, imageFile);

		if (imageFile.getImagefileid() == null) {
			imageFile.setImagefileid(System.currentTimeMillis());
		}
		if (imageFile.getImagefileused() == null) {
			imageFile.setImagefileused(1);
		}
		if (imageFile.getDownloads() == null) {
			imageFile.setDownloads(0);
		}
		if (imageFile.getSecretlevel() == null) {
			imageFile.setSecretlevel(4);
		}
		imageFile.setCreatedat(new Date());
		imageFile.setUpdatedat(new Date());

		imageFileMapper.insert(imageFile);
		log.info("文件记录已保存到数据库: fileId={}, fileName={}", imageFile.getImagefileid(), imageFile.getImagefilename());
		return imageFile.getImagefileid();
	}

	/**
	 * 构建ImageFileEntity对象（静态方法）
	 */
	public static ImageFileEntity buildEntity(String filename, String contentType, String filePath,
											  Long fileSize, boolean isZip, boolean isEncrypt, String aesCode) {
		return buildEntity(filename, contentType, filePath, fileSize, isZip, isEncrypt, aesCode, null);
	}

	public static ImageFileEntity buildEntity(String filename, String contentType, String filePath,
											  Long fileSize, boolean isZip, boolean isEncrypt, String aesCode, String tenantId) {
		ImageFileEntity entity = new ImageFileEntity();
		entity.setImagefilename(filename);
		entity.setImagefiletype(contentType);
		entity.setFilerealpath(filePath);
		entity.setFilesize(fileSize);
		entity.setIszip(isZip ? 1 : 0);
		entity.setIsaesencrypt(isEncrypt ? 1 : 0);
		entity.setAescode(aesCode != null ? aesCode : "");
		entity.setTenantid(tenantId != null ? tenantId : "");
		return entity;
	}
}
