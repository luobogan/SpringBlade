package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.BpmnSaveDTO;
import org.springblade.workflow.dto.DefinitionSaveDTO;
import org.springblade.workflow.dto.NodeOperatorSaveDTO;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfWorkflowType;
import org.springblade.workflow.service.IWfDefinitionService;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormConditionVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 流程定义控制器
 *
 * <p>路径约定：Controller 使用<b>资源路径</b>，网关按服务名路由
 * （{@code /blade-workflow/**} + StripPrefix=1），前端经网关以
 * {@code /api/blade-workflow/definition/...} 访问。</p>
 */
@RestController
@RequestMapping("/definition")
// 角色门禁：流程管理员（workflow），与 formmode 侧保持同一角色
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "流程定义", description = "流程定义、节点、出口、操作者配置与版本管理")
public class WfDefinitionController {

    private final IWfDefinitionService definitionService;

    @PostMapping
    @Operation(summary = "创建流程定义", description = "含节点、出口、操作者")
    public R<String> create(@RequestBody DefinitionSaveDTO dto) {
        return R.data(String.valueOf(definitionService.save(dto)), "创建成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新流程定义")
    public R<String> update(@PathVariable("id") Long id, @RequestBody DefinitionSaveDTO dto) {
        if (dto.getDefinition() != null) {
            dto.getDefinition().setId(id);
        }
        return R.data(String.valueOf(definitionService.save(dto)), "更新成功");
    }

    @PostMapping("/{id}/deploy")
    @Operation(summary = "部署到引擎", description = "将画布 BPMN 部署到 Flowable 并置为已发布")
    public R<Boolean> deploy(@PathVariable("id") Long id) {
        return R.data(definitionService.deploy(id), "部署成功");
    }

    @PutMapping("/{id}/bpmn")
    @Operation(summary = "保存 BPMN（画布产出）", description = "持久化 bpmn-js 画布 XML 并解析节点、出口")
    public R<String> saveBpmn(@PathVariable("id") Long id, @RequestBody BpmnSaveDTO body) {
        return R.data(String.valueOf(definitionService.saveBpmn(id, body.getBpmnXml())), "保存成功");
    }

    @GetMapping("/{id}/bpmn")
    @Operation(summary = "获取 BPMN XML", description = "返回画布已保存的 BPMN 2.0 定义")
    public R<String> getBpmn(@PathVariable("id") Long id) {
        return R.data(definitionService.getBpmn(id));
    }

    @PostMapping("/{id}/version")
    @Operation(summary = "另存为新版本")
    public R<String> saveAsNewVersion(@PathVariable("id") Long id) {
        return R.data(String.valueOf(definitionService.saveAsNewVersion(id)), "已生成新版本");
    }

    @GetMapping("/{id}")
    @Operation(summary = "定义详情")
    public R<WfProcessDefinition> detail(@PathVariable("id") Long id) {
        return R.data(definitionService.detail(id));
    }

    @GetMapping("/list")
    @Operation(summary = "按表单查定义列表")
    public R<List<WfProcessDefinition>> list(
        @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId) {
        return R.data(definitionService.listByForm(formId));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用")
    public R<Boolean> enable(@PathVariable("id") Long id) {
        return R.data(definitionService.enable(id, true), "已启用");
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用")
    public R<Boolean> disable(@PathVariable("id") Long id) {
        return R.data(definitionService.enable(id, false), "已停用");
    }

    @GetMapping("/{id}/nodes")
    @Operation(summary = "节点列表")
    public R<List<WfProcessNode>> nodes(@PathVariable("id") Long id) {
        return R.data(definitionService.nodes(id));
    }

    @GetMapping("/{id}/links")
    @Operation(summary = "出口列表")
    public R<List<WfNodeLink>> links(@PathVariable("id") Long id) {
        return R.data(definitionService.links(id));
    }

    @PutMapping("/{id}/node/{nodeKey}")
    @Operation(summary = "更新节点基础属性", description = "按 nodeKey 更新，保留操作者与字段权限")
    public R<WfProcessNode> updateNode(@PathVariable("id") Long id,
                                       @PathVariable("nodeKey") String nodeKey,
                                       @RequestBody WfProcessNode node) {
        return R.data(definitionService.updateNode(id, nodeKey, node), "保存成功");
    }

    @GetMapping("/{id}/node/{nodeKey}/operator")
    @Operation(summary = "读取节点操作者")
    public R<List<WfNodeOperator>> nodeOperators(@PathVariable("id") Long id,
                                                 @PathVariable("nodeKey") String nodeKey) {
        return R.data(definitionService.nodeOperators(id, nodeKey));
    }

    @PostMapping("/{id}/link")
    @Operation(summary = "新增出口（连线）")
    public R<WfNodeLink> createLink(@PathVariable("id") Long id, @RequestBody WfNodeLink link) {
        link.setId(null);
        return R.data(definitionService.saveLink(id, link), "保存成功");
    }

    @PutMapping("/{id}/link/{linkId}")
    @Operation(summary = "更新出口（连线）")
    public R<WfNodeLink> updateLink(@PathVariable("id") Long id,
                                    @PathVariable("linkId") Long linkId,
                                    @RequestBody WfNodeLink link) {
        link.setId(linkId);
        return R.data(definitionService.saveLink(id, link), "保存成功");
    }

    @DeleteMapping("/{id}/link/{linkId}")
    @Operation(summary = "删除出口（连线）")
    public R<Boolean> deleteLink(@PathVariable("id") Long id, @PathVariable("linkId") Long linkId) {
        return R.data(definitionService.deleteLink(id, linkId), "删除成功");
    }

    @DeleteMapping("/{id}/node/{nodeKey}")
    @Operation(summary = "移除节点", description = "级联清理该节点的操作者/字段权限/明细权限/出口连线/布局，避免残留孤立数据")
    public R<Boolean> deleteNode(@PathVariable("id") Long id,
                                 @PathVariable("nodeKey") String nodeKey) {
        return R.data(definitionService.deleteNode(id, nodeKey), "移除成功");
    }

    @PutMapping("/{id}/node/{nodeKey}/operator")
    @Operation(summary = "配置节点操作者", description = "整体覆盖保存；请求体为 { operators: [...] }")
    public R<Boolean> configOperator(@PathVariable("id") Long id,
                                     @PathVariable("nodeKey") String nodeKey,
                                     @RequestBody NodeOperatorSaveDTO body) {
        return R.data(definitionService.configOperator(id, nodeKey, body.getOperators()), "保存成功");
    }

    @GetMapping("/form-condition")
    @Operation(summary = "获取定义表单字段描述", description = "condition 驱动：前端据此动态渲染新增/编辑表单")
    public R<FormConditionVO> formCondition(
        @Parameter(description = "场景：add=新建 edit=编辑") @RequestParam(value = "method", defaultValue = "add") String method,
        @Parameter(description = "编辑时的定义ID") @RequestParam(value = "id", required = false) Long id) {
        return R.data(definitionService.getFormCondition(method, id));
    }

    @GetMapping("/browser/{type}")
    @Operation(summary = "浏览框选项列表", description = "按类型返回可选列表并支持关键字搜索（对齐 ecology BrowserBean，如 wftype=路径类型）")
    public R<List<BrowserOptionVO>> browserOptions(
        @Parameter(description = "浏览框类型，如 wftype") @PathVariable("type") String type,
        @Parameter(description = "搜索关键字") @RequestParam(value = "keyword", required = false) String keyword) {
        return R.data(definitionService.listBrowserOptions(type, keyword));
    }

    @GetMapping("/browser/{type}/{id}")
    @Operation(summary = "浏览框选项详情", description = "回显已选值的标签（编辑场景）")
    public R<BrowserOptionVO> browserOption(
        @Parameter(description = "浏览框类型") @PathVariable("type") String type,
        @Parameter(description = "已选值（主键）") @PathVariable("id") Long id) {
        return R.data(definitionService.getBrowserOption(type, id));
    }

    @PostMapping("/browser/wftype")
    @Operation(summary = "新增路径类型", description = "浏览框 wftype 的「+」新增，回写新选项（BrowserOptionVO）供前端直接选中")
    public R<BrowserOptionVO> addWorkflowType(@RequestBody WfWorkflowType type) {
        return R.data(definitionService.saveBrowserOption("wftype", type), "添加成功");
    }

}
