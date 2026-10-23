package org.springblade.workflow.odoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公文(odoc)模块配置。
 *
 * <ul>
 *   <li>{@code doc-server-url}：OnlyOffice 文档服务器（DOCS），为空则前端隐藏在线编辑、只走上传/下载。</li>
 *   <li>{@code public-base-url}：本服务对 OnlyOffice 可达的基础地址（回调用），形如
 *       {@code http://host:81/blade-workflow}。</li>
 *   <li>{@code upload-path}：本地文件存储目录（正文/模板/套红产物/版式/签章文件）。
 *       默认与本系统既有附件上传同目录 {@code D:/workproject/springbladeandreact/springBlade/uploads/odoc}；
 *       fileId 用雪花 ID 作文件名，免依赖 blade-resource。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "odoc")
public class OdocProperties {

	/** OnlyOffice 文档服务器地址，如 http://192.168.1.5:8080 */
	private String docServerUrl = "";

	/** 本服务对外可达基础地址（OnlyOffice 回调用），如 http://192.168.1.5:81/blade-workflow */
	private String publicBaseUrl = "";

	/** 在线编辑语言 */
	private String lang = "zh-CN";

	/** 本地文件存储目录（odoc 文件） */
	private String uploadPath = "D:/workproject/springbladeandreact/springBlade/uploads/odoc";

	/**
	 * 版式转换厂商：none=使用内置 PDF 渲染器（离线默认），或 shuke/yozo/fuxin/fuxin-gsdk
	 * （需在 blade-workflow-odoc-layout 接入对应 SDK）。空值等同 none。
	 */
	private String layoutVendor = "none";

	/**
	 * 内置 PDF 渲染器所用中文字体目录（CJK TTF）。留空则自动探测常用位置
	 * （Windows C:/Windows/Fonts、Linux /usr/share/fonts 等）。
	 */
	private String pdfFontPath = "";

	/**
	 * 内容/回调 URL 签名密钥（加固 OnlyOffice / 浏览器直连的 content/callback 端点）。
	 * 生产须显式配置高熵值（Nacos: odoc.url-sign-secret）；缺省仅用于本地开发。
	 */
	private String urlSignSecret = "odoc-dev-secret-change-me";

	/** URL 签名有效期（秒），默认 5 分钟 */
	private int urlSignTtlSeconds = 300;
}
