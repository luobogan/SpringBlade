package org.springblade.mall.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springblade.core.tenant.TenantUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 动态租户上传文件访问配置
 */
@Configuration
public class TenantUploadConfig implements WebMvcConfigurer {

    private static final String BASE_PATH = "D:" + File.separator + "workproject" + File.separator + "springbladeandreact" + File.separator + "springBlade" + File.separator + "uploads";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantUploadInterceptor())
                .addPathPatterns("/uploads/**");
    }

    @Component
    public static class TenantUploadInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String requestPath = request.getRequestURI();

            if (requestPath.startsWith("/uploads/")) {
                String filePath = requestPath.substring("/uploads".length());
                File file = findFile(filePath);

                if (file != null && file.exists()) {
                    serveFile(response, file);
                    return false;
                }
            }
            return true;
        }

        private File findFile(String relativePath) {
            Path fullPath = Paths.get(BASE_PATH, relativePath);
            if (Files.exists(fullPath)) {
                return fullPath.toFile();
            }

            String tenantId = getCurrentTenantId();
            if (tenantId != null && !tenantId.isEmpty()) {
                Path tenantPath = Paths.get(BASE_PATH, tenantId, relativePath);
                if (Files.exists(tenantPath)) {
                    return tenantPath.toFile();
                }
            }

            return null;
        }

        private String getCurrentTenantId() {
            try {
                return TenantUtil.getTenantId();
            } catch (Exception e) {
                return null;
            }
        }

        private void serveFile(HttpServletResponse response, File file) throws IOException {
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            response.setContentType(contentType);
            response.setContentLengthLong(file.length());

            try (java.io.InputStream inputStream = Files.newInputStream(file.toPath());
                 java.io.OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }
    }
}