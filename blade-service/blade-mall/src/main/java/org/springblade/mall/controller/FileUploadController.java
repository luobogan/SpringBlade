package org.springblade.mall.controller;

import org.springblade.common.constant.CommonConstant;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME)
public class FileUploadController extends BladeController {

    @Value("${blade.prop.upload-path:uploads}")
    private String uploadPath;

    @Value("${blade.prop.upload-domain:http://localhost:8085}")
    private String uploadDomain;

    // 初始化上传目录
    @PostConstruct
    public void init() {
        // 确保上传目录是绝对路径
        File uploadDir = new File(uploadPath);
        if (!uploadDir.isAbsolute()) {
            // 如果是相对路径，使用当前工作目录作为基础
            uploadPath = System.getProperty("user.dir") + File.separator + uploadPath;
            uploadDir = new File(uploadPath);
        }

        // 确保上传目录存在
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        System.out.println("Upload directory: " + uploadPath);
    }

    /**
     * 上传图片（管理后台）
     */
    @PostMapping("/admin/upload/image")
    public ResponseEntity<R<String>> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam(value = "type", defaultValue = "product") String type) {
        try {
            System.out.println("Upload path: " + uploadPath);
            System.out.println("Upload domain: " + uploadDomain);
            System.out.println("Upload type: " + type);

            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
            }

            // 检查文件类型
            String contentType = file.getContentType();
            System.out.println("File content type: " + contentType);
            // 检查文件扩展名
            String originalFilename = file.getOriginalFilename();
            System.out.println("Original filename: " + originalFilename);
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body(R.fail("文件名无效"));
            }
            String fileExtension = originalFilename.toLowerCase().substring(originalFilename.lastIndexOf('.'));
            System.out.println("File extension: " + fileExtension);

            // 允许的文件类型
            boolean isAllowedExtension = ".jpg,.jpeg,.png,.ico".contains(fileExtension);
            boolean isAllowedContentType = contentType != null && (contentType.startsWith("image/") || "image/x-icon".equals(contentType) || "image/vnd.microsoft.icon".equals(contentType));

            System.out.println("Is allowed extension: " + isAllowedExtension);
            System.out.println("Is allowed content type: " + isAllowedContentType);

            if (!isAllowedExtension && !isAllowedContentType) {
                return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
            }

            // 确保上传目录存在
            File uploadDir = new File(uploadPath + File.separator + type);
            System.out.println("Upload directory: " + uploadDir.getAbsolutePath());
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                System.out.println("Directory created: " + created);
            }

            // 生成唯一文件名
            System.out.println("Original filename: " + originalFilename);
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString() + extension;
            System.out.println("Generated filename: " + fileName);

            // 保存文件
            File dest = new File(uploadDir, fileName);
            System.out.println("Destination file: " + dest.getAbsolutePath());
            file.transferTo(dest);
            System.out.println("File transferred successfully");

            // 返回文件URL（使用相对路径）
            String fileUrl = "/uploads/" + type + "/" + fileName;
            System.out.println("File URL: " + fileUrl);
            return ResponseEntity.ok(R.data(fileUrl, "上传成功"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传文件（支持视频等多种类型）
     */
    @PostMapping("/admin/upload/file")
    public ResponseEntity<R<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Upload path: " + uploadPath);

            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null) {
                return ResponseEntity.badRequest().body(R.fail("文件类型无效"));
            }

            // 允许的文件类型
            boolean isAllowedType = contentType.startsWith("image/") ||
                                   contentType.startsWith("video/") ||
                                   contentType.startsWith("audio/") ||
                                   contentType.equals("application/pdf");
            if (!isAllowedType) {
                return ResponseEntity.badRequest().body(R.fail("不支持的文件类型"));
            }

            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body(R.fail("文件名无效"));
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString() + extension;

            // 保存文件
            File dest = new File(uploadDir, fileName);
            file.transferTo(dest);

            // 返回文件URL（使用相对路径）
            String fileUrl = "/uploads/" + fileName;
            return ResponseEntity.ok(R.data(fileUrl, "上传成功"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传单个文件（前端用户使用）
     */
    @PostMapping("/upload/single")
    public ResponseEntity<R<String>> uploadSingleFile(@RequestParam("file") MultipartFile file, @RequestParam("type") String type) {
        try {
            System.out.println("Upload path: " + uploadPath);
            System.out.println("File type: " + type);

            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(R.fail("文件不能为空"));
            }

            // 检查文件类型
            String contentType = file.getContentType();
            System.out.println("File content type: " + contentType);
            if (contentType == null) {
                return ResponseEntity.badRequest().body(R.fail("文件类型无效"));
            }

            // 对于头像类型，只允许图片
            if ("avatar".equals(type)) {
                if (!contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
                }
            }

            // 检查文件大小（限制为5MB）
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(R.fail("文件大小不能超过5MB"));
            }

            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            System.out.println("Upload directory: " + uploadDir.getAbsolutePath());
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                System.out.println("Directory created: " + created);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            System.out.println("Original filename: " + originalFilename);
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body(R.fail("文件名无效"));
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString() + extension;
            System.out.println("Generated filename: " + fileName);

            // 保存文件
            File dest = new File(uploadDir, fileName);
            System.out.println("Destination file: " + dest.getAbsolutePath());
            file.transferTo(dest);
            System.out.println("File transferred successfully");

            // 返回文件URL（使用相对路径）
            String fileUrl = "/uploads/" + fileName;
            System.out.println("File URL: " + fileUrl);
            return ResponseEntity.ok(R.data(fileUrl, "上传成功"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(R.fail("上传失败: " + e.getMessage()));
        }
    }
}



