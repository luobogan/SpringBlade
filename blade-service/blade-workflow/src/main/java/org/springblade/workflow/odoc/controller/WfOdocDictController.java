package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocDict;
import org.springblade.workflow.odoc.service.IWfOdocDictService;
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
 * 公文字典控制器（秘密等级/紧急程度/种类/主题分类/机构代字）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文字典", description = "秘密等级/紧急程度/种类/主题分类/机构代字 维护")
public class WfOdocDictController {

	private final IWfOdocDictService dictService;

	@GetMapping("/dict/{dictType}")
	@Operation(summary = "某类型字典列表", description = "secret_level/urgency/doc_type/topic_type/org_abbr")
	public R<List<WfOdocDict>> listByType(@PathVariable String dictType,
										  @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
		return R.data(enabledOnly ? dictService.listEnabledByType(dictType) : dictService.listByType(dictType));
	}

	@GetMapping("/dict/{dictType}/{code}")
	@Operation(summary = "按编码取字典项")
	public R<WfOdocDict> getByCode(@PathVariable String dictType, @PathVariable String code) {
		return R.data(dictService.getByCode(dictType, code));
	}

	@PostMapping("/dict/save")
	@Operation(summary = "保存字典项", description = "按 dictType+code 幂等 upsert")
	public R<WfOdocDict> save(@RequestBody WfOdocDict dict) {
		return R.data(dictService.save(dict));
	}

	@DeleteMapping("/dict/{id}")
	@Operation(summary = "删除字典项")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(dictService.remove(id));
	}
}
