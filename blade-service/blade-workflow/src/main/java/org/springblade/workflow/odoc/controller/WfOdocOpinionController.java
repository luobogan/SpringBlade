package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.entity.WfOdocOpinion;
import org.springblade.workflow.odoc.service.IWfOdocOpinionService;
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
 * 公文意见模板控制器（常用批示语 / 签字意见模板）。
 *
 * <p>路径与其它 odoc 控制器同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文意见模板", description = "常用批示语/签字意见模板 维护")
public class WfOdocOpinionController {

	private final IWfOdocOpinionService opinionService;

	@GetMapping("/opinion/list")
	@Operation(summary = "意见模板列表", description = "全局公共 + 指定流程(defId 可选)")
	public R<List<WfOdocOpinion>> list(@RequestParam(required = false) Long defId) {
		return R.data(opinionService.list(defId));
	}

	@PostMapping("/opinion/save")
	@Operation(summary = "保存意见模板")
	public R<WfOdocOpinion> save(@RequestBody WfOdocOpinion opinion) {
		return R.data(opinionService.save(opinion));
	}

	@DeleteMapping("/opinion/{id}")
	@Operation(summary = "删除意见模板")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.data(opinionService.remove(id));
	}
}
