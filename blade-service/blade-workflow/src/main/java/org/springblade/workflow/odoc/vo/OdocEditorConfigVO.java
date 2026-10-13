package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * OnlyOffice 编辑器配置（后端计算、前端直接喂给 OnlyOffice API）。
 *
 * <p>前端用 {@link #docServerUrl} 加载 {@code web-apps/apps/api/documents/api.js}，
 * 其余字段对齐 OnlyOffice 官方 config 结构；空（未部署 DOCS）时整体为 null，
 * 前端据此隐藏在线编辑、只保留上传/下载。</p>
 */
@Data
@Schema(description = "OnlyOffice 编辑器配置")
public class OdocEditorConfigVO {

	@Schema(description = "OnlyOffice 文档服务器地址（前端用于加载 api.js）")
	private String docServerUrl;

	@Schema(description = "文档类型 word/spreadsheet/presentation")
	private String documentType = "word";

	@Schema(description = "document 段")
	private Document document;

	@Schema(description = "editorConfig 段")
	private EditorConfig editorConfig;

	@Data
	@Schema(description = "OnlyOffice document 段")
	public static class Document {
		private String fileType;
		private String key;
		private String title;
		private String url;
		private Permissions permissions = new Permissions();
	}

	@Data
	@Schema(description = "document.permissions 段")
	public static class Permissions {
		private Boolean edit = true;
		private Boolean download = true;
		private Boolean print = true;
	}

	@Data
	@Schema(description = "OnlyOffice editorConfig 段")
	public static class EditorConfig {
		private String callbackUrl;
		private String lang;
		private String mode; // edit / view
		private User user = new User();
	}

	@Data
	@Schema(description = "editorConfig.user 段")
	public static class User {
		private String id;
		private String name;
	}
}
