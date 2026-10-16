package org.springblade.workflow.odoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.odoc.dto.OdocDocTypeChangeSubmitDTO;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChange;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChangeRevision;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.service.IWfOdocDocTypeChangeService;
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
 * 文种转换控制器（对应 E9 {@code OdocDocTypeChangeAction} 9 端点，收敛为设置/赋值/记录/提交）。
 *
 * <p>路径与 {@link WfOdocController} 同基 {@code /odoc}，网关前缀 {@code /blade-workflow/odoc/**}。</p>
 */
@RestController
@RequestMapping("/odoc")
@RequiredArgsConstructor
@Tag(name = "公文文种转换", description = "转换配置/赋值设置/变更记录/提交变更")
public class WfOdocDocTypeChangeController {

	private final IWfOdocDocTypeChangeService docTypeChangeService;

	@GetMapping("/doc-type-change/setting")
	@Operation(summary = "文种转换配置列表", description = "按源流程定义取可变更目标")
	public R<List<WfOdocDocTypeChange>> setting(@RequestParam Long defId) {
		return R.data(docTypeChangeService.listByDefId(defId));
	}

	@PostMapping("/doc-type-change/setting")
	@Operation(summary = "保存文种转换配置", description = "按 源defId+目标defId 幂等 upsert")
	public R<WfOdocDocTypeChange> saveSetting(@RequestBody WfOdocDocTypeChange config) {
		return R.data(docTypeChangeService.save(config));
	}

	@DeleteMapping("/doc-type-change/setting/{id}")
	@Operation(summary = "删除文种转换配置")
	public R<Boolean> removeSetting(@PathVariable Long id) {
		return R.data(docTypeChangeService.remove(id));
	}

	@GetMapping("/doc-type-change/assignment")
	@Operation(summary = "赋值设置", description = "文种变更次数限制")
	public R<WfOdocProcessSet> assignment(@RequestParam Long defId) {
		return R.data(docTypeChangeService.getAssignment(defId));
	}

	@PostMapping("/doc-type-change/assignment")
	@Operation(summary = "保存赋值设置", description = "文种变更次数限制，按 defId upsert")
	public R<WfOdocProcessSet> saveAssignment(@RequestBody WfOdocProcessSet set) {
		return R.data(docTypeChangeService.saveAssignment(set));
	}

	@GetMapping("/doc-type-change/revision")
	@Operation(summary = "文种变更记录", description = "按实例取变更记录（PC/移动同源）")
	public R<List<WfOdocDocTypeChangeRevision>> revision(@RequestParam Long instanceId) {
		return R.data(docTypeChangeService.revisions(instanceId));
	}

	@PostMapping("/doc-type-change/submit")
	@Operation(summary = "提交文种变更", description = "校验次数→记录变更→更新公文镜像 officalType")
	public R<WfOdocDocTypeChangeRevision> submit(@RequestBody OdocDocTypeChangeSubmitDTO dto) {
		return R.data(docTypeChangeService.submit(dto));
	}

}
