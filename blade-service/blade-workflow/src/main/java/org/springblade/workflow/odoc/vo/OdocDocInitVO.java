package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.common.wps.WpsEditorConfig;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.vo.OdocEditorConfigVO;

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

	@Schema(description = "编辑器类型：onlyoffice / wps（WPS 统一改造 P2）")
	private String editorType;

	@Schema(description = "在线编辑配置（OnlyOffice）；不可编辑或未部署 DOCS 时为 null")
	private OdocEditorConfigVO editorConfig;

	@Schema(description = "WPS WebOffice 配置（WPS 已配置且 editor=wps 时非 null；P2 新增）")
	private WpsEditorConfig wpsEditorConfig;

	@Schema(description = "是否可在线编辑")
	private Boolean canEdit;
}
