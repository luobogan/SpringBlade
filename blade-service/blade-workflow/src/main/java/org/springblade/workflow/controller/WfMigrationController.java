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
 *
 * @deprecated <b>2026-11-09 已弃用</b>：任务台账扩展任务已一次性迁至 {@code wf_ext_task}，
 *     本端点所属的「数据迁移/回填/对账」能力不再作为过渡手段使用（用户决策：一次性完成、不做双写与存量迁移）。
 *     代码保留仅供追溯/排障，端点仍在但<b>不应再调用</b>；后续 P6 终态再考虑物理删除。
 */
@Deprecated
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
