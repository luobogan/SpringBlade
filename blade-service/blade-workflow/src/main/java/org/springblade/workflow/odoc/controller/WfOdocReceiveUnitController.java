package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;
import org.springblade.workflow.odoc.service.IWfOdocReceiveUnitService;
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
 * 收发文单位控制器（对应 E9 DOCRECEIVEUNIT）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "收发文单位", description = "收发文单位档案(层级/内部外部) 维护")
public class WfOdocReceiveUnitController {

	private final IWfOdocReceiveUnitService unitService;

	@GetMapping("/receive-unit/list")
	@Operation(summary = "单位列表", description = "enabledOnly=true 仅启用")
	public R<List<WfOdocReceiveUnit>> list(
		@RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
		return R.data(enabledOnly ? unitService.listEnabled() : unitService.list());
	}

	@GetMapping("/receive-unit/children")
	@Operation(summary = "下级单位", description = "superiorId 空=根级")
	public R<List<WfOdocReceiveUnit>> children(@RequestParam(required = false) Long superiorId) {
		return R.data(unitService.listChildren(superiorId));
	}

	@GetMapping("/receive-unit/{id}")
	@Operation(summary = "按ID取单位")
	public R<WfOdocReceiveUnit> get(@PathVariable Long id) {
		return R.data(unitService.get(id));
	}

	@PostMapping("/receive-unit/save")
	@Operation(summary = "保存单位", description = "有 id 更新，无 id 新增；禁止上级为自身")
	public R<WfOdocReceiveUnit> save(@RequestBody WfOdocReceiveUnit unit) {
		return R.data(unitService.save(unit));
	}

	@DeleteMapping("/receive-unit/{id}")
	@Operation(summary = "删除单位")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(unitService.remove(id));
	}
}
