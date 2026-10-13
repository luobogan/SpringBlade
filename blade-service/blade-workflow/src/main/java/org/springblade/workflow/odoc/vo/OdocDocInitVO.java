package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;

import java.util.List;

/**
 * 正文初始化返回：文档主记录 + 当前版本 + 版本历史 + 在线编辑配置。
 */
@Data
@Schema(description = "正文初始化返回")
public class OdocDocInitVO {

	@Schema(description = "正文ID")
	private Long docId;

	@Schema(description = "标题")
	private String title;

	@Schema(description = "文档状态 9草稿 1正式")
	private Integer docStatus;

	@Schema(description = "当前版本")
	private WfOdocDocVersion currentVersion;

	@Schema(description = "版本历史（按版本号倒序）")
	private List<WfOdocDocVersion> versions;

	@Schema(description = "在线编辑配置；未部署 OnlyOffice 时为 null")
	private OdocEditorConfigVO editorConfig;

	@Schema(description = "是否可在线编辑（依赖 OnlyOffice 已部署）")
	private Boolean canEdit;
}
