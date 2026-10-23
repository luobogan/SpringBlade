package org.springblade.doc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：附件落盘到 springBlade/uploads/{docId}/{文件名}，经此映射对外提供。
 * 配合网关 secure.skip-url 白名单（/api/blade-doc/uploads/**）实现免鉴权直链，供前端原生 iframe 预览。
 * 仅服务附件目录，主文档正文（./doc-files，token 签名端点）不受影响。
 */
@Configuration
@RequiredArgsConstructor
public class DocWebConfig implements WebMvcConfigurer {

    private final DocProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = properties.getAttachmentPath();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        // 网关 StripPrefix=2：/api/blade-doc/uploads/{docId}/{file} -> /uploads/{docId}/{file}
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + base)
            .setCachePeriod(3600);
        // 兜底：网关仅剥离 /api（StripPrefix=1）时下行路径带 /blade-doc 前缀
        registry.addResourceHandler("/blade-doc/uploads/**")
            .addResourceLocations("file:" + base)
            .setCachePeriod(3600);
    }
}
