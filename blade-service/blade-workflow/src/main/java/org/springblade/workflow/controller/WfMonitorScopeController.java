package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.entity.WfMonitorScope;
import org.springblade.workflow.service.IWfMonitorScopeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程监控委托范围控制器（对应设计文档 §1.3）
 *
 * <p>管理面：仅流程管理员/超管可配置「谁可以监控哪些流程」。查看面的数据范围裁剪在
 * {@code WfInstanceServiceImpl#monitorList} 内按 {@code IWfMonitorScopeService#resolve} 生效，
 * 委托人经 {@code /monitor/instances}（已放开为 HAS_AUTH）只看授权范围。</p>
 */
@RestController
@RequestMapping("/monitor/scope")
@RequiredArgsConstructor
@Tag(name = "流程监控委托范围", description = "部门/分部/人员级监控委托配置")
public class WfMonitorScopeController {

	private final IWfMonitorScopeService scopeService;

	@GetMapping
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@Operation(summary = "监控委托范围分页列表")
	public R<IPage<WfMonitorScope>> page(
		@Parameter(description = "当前页") @RequestParam(required = false, defaultValue = "1") Integer current,
		@Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "20") Integer pageSize,
		WfMonitorScope condition) {
		return R.data(scopeService.pageScope(current, pageSize, condition));
	}

	@GetMapping("/{id}")
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@Operation(summary = "监控委托范围详情")
	public R<WfMonitorScope> detail(@PathVariable Long id) {
		return R.data(scopeService.getScope(id));
	}

	@PostMapping
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@Operation(summary = "新增监控委托范围")
	public R<Boolean> save(@RequestBody WfMonitorScope scope) {
		return R.data(scopeService.saveScope(scope), "已保存");
	}

	@PutMapping
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@Operation(summary = "更新监控委托范围")
	public R<Boolean> update(@RequestBody WfMonitorScope scope) {
		return R.data(scopeService.updateScope(scope), "已更新");
	}

	@DeleteMapping("/{id}")
	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@Operation(summary = "删除监控委托范围")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(scopeService.removeScope(id), "已删除");
	}
}
