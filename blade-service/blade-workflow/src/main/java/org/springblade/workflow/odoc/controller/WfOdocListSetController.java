package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocListSet;
import org.springblade.workflow.odoc.service.IWfOdocListSetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公文列表列设置控制器（对应 E9 OdocCustomListSet）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文列表列设置", description = "按列表编码配置默认隐藏列")
public class WfOdocListSetController {

	private final IWfOdocListSetService listSetService;

	@GetMapping("/list-set")
	@Operation(summary = "取列表列设置", description = "无则返回 null")
	public R<WfOdocListSet> get(@RequestParam String listCode) {
		return R.data(listSetService.getByCode(listCode));
	}

	@PostMapping("/list-set")
	@Operation(summary = "保存列表列设置", description = "按 listCode 幂等 upsert")
	public R<WfOdocListSet> save(@RequestBody WfOdocListSet set) {
		return R.data(listSetService.save(set));
	}
}
