package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.entity.ModeFormGroup;
import org.springblade.formmode.service.IModeFormGroupService;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 明细表组属性控制器（对齐泛微 E9 modeformgroup）
 *
 * <p>提供明细表「设计期属性」的读写：可新增 / 可编辑 / 可删除 / 可复制行、
 * 打印是否显示序号、是否允许横向滚动。</p>
 *
 * <p><b>定位维度</b>：设计器只有 formId，故按「formId + 明细表序号(order)」定位。</p>
 *
 * <p><b>鉴权</b>：读接口 {@code /detail} 运行期也会被用到（判断明细行按钮是否可用），
 * 只要求「已登录」；写接口 {@code /save} 属设计态，要求流程/建模管理角色。</p>
 *
 * <p><b>与节点权限的关系</b>：本接口是设计期开关；流程节点级明细权限在 blade-workflow 侧
 * （{@code wf:detailTablePerm}）。运行期最终生效结果应为「设计期开关 AND 节点权限」。</p>
 */
@RestController
@RequestMapping("/form-mode-group")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@Tag(name = "明细表组属性", description = "明细表设计期属性（可新增/编辑/删除/复制、打印序号、横向滚动）")
@RequiredArgsConstructor
@Slf4j
public class ModeFormGroupController extends BladeController {

	private final IModeFormGroupService modeFormGroupService;

	/**
	 * 获取明细表组属性
	 */
	@GetMapping("/detail")
	@PreAuth(WorkflowConstant.HAS_AUTH)
	@Operation(summary = "获取明细表组属性",
		description = "按表单ID + 明细表序号取；无配置时返回默认值（允许增删改复制、不打印序号、不横向滚动），不落库")
	public R<ModeFormGroup> detail(
		@Parameter(description = "表单ID") @RequestParam("formId") Long formId,
		@Parameter(description = "明细表序号（对应设计器 detailTables 的序号）")
		@RequestParam("order") Integer order) {
		return R.data(modeFormGroupService.getByFormAndOrder(formId, order));
	}

	/**
	 * 保存明细表组属性（按 formId + dsporder 做 upsert）
	 */
	@PostMapping("/save")
	@Operation(summary = "保存明细表组属性",
		description = "按 formId + dsporder 做 upsert；grouptype 固定为 2（明细表）")
	public R<Boolean> save(@RequestBody ModeFormGroup group) {
		return R.status(modeFormGroupService.saveOrUpdateGroup(group));
	}
}
