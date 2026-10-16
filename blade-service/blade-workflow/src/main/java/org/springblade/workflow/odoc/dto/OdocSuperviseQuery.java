package org.springblade.workflow.odoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公文督办查询条件。
 *
 * <p>superviseType：0=我发起的督办（督办人=本人）；1=我被督办（发起人=本人）。</p>
 */
@Data
@Schema(description = "公文督办查询条件")
public class OdocSuperviseQuery {

	@Schema(description = "公文类型 1发文 2收文 3签报；空=全部")
	private Integer officalType;

	@Schema(description = "督办视角 0我发起的督办 1我被督办")
	private Integer superviseType = 0;

	@Schema(description = "状态 0督办中 1已办结 2已取消；空=全部")
	private Integer status;

	@Schema(description = "当前页")
	private Long current = 1L;

	@Schema(description = "每页条数")
	private Long size = 10L;

}
