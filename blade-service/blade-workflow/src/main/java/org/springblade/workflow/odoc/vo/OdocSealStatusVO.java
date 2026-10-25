package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公文签署状态视图（供前端待办/签批页展示「签署中 / 去签署 / 已签章」）。
 */
@Data
@Schema(description = "公文签署状态")
public class OdocSealStatusVO {

	@Schema(description = "公文正文ID")
	private Long docId;

	@Schema(description = "用印方案 0自研印章(本地) 1第三方电子签章")
	private Integer sealMode;

	@Schema(description = "签署状态 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败")
	private Integer signStatus;

	@Schema(description = "签署状态文案")
	private String signStatusText;

	@Schema(description = "本地签署流程ID")
	private Long signFlowId;

	@Schema(description = "第三方签署流程ID")
	private String esignFlowId;

	@Schema(description = "签署链接")
	private String signUrl;

	@Schema(description = "归档后的公文版本ID")
	private Long archiveVersionId;

	@Schema(description = "是否已签署完成")
	private Boolean finished;

}
