package org.springblade.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.system.dto.ScopeFieldBatch;
import org.springblade.system.entity.ScopeField;
import org.springblade.system.service.IScopeFieldService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据权限可见字段配置控制器。
 *
 * <p>与 DataScopeController 分开：列表动态列是「读多写少」的展示配置，
 * 读取必须对全部登录用户开放（业务列表页需要据此渲染表头），
 * 仅写入要求管理员角色。</p>
 */
@RestController
@AllArgsConstructor
@RequestMapping("/scope-field")
@Tag(name = "数据权限可见字段", description = "列表动态列配置")
public class ScopeFieldController extends BladeController {

	private final IScopeFieldService scopeFieldService;

	/**
	 * 查询列配置：优先按 scopeId 精确查，否则按 menuId / resourceCode 查菜单级默认配置
	 */
	@GetMapping("/list")
	@Operation(summary = "查询可见字段配置")
	public R<List<ScopeField>> list(
		@RequestParam(required = false) Long scopeId,
		@RequestParam(required = false) Long menuId,
		@RequestParam(required = false) String resourceCode) {
		return R.data(scopeFieldService.listByScope(scopeId, menuId, resourceCode));
	}

	/**
	 * 批量保存（按 scopeId + menuId 差集合并）
	 */
	@PostMapping("/submit")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@Operation(summary = "保存可见字段配置")
	public R submit(@Valid @RequestBody ScopeFieldBatch batch) {
		try {
			return R.status(scopeFieldService.saveBatchConfig(batch));
		} catch (IllegalArgumentException e) {
			return R.fail(e.getMessage());
		}
	}
}
