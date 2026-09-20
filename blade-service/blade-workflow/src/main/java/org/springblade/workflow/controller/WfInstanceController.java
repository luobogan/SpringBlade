package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.WfNodeOperatorVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程实例控制器
 *
 * <p>路径约定：Controller 使用<b>资源路径</b>，前端经网关以
 * {@code /api/blade-workflow/instance/...} 访问。</p>
 */
@RestController
@RequestMapping("/instance")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "流程实例", description = "流程发起、实例查询、流转记录与运行期控制")
public class WfInstanceController {

    private final IWfInstanceService instanceService;

    @PostMapping("/start")
    @Operation(summary = "发起流程", description = "formId + dataId + 流程定义；写表单数据快照并生成首个待办")
    public R<Long> start(@RequestBody StartProcessDTO dto) {
        return R.data(instanceService.start(dto), "发起成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "实例详情", description = "含当前节点与状态")
    public R<InstanceVO> detail(@PathVariable("id") Long id) {
        return R.data(instanceService.detail(id));
    }

    @GetMapping("/by-biz")
    @Operation(summary = "按业务数据反查实例")
    public R<InstanceVO> byBiz(
        @Parameter(description = "表单ID") @RequestParam("formId") Long formId,
        @Parameter(description = "业务数据ID") @RequestParam("dataId") Long dataId) {
        return R.data(instanceService.getByBiz(formId, dataId));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "流转/审批记录")
    public R<List<ApprovalLogVO>> logs(@PathVariable("id") Long id) {
        return R.data(instanceService.logs(id));
    }

    @GetMapping("/{id}/node-operators")
    @Operation(summary = "节点操作者情况",
        description = "流程图节点悬浮「操作者」面板数据：按节点返回已操作 / 已查看 / 未操作人员ID（姓名由前端人员字典解析）")
    public R<java.util.Map<String, WfNodeOperatorVO>> nodeOperators(@PathVariable("id") Long id) {
        return R.data(instanceService.nodeOperators(id));
    }

    @GetMapping("/{id}/snapshot/{nodeKey}")
    @Operation(summary = "表单数据快照")
    public R<String> snapshot(@PathVariable("id") Long id,
                              @PathVariable("nodeKey") String nodeKey) {
        return R.data(instanceService.snapshot(id, nodeKey));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "撤回")
    public R<Boolean> withdraw(@PathVariable("id") Long id,
                               @RequestParam(value = "opinion", required = false) String opinion) {
        return R.data(instanceService.withdraw(id, opinion), "已撤回");
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "暂停")
    public R<Boolean> stop(@PathVariable("id") Long id) {
        return R.data(instanceService.stop(id), "已暂停");
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复")
    public R<Boolean> resume(@PathVariable("id") Long id) {
        return R.data(instanceService.resume(id), "已恢复");
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "撤销")
    public R<Boolean> cancel(@PathVariable("id") Long id,
                             @RequestParam(value = "opinion", required = false) String opinion) {
        return R.data(instanceService.cancel(id, opinion), "已撤销");
    }

}
