package org.springblade.mall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${blade.prop.upload-path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.isAbsolute()) {
            uploadPath = System.getProperty("user.dir") + File.separator + uploadPath;
            uploadDir = new File(uploadPath);
        }
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + File.separator);
    }
}
