package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公文统计（按公文类型汇总在途/办结等，对应 E9 OdocReportAction.getCountData 基础维度）。
 *
 * <p>status 口径同 {@code WfInstance}：0运行中 1通过 2不通过 3撤销 4暂停 5草稿。</p>
 */
@Data
@Schema(description = "公文统计(按类型)")
public class OdocStatisticVO {

	@Schema(description = "公文类型 1发文 2收文 3签报")
	private Integer officalType;

	@Schema(description = "总数")
	private long total;

	@Schema(description = "运行中")
	private long running;

	@Schema(description = "通过/办结")
	private long approved;

	@Schema(description = "不通过")
	private long rejected;

	@Schema(description = "撤销")
	private long canceled;

	@Schema(description = "暂停")
	private long suspended;

	@Schema(description = "草稿")
	private long draft;

}
