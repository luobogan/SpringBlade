package org.springblade.mall.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * 文件上传工具类
 */
@Component
public class FileUploadUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUploadUtil.class);

    @Value("${blade.prop.upload-path:uploads}")
    private String uploadPath;

    @Value("${blade.prop.upload-domain:http://localhost:8085}")
    private String uploadDomain;

    private static String STATIC_UPLOAD_PATH;
    private static String STATIC_UPLOAD_DOMAIN;

    @PostConstruct
    public void init() {
        STATIC_UPLOAD_PATH = uploadPath;
        STATIC_UPLOAD_DOMAIN = uploadDomain;

        // 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 创建子目录
        String[] subDirs = {"category", "brand", "product", "member", "common"};
        for (String subDir : subDirs) {
            File dir = new File(uploadPath, subDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    /**
     * 上传 base64 图片并返回文件路径
     *
     * @param base64Image base64 编码的图片数据
     * @param folder      文件夹名称
     * @return 文件访问路径
     */
    public static String uploadBase64Image(String base64Image, String folder) {
        if (base64Image == null || base64Image.isEmpty()) {
            return null;
        }

        // 如果已经是 URL 路径，直接返回
        if (!base64Image.startsWith("data:image")) {
            return base64Image;
        }

        try {
            // 解析 base64 数据
            String[] parts = base64Image.split(",");
            if (parts.length != 2) {
                log.error("Invalid base64 image format");
                return null;
            }

            String base64Data = parts[1];
            String[] mimeTypeParts = parts[0].split(";")[0].split(":");
            if (mimeTypeParts.length < 2) {
                log.error("Invalid base64 image format");
                return null;
            }
            String mimeType = mimeTypeParts[1];
            String extension = getExtensionFromMimeType(mimeType);

            // 生成文件名
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // 构建文件路径
            String folderPath = STATIC_UPLOAD_PATH + File.separator + folder;
            File folderDir = new File(folderPath);
            if (!folderDir.exists()) {
                folderDir.mkdirs();
            }

            String filePath = folderPath + File.separator + fileName;

            // 解码并保存文件
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Path path = Paths.get(filePath);
            Files.write(path, imageBytes);

            // 返回访问路径
            return "/uploads/" + folder + "/" + fileName;

        } catch (Exception e) {
            log.error("Failed to upload base64 image", e);
            return null;
        }
    }

    /**
     * 上传 MultipartFile 并返回文件路径
     *
     * @param file   文件
     * @param folder 文件夹名称
     * @return 文件访问路径
     */
    public static String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            } else {
                extension = "jpg";
            }

            // 生成文件名
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // 构建文件路径
            String folderPath = STATIC_UPLOAD_PATH + File.separator + folder;
            File folderDir = new File(folderPath);
            if (!folderDir.exists()) {
                folderDir.mkdirs();
            }

            String filePath = folderPath + File.separator + fileName;

            // 保存文件
            file.transferTo(new File(filePath));

            // 返回访问路径
            return "/uploads/" + folder + "/" + fileName;

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            return null;
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        try {
            // 提取相对路径
            String relativePath = filePath;
            if (filePath.startsWith("/uploads/")) {
                relativePath = filePath.substring("/uploads/".length());
            }

            String fullPath = STATIC_UPLOAD_PATH + File.separator + relativePath;
            File file = new File(fullPath);

            if (file.exists()) {
                return file.delete();
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    /**
     * 获取完整 URL
     *
     * @param path 相对路径
     * @return 完整 URL
     */
    public static String getFullUrl(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        return STATIC_UPLOAD_DOMAIN + path;
    }

    /**
     * 从 MIME 类型获取文件扩展名
     *
     * @param mimeType MIME 类型
     * @return 文件扩展名
     */
    private static String getExtensionFromMimeType(String mimeType) {
        switch (mimeType) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }
}
