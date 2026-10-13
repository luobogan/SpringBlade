package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocMould;

import java.util.List;

/**
 * 套红状态信息：文档是否已套红 + 当前版本 + 可用套红模板 + 是否强制套红。
 */
@Data
@Schema(description = "套红状态信息")
public class OdocTaohongInfoVO {

	@Schema(description = "正文ID")
	private Long docId;

	@Schema(description = "是否已套红 1是 0否")
	private Integer hasUsedTemplet;

	@Schema(description = "当前版本")
	private WfOdocDocVersion currentVersion;

	@Schema(description = "可用套红模板（mouldType=2 启用）")
	private List<WfOdocMould> moulds;

	@Schema(description = "套红节点是否强制套红 1是")
	private Integer mustTaohong;
}
