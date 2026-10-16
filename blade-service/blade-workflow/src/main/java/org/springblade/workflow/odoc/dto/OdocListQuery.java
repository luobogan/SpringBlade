package org.springblade.workflow.odoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 公文待办 / 已办 / 我的请求 查询条件。
 *
 * <p>officalType 经 {@code wf_odoc_request.offical_type} 收敛为公文 + 发文/收文/签报维度
 * （对应 E9 {@code workflow_base.isWorkflowDoc + officalType}）。</p>
 */
@Data
@Schema(description = "公文列表查询条件")
public class OdocListQuery {

	@Schema(description = "公文类型 1发文 2收文 3签报；空=全部公文")
	private Integer officalType;

	@Schema(description = "流程标题模糊")
	private String title;

	@Schema(description = "密级过滤（命中其一）")
	private List<String> secretLevels;

	@Schema(description = "时间范围起点（待办=接收时间，已办=处理时间）")
	private Date beginTime;

	@Schema(description = "时间范围终点")
	private Date endTime;

	@Schema(description = "办理人/发起人（仅流程管理员可指定他人，否则强制收敛为本人）")
	private Long assignee;

	@Schema(description = "当前页")
	private Long current = 1L;

	@Schema(description = "每页条数")
	private Long size = 10L;

}
