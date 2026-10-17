package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公文通用设置控制器（单例）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文通用设置", description = "模块级开关 + 新建流程默认值")
public class WfOdocSettingController {

	private final IWfOdocSettingService settingService;

	@GetMapping("/setting")
	@Operation(summary = "取通用设置", description = "无落库行时返回默认值")
	public R<WfOdocSetting> get() {
		return R.data(settingService.get());
	}

	@PostMapping("/setting")
	@Operation(summary = "保存通用设置", description = "单例 upsert")
	public R<WfOdocSetting> save(@RequestBody WfOdocSetting setting) {
		return R.data(settingService.save(setting));
	}
}
