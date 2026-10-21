package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例「相关资源」条目（对齐 ecology 请求页「相关资源」页签）
 *
 * <p>与 ecology 一样**不单独建关联表**：运行时按「资源类表单字段」从业务数据抽取，
 * 因此表单里填了相关流程/相关文档/附件，本页签即可见（只读汇总）。</p>
 */
@Data
@Schema(description = "实例相关资源条目")
public class WfInstanceResourceVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 资源类型：1=相关流程 2=相关文档 3=相关附件 */
	@Schema(description = "资源类型 1相关流程 2相关文档 3相关附件")
	private Integer resType;

	@Schema(description = "资源ID（流程实例ID / 文档ID / 附件文件ID）")
	private String resId;

	@Schema(description = "资源名称（流程标题 / 文档标题 / 附件名）")
	private String resName;

	@Schema(description = "创建人ID")
	private String creator;

	@Schema(description = "创建时间")
	private String createTime;

	/** 文档打开用 docId；附件下载用 fileId（当前与 resId 同值，预留扩展） */
	@Schema(description = "附加ID（文档=docId；附件=fileId）")
	private String extraId;
}
