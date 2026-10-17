package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;
import org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService;
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
 * 收发文公共组控制器（对应 E9 receiveunit_group）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "收发文公共组", description = "收发文公共组(多单位打包) 维护")
public class WfOdocReceiveGroupController {

	private final IWfOdocReceiveGroupService groupService;

	@GetMapping("/receive-group/list")
	@Operation(summary = "公共组列表", description = "enabledOnly=true 仅启用")
	public R<List<WfOdocReceiveGroup>> list(
		@RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
		return R.data(enabledOnly ? groupService.listEnabled() : groupService.list());
	}

	@GetMapping("/receive-group/{id}")
	@Operation(summary = "按ID取公共组")
	public R<WfOdocReceiveGroup> get(@PathVariable Long id) {
		return R.data(groupService.get(id));
	}

	@PostMapping("/receive-group/save")
	@Operation(summary = "保存公共组", description = "有 id 更新，无 id 新增")
	public R<WfOdocReceiveGroup> save(@RequestBody WfOdocReceiveGroup group) {
		return R.data(groupService.save(group));
	}

	@DeleteMapping("/receive-group/{id}")
	@Operation(summary = "删除公共组")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(groupService.remove(id));
	}
}
