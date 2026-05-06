package org.springblade.mall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = "D:" + File.separator + "workproject" + File.separator + "springbladeandreact" + File.separator + "springBlade";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + basePath + File.separator + "uploads" + File.separator);
    }
}