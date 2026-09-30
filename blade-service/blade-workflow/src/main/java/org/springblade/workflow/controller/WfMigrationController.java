package org.springblade.workflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.WfBackfillResult;
import org.springblade.workflow.dto.WfReconcileResult;
import org.springblade.workflow.job.WfDefinitionBackfillJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 去 {@code wf_*} 表迁移管理端点（T-5 / P1）。
 *
 * <p>提供一次性定义期回填与双轨对账的手动触发入口（仅流程管理员）。
 * 定时回填由 {@code blade.workflow.backfill.enabled} 开关控制（默认关闭）。</p>
 */
@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
@Slf4j
public class WfMigrationController {

	private final WfDefinitionBackfillJob backfillJob;

	@GetMapping("/backfill")
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	public R<WfBackfillResult> backfill(
		@RequestParam(required = false, defaultValue = "false") boolean force,
		@RequestParam(required = false) Long defId) {
		return R.data(backfillJob.backfillAll(force, defId));
	}

	@GetMapping("/reconcile")
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	public R<WfReconcileResult> reconcile() {
		return R.data(backfillJob.reconcileAll());
	}

}
