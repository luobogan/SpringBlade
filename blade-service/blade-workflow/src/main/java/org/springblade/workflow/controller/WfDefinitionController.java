package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
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
import org.springblade.workflow.vo.FormBindingVO;
import org.springblade.workflow.vo.FormConditionVO;
import org.springblade.workflow.vo.SimulateResultVO;
import org.springblade.workflow.vo.VersionDiffVO;
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
 *
 * <p><b>鉴权分两层</b>：</p>
 * <ul>
 *   <li>类级 {@link WorkflowConstant#HAS_ROLE_WORKFLOW}：<b>建模 / 部署 / 版本 / 启停 /
 *       删除 / 节点与出口配置 / 操作者配置 / 模拟测试 / 路径类型新增</b> 等管理动作，
 *       只有流程管理员可调（默认行为，无需逐个标注）。</li>
 *   <li>方法级 {@link WorkflowConstant#HAS_AUTH}：发起页与办理页要用的<b>只读</b>接口
 *       （列表 / 详情 / BPMN / 节点 / 出口 / 浏览框选项）。普通员工没有 {@code workflow}
 *       角色也必须能选流程、看流程图，因此只要求「已登录」。</li>
 * </ul>
 * <p>只读接口放开后「能看到哪些流程」由服务层收口：非管理员只返回<b>已发布</b>
 * （{@code status=1}）的定义，草稿/停用版本不对普通用户暴露；
 * 发起动作本身的记录级校验在实例侧（{@code WfInstanceServiceImpl}）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/definition")
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
        Boolean ok = definitionService.deploy(id);
        // 管理面审计（方案 §8 P3）：部署会改变「引擎最新版本」，必须能回答
        // 「谁在何时部署了哪个定义」—— 这是排查「正式版本被测试/手工部署顶替」的第一手依据
        log.info("[blade-workflow][审计] 部署流程定义. defId={}, 结果={}, operator={}",
            id, ok, SecureUtil.getUserId());
        return R.data(ok, "部署成功");
    }

    @PutMapping("/{id}/bpmn")
    @Operation(summary = "保存 BPMN（画布产出）", description = "持久化 bpmn-js 画布 XML 并解析节点、出口")
    public R<String> saveBpmn(@PathVariable("id") Long id, @RequestBody BpmnSaveDTO body) {
        return R.data(String.valueOf(definitionService.saveBpmn(id, body.getBpmnXml())), "保存成功");
    }

    @GetMapping("/{id}/bpmn")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "获取 BPMN XML", description = "返回画布已保存的 BPMN 2.0 定义；发起页/办理页「流程图」页签只读使用")
    public R<String> getBpmn(@PathVariable("id") Long id) {
        return R.data(definitionService.getBpmn(id));
    }

    @PostMapping("/{id}/version")
    @Operation(summary = "另存为新版本", description = "完整复制定义/BPMN/节点/出口/权限/操作者为 v+1 草稿，返回新版本 defId")
    public R<String> saveAsNewVersion(@PathVariable("id") Long id) {
        String newDefId = String.valueOf(definitionService.saveAsNewVersion(id));
        log.info("[blade-workflow][审计] 另存为新版本. 源defId={}, 新defId={}, operator={}",
            id, newDefId, SecureUtil.getUserId());
        return R.data(newDefId, "已生成新版本");
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "版本列表", description = "同一流程（同 procKey 版本组）的全部版本，按版本号升序")
    public R<List<WfProcessDefinition>> versions(@PathVariable("id") Long id) {
        return R.data(definitionService.versions(id));
    }

    @PostMapping("/{id}/version/activate")
    @Operation(summary = "切换当前版本", description = "把该定义所在版本组的当前（激活）版本切为本版本；不部署引擎、不改发布状态")
    public R<Boolean> activateVersion(@PathVariable("id") Long id) {
        Boolean ok = definitionService.activateVersion(id);
        // 版本切换直接决定「之后发起的流程跑哪一版」，是灰度/回滚的关键动作 → 必须留痕
        log.info("[blade-workflow][审计] 切换当前版本. defId={}, 结果={}, operator={}",
            id, ok, SecureUtil.getUserId());
        return R.data(ok, "已切换为当前版本");
    }

    @GetMapping("/{id}/version/diff")
    @Operation(summary = "版本差异对比", description = "当前版本 vs 目标版本（targetId），按节点/出口输出增删改差异")
    public R<VersionDiffVO> versionDiff(@PathVariable("id") Long id,
                                        @Parameter(description = "被对比版本defId") @RequestParam("targetId") Long targetId) {
        return R.data(definitionService.diff(id, targetId));
    }

    @GetMapping("/{id}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "定义详情", description = "发起页据此校验「已发布 + 当前激活版本」；只读，登录即可")
    public R<WfProcessDefinition> detail(@PathVariable("id") Long id) {
        return R.data(definitionService.detail(id));
    }

    @GetMapping("/list")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "按表单查定义列表",
        description = "发起页「可发起流程」卡片列表数据源：非流程管理员只返回已发布(status=1)的定义")
    public R<List<WfProcessDefinition>> list(
        @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId) {
        return R.data(definitionService.listByForm(formId));
    }

    @GetMapping("/form-binding/{formId}")
    @Operation(summary = "查询表单绑定情况", description = "返回绑定该表单的流程定义与流程实例数量；供表单删除前校验")
    public R<FormBindingVO> formBinding(@PathVariable("formId") Long formId) {
        return R.data(definitionService.formBinding(formId));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用")
    public R<Boolean> enable(@PathVariable("id") Long id) {
        Boolean ok = definitionService.enable(id, true);
        log.info("[blade-workflow][审计] 启用流程定义. defId={}, 结果={}, operator={}",
            id, ok, SecureUtil.getUserId());
        return R.data(ok, "已启用");
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用")
    public R<Boolean> disable(@PathVariable("id") Long id) {
        Boolean ok = definitionService.enable(id, false);
        log.info("[blade-workflow][审计] 停用流程定义. defId={}, 结果={}, operator={}",
            id, ok, SecureUtil.getUserId());
        return R.data(ok, "已停用");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除流程定义", description = "级联清理节点、出口、操作者、权限与布局，再删除定义本身")
    public R<Boolean> remove(@PathVariable("id") Long id) {
        Boolean ok = definitionService.removeDefinition(id);
        log.info("[blade-workflow][审计] 删除流程定义. defId={}, 结果={}, operator={}",
            id, ok, SecureUtil.getUserId());
        return R.data(ok, "已删除");
    }

    @GetMapping("/{id}/nodes")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "节点列表", description = "发起页据 nodeType=0 定位开始节点；只读，登录即可")
    public R<List<WfProcessNode>> nodes(@PathVariable("id") Long id) {
        return R.data(definitionService.nodes(id));
    }

    @GetMapping("/{id}/links")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "出口列表", description = "流程图连线与条件展示；只读，登录即可")
    public R<List<WfNodeLink>> links(@PathVariable("id") Long id) {
        return R.data(definitionService.links(id));
    }

    @PostMapping("/{id}/simulate")
    @Operation(summary = "流程模拟运行", description = "带模拟表单数据走查节点/网关条件，逐节点校验并回写测试状态（设计期校验）")
    public R<SimulateResultVO> simulate(@PathVariable("id") Long id,
                                       @RequestBody(required = false) Map<String, Object> formData) {
        return R.data(definitionService.simulate(id, formData), "模拟完成");
    }

    @PostMapping("/{id}/node/{nodeKey}/test-status")
    @Operation(summary = "保存节点测试状态", description = "0未测试 1通过 2未通过（模拟运行结果，或手动标记）")
    public R<Boolean> saveNodeTestStatus(@PathVariable("id") Long id,
                                        @PathVariable("nodeKey") String nodeKey,
                                        @RequestParam("status") int status) {
        definitionService.saveNodeTestStatus(id, nodeKey, status);
        return R.data(true, "已保存");
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

    @PostMapping("/{id}/node/{nodeKey}/operator/sync")
    @Operation(summary = "同步操作者到其它节点", description = "把本节点操作者整体覆盖写入目标节点集合")
    public R<Boolean> syncOperator(@PathVariable("id") Long id,
                                   @PathVariable("nodeKey") String nodeKey,
                                   @RequestBody List<String> targetNodeKeys) {
        return R.data(definitionService.syncOperatorToNodes(id, nodeKey, targetNodeKeys), "同步成功");
    }

    @GetMapping("/form-condition")
    @Operation(summary = "获取定义表单字段描述", description = "condition 驱动：前端据此动态渲染新增/编辑表单")
    public R<FormConditionVO> formCondition(
        @Parameter(description = "场景：add=新建 edit=编辑") @RequestParam(value = "method", defaultValue = "add") String method,
        @Parameter(description = "编辑时的定义ID") @RequestParam(value = "id", required = false) Long id) {
        return R.data(definitionService.getFormCondition(method, id));
    }

    @GetMapping("/browser/{type}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "浏览框选项列表", description = "按类型返回可选列表并支持关键字搜索（对齐 ecology BrowserBean，如 wftype=路径类型）；发起页按「路径类型」分组展示，登录即可")
    public R<List<BrowserOptionVO>> browserOptions(
        @Parameter(description = "浏览框类型，如 wftype") @PathVariable("type") String type,
        @Parameter(description = "搜索关键字") @RequestParam(value = "keyword", required = false) String keyword) {
        return R.data(definitionService.listBrowserOptions(type, keyword));
    }

    @GetMapping("/browser/{type}/{id}")
    @PreAuth(WorkflowConstant.HAS_AUTH)
    @Operation(summary = "浏览框选项详情", description = "回显已选值的标签（编辑与运行时展示都要用），登录即可")
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
