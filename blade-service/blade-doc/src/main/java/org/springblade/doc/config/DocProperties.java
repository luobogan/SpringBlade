package org.springblade.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 内容引擎配置（前缀 blade.doc）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blade.doc")
public class DocProperties {

    /** 本地文件存储目录（fileId 即文件名）；生产应改为持久化绝对路径 */
    private String uploadPath = "./doc-files";

    /** 附件本地存储目录（springBlade/uploads/{docId}/{文件名}，带扩展名）；由网关白名单静态直链预览，无需云端 */
    private String attachmentPath = "D:/project/springbladeandreact/springBlade/uploads";

    /** 单文件上传大小限制（MB），对齐 E9 SecCategoryComInfo 缺省 5MB */
    private int maxUploadSize = 5;

    /** 允许上传的文件扩展名白名单（对齐 E9 上传白名单交集；生产经 Nacos 覆盖） */
    private List<String> allowedFileTypes = Arrays.asList(
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "ofd", "wps", "et");

    /**
     * 内容/回调 URL 签名密钥（加固 DOC-P0-4 可枚举下载）。
     * 生产须显式配置高熵值（Nacos: blade.doc.url-sign-secret）；缺省仅用于本地开发。
     */
    private String urlSignSecret = "doc-dev-secret-change-me";

    /** URL 签名有效期（秒），默认 5 分钟 */
    private int urlSignTtlSeconds = 300;

    /**
     * 自定义字段加密密钥（AES）。读与写必须使用同一密钥/口径，
     * 从根上避免 E9 §18.4「scope 传错 → 加密被静默跳过 → 明文入库」。生产 Nacos 覆盖为高熵值。
     */
    private String fieldEncryptKey = "doc-field-enc-dev-key";

    /**
     * WPS 在线编辑 SDK（kdocs jssdk）配置。
     * 仅当 appId/appSecret 均非空时才启用 WPS 方案；否则不签发 WPS 配置。
     */
    @Data
    public static class Wps {
        /** WPS 开放平台应用 ID */
        private String appId = "";
        /** WPS 开放平台应用密钥（用于服务端签名） */
        private String appSecret = "";
        /** WebOffice umd.js 地址（默认前端 public 本地部署；可经 Nacos 覆盖为 CDN/私有化地址） */
        private String jssdkUrl = "/web-office-sdk-solution.umd.js";

        /** 公网访问 base（用于拼 download/upload 回调绝对 URL，如 https://www.zwymding.top；留空则按请求推断） */
        private String publicBase = "";
    }

    private Wps wps = new Wps();

    public Wps getWps() {
        return wps;
    }
}
