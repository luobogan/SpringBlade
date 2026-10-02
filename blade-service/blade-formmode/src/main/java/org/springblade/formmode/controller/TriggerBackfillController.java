package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.formmode.service.IApprovalTriggerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程触发改造专用控制器：「以 Flowable 为唯一事实源」改造的存量迁移。
 *
 * <p>提供一次性跨库回填接口，把 mode_triggerworkflowset.workflowid（wf_process_definition.id）
 * 转换为 workflow_key（procKey）。仅工作流管理员可触发。</p>
 */
@RestController
@RequestMapping("/api/blade-formmode/trigger-backfill")
@Tag(name = "触发键回填", description = "「以 Flowable 为唯一事实源」改造的存量迁移")
@RequiredArgsConstructor
public class TriggerBackfillController {

	private final IApprovalTriggerService approvalTriggerService;

	@PostMapping("/workflow-key")
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	public R<Integer> backfillWorkflowKey() {
		return R.data(approvalTriggerService.backfillWorkflowKey());
	}

}
