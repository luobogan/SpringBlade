package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 双轨对账结果（T-5，只读告警）。
 *
 * <p>比较「引擎 BPMN 扩展」与「wf_* 源表」的定义期语义差异；不一致仅记录，不修复、不改写。</p>
 */
@Data
@Schema(description = "定义期双轨对账结果")
public class WfReconcileResult {

	@Schema(description = "参与对账的流程定义总数")
	private int total;

	@Schema(description = "一致数")
	private int matched;

	@Schema(description = "不一致数")
	private int mismatched;

	@Schema(description = "不一致明细")
	private List<String> details;

	public WfReconcileResult() {
	}

	public WfReconcileResult(int total, int matched, int mismatched, List<String> details) {
		this.total = total;
		this.matched = matched;
		this.mismatched = mismatched;
		this.details = details;
	}
}
