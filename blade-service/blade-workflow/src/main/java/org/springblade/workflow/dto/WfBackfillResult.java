package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 定义期回填结果（T-5）。
 */
@Data
@Schema(description = "定义期回填结果")
public class WfBackfillResult {

	@Schema(description = "扫描到的流程定义总数")
	private int total;

	@Schema(description = "回填成功数")
	private int ok;

	@Schema(description = "跳过数（已回填 / 无 BPMN）")
	private int skip;

	@Schema(description = "失败数")
	private int fail;

	@Schema(description = "失败明细")
	private List<String> errors;

	public WfBackfillResult() {
	}

	public WfBackfillResult(int total, int ok, int skip, int fail, List<String> errors) {
		this.total = total;
		this.ok = ok;
		this.skip = skip;
		this.fail = fail;
		this.errors = errors;
	}
}
