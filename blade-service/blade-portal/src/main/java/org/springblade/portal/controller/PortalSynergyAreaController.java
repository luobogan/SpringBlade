package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.PortalSynergyAreaSaveDTO;
import org.springblade.portal.entity.PortalSynergyArea;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalSynergyAreaService;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 门户协同区控制器（对标泛微 synergy §15.7 C2；知识型延后，本期实现 workflow/form/node）。
 * <p>深度对接 blade-workflow：绑定流程/节点/字段经其 Feign 端点拉取真实选项。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "协同区", description = "门户协同区配置（流程/流程表单/流程节点，深度对接 blade-workflow）")
public class PortalSynergyAreaController {

	private final IPortalSynergyAreaService synergyService;
	private final PortalRightUtil portalRightUtil;
	private final IPortalLogService logService;

	@GetMapping("/synergy/tree")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "协同区树", description = "按 area_type 分组的协同区列表（workflow/form/node）")
	public R<Map<String, List<PortalSynergyArea>>> tree() {
		return R.data(synergyService.tree());
	}

	@PostMapping("/synergy/save")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存协同区", description = "新增或按自然键(tenant,area_type,bind_id,node_id)更新")
	public R<PortalSynergyArea> save(@RequestBody PortalSynergyAreaSaveDTO dto) {
		requireRight(PortalConstant.PERM_SYNERGY_MAINT);
		return R.data(synergyService.save(dto));
	}

	@PostMapping("/synergy/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "注销协同区", description = "逻辑删除 + 审计")
	public R<Boolean> remove(@Parameter(description = "协同区ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_SYNERGY_MAINT);
		return R.status(synergyService.remove(id));
	}

	@PostMapping("/synergy/set-enabled")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "启用/禁用协同区")
	public R<Boolean> setEnabled(@RequestParam Long id, @RequestParam Integer enabled) {
		requireRight(PortalConstant.PERM_SYNERGY_MAINT);
		return R.status(synergyService.setEnabled(id, enabled));
	}

	@PostMapping("/synergy/set-width")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "设置协同区宽度")
	public R<Boolean> setWidth(@RequestParam Long id, @RequestParam Integer width) {
		requireRight(PortalConstant.PERM_SYNERGY_MAINT);
		return R.status(synergyService.setWidth(id, width));
	}

	@PostMapping("/synergy/save-params")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "保存协同区扩展参数")
	public R<Boolean> saveParams(@RequestParam Long id, @RequestParam(required = false) String paramsJson) {
		requireRight(PortalConstant.PERM_SYNERGY_MAINT);
		return R.status(synergyService.saveParams(id, paramsJson));
	}

	@GetMapping("/synergy/workflow-options")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "已发布流程定义选项", description = "供协同区绑定流程下拉（深度对接 blade-workflow）")
	public R<List<BrowserOptionVO>> workflowOptions() {
		return R.data(synergyService.workflowOptions());
	}

	@GetMapping("/synergy/node-options")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "流程节点选项", description = "供流程节点协同区节点下拉")
	public R<List<BrowserOptionVO>> nodeOptions(@Parameter(description = "流程定义ID") @RequestParam Long defId) {
		return R.data(synergyService.nodeOptions(defId));
	}

	@GetMapping("/synergy/wf-fields")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "流程表单字段", description = "供流程表单协同区字段映射（key/label/control）")
	public R<List<FormFieldVO>> wfFields(@Parameter(description = "流程定义ID") @RequestParam Long defId) {
		return R.data(synergyService.wfFields(defId));
	}

	private PortalSynergyAreaSaveDTO toPatch(PortalSynergyArea a) {
		PortalSynergyAreaSaveDTO dto = new PortalSynergyAreaSaveDTO();
		dto.setId(a.getId());
		dto.setAreaType(a.getAreaType());
		dto.setTitle(a.getTitle());
		dto.setBindId(a.getBindId());
		dto.setBindName(a.getBindName());
		dto.setNodeId(a.getNodeId());
		dto.setNodeName(a.getNodeName());
		dto.setFormId(a.getFormId());
		dto.setEnabled(a.getEnabled());
		dto.setWidth(a.getWidth());
		dto.setParamsJson(a.getParamsJson());
		return dto;
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行协同区维护操作");
		}
	}
}
