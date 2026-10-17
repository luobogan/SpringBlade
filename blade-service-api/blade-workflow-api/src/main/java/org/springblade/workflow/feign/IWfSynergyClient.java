package org.springblade.workflow.feign;

import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 门户协同区(synergy) Feign 客户端接口（深度对接 blade-workflow）。
 *
 * <p><b>服务名</b>：{@code blade-workflow}（{@link WorkflowConstant#APPLICATION_WORKFLOW_NAME}）。</p>
 * <p><b>用途</b>：门户「协同区」(流程/流程表单/流程节点协同)在管理端需要把协同区域绑定到
 * 具体流程定义/节点/字段，本客户端只读地暴露三类选项数据：
 * <ol>
 *   <li>{@code definitionOptions}：已发布流程定义（id+name），供协同区选择绑定流程；</li>
 *   <li>{@code definitionNodes}：指定流程定义的节点（id+name），供流程节点协同区选择节点；</li>
 *   <li>{@code definitionFields}：指定流程定义的表单字段（key/label/control），供流程表单协同区字段映射。</li>
 * </ol>
 * <p><b>路径约定</b>：内部端点用 {@code /feign/client} 前缀，由网关 {@code InnerFilter} 拒绝外部访问；
 * 经服务发现直连、不经网关，故不带 {@code /api} 前缀（与 {@link IWorkflowClient} 一致）。</p>
 */
@FeignClient(
	value = WorkflowConstant.APPLICATION_WORKFLOW_NAME,
	fallback = IWfSynergyClientFallback.class
)
public interface IWfSynergyClient {

	/**
	 * 已发布流程定义选项（id+name）。
	 *
	 * <p>对应服务端 {@code GET /feign/client/definition/published}，内部复用
	 * {@code IWfDefinitionService.listByForm(null)}（非管理员仅返回已发布定义）。</p>
	 *
	 * @return 流程定义选项列表（value=id 字符串，label=name）
	 */
	@GetMapping("/feign/client/definition/published")
	R<List<BrowserOptionVO>> definitionOptions();

	/**
	 * 指定流程定义的节点选项（id+name）。
	 *
	 * @param defId 流程定义ID（wf_process_definition.id）
	 * @return 节点选项列表（value=id 字符串，label=nodeName）
	 */
	@GetMapping("/feign/client/definition/nodes/{defId}")
	R<List<BrowserOptionVO>> definitionNodes(@PathVariable("defId") Long defId);

	/**
	 * 指定流程定义的表单字段（key/label/control 等）。
	 *
	 * <p>供流程表单协同区做字段映射（选择哪些字段在协同面板展示）。</p>
	 *
	 * @param defId 流程定义ID
	 * @return 表单字段列表
	 */
	@GetMapping("/feign/client/definition/fields/{defId}")
	R<List<FormFieldVO>> definitionFields(@PathVariable("defId") Long defId);
}
