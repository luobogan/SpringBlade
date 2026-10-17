package org.springblade.workflow.feign;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 门户协同区(synergy) 内部 Feign 端点（供 blade-portal 协同区管理端绑定流程/节点/字段）。
 *
 * <p>与 {@link WfTaskClient} 同类：{@code @Hidden} + 无 {@code @PreAuth}，路径以
 * {@code /feign/client} 前缀隔离，网关 {@code InnerFilter} 拒绝外部访问。</p>
 *
 * <p>只读、低侵入：完全复用既有 {@code IWfDefinitionService} 能力
 * （{@code listByForm} / {@code loadNodes} / {@code getFormCondition}），不新增 Mapper/Service。</p>
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class WfSynergyClient {

	private final IWfDefinitionService definitionService;

	/**
	 * 已发布流程定义选项（id+name），供协同区选择绑定流程。
	 * <p>非管理员（Feign 内部调用无用户令牌）仅返回已发布定义，符合发起页口径。</p>
	 */
	@GetMapping("/feign/client/definition/published")
	public R<List<BrowserOptionVO>> definitionOptions() {
		List<BrowserOptionVO> opts = definitionService.listByForm(null).stream()
			.map(d -> {
				BrowserOptionVO o = new BrowserOptionVO();
				o.setValue(d.getId() == null ? null : d.getId().toString());
				o.setLabel(d.getName());
				return o;
			})
			.collect(Collectors.toList());
		return R.data(opts);
	}

	/**
	 * 指定流程定义的节点选项（id+name），供流程节点协同区选择节点。
	 */
	@GetMapping("/feign/client/definition/nodes/{defId}")
	public R<List<BrowserOptionVO>> definitionNodes(@PathVariable("defId") Long defId) {
		if (defId == null) {
			return R.fail("流程定义ID不能为空");
		}
		List<BrowserOptionVO> opts = definitionService.loadNodes(defId).stream()
			.map(n -> {
				BrowserOptionVO o = new BrowserOptionVO();
				o.setValue(n.getId() == null ? null : n.getId().toString());
				o.setLabel(n.getNodeName());
				return o;
			})
			.collect(Collectors.toList());
		return R.data(opts);
	}

	/**
	 * 指定流程定义的表单字段（key/label/control 等），供流程表单协同区做字段映射。
	 */
	@GetMapping("/feign/client/definition/fields/{defId}")
	public R<List<FormFieldVO>> definitionFields(@PathVariable("defId") Long defId) {
		if (defId == null) {
			return R.fail("流程定义ID不能为空");
		}
		List<FormFieldVO> fields = definitionService.getFormCondition("add", defId).getFields();
		return R.data(fields == null ? new ArrayList<>() : fields);
	}
}
