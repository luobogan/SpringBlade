package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocSecretRule;
import org.springblade.workflow.odoc.service.IWfOdocSecretRuleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 密级收发限制规则控制器（对应 E9 SENDREUNITRESTRICTIONS / CommReceiveGroup）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。
 * 提交时的强制校验由 {@code WfNodeMustDoActionExecutor} 完成；{@code /check} 供前端预校验。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "密级收发限制", description = "按密级约束收文单位/公共组")
public class WfOdocSecretRuleController {

	private final IWfOdocSecretRuleService secretRuleService;

	@GetMapping("/secret-rule/list")
	@Operation(summary = "规则列表")
	public R<List<WfOdocSecretRule>> list() {
		return R.data(secretRuleService.list());
	}

	@PostMapping("/secret-rule/save")
	@Operation(summary = "保存规则")
	public R<WfOdocSecretRule> save(@RequestBody WfOdocSecretRule rule) {
		return R.data(secretRuleService.save(rule));
	}

	@DeleteMapping("/secret-rule/{id}")
	@Operation(summary = "删除规则")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(secretRuleService.remove(id));
	}

	@PostMapping("/secret-rule/check")
	@Operation(summary = "预校验", description = "返回 null=通过，否则拒绝原因")
	public R<String> check(@RequestParam String docSecretLevel, @RequestParam String receiveUnitIds) {
		return R.data(secretRuleService.violationMessage(docSecretLevel, receiveUnitIds));
	}
}
