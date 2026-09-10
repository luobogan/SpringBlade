package org.springblade.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.dto.CirculateDTO;
import org.springblade.workflow.dto.ForwardDTO;
import org.springblade.workflow.dto.RejectDTO;
import org.springblade.workflow.dto.UrgeDTO;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程任务控制器
 *
 * <p>承载国产 OA 特有语义：同意 / 退回 / 转发 / 加签 / 抄送 / 催办。
 * 前端经网关以 {@code /api/blade-workflow/task/...} 访问。</p>
 */
@RestController
@RequestMapping("/task")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@RequiredArgsConstructor
@Tag(name = "流程任务", description = "待办/已办查询与审批处理")
public class WfTaskController {

    private final IWfTaskService taskService;

    @GetMapping("/todo")
    @Operation(summary = "我的待办")
    public R<List<WfTaskVO>> todo(
        @Parameter(description = "办理人，默认当前登录人") @RequestParam(value = "assignee", required = false) Long assignee) {
        return R.data(taskService.todo(resolveAssignee(assignee)));
    }

    @GetMapping("/done")
    @Operation(summary = "我的已办")
    public R<List<WfTaskVO>> done(
        @Parameter(description = "办理人，默认当前登录人") @RequestParam(value = "assignee", required = false) Long assignee) {
        return R.data(taskService.done(resolveAssignee(assignee)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "同意", description = "按节点 sign_order 判定是否推进：会签需全部办完，或签任一人即推进，依次逐个激活")
    public R<Boolean> approve(@PathVariable("id") Long id, @RequestBody(required = false) ApproveDTO dto) {
        return R.data(taskService.approve(id, dto), "审批完成");
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "退回", description = "指定退回节点需在 BPMN 建模退回线（P5）；当前内核阶段退回应终止实例并置为不通过")
    public R<Boolean> reject(@PathVariable("id") Long id, @RequestBody(required = false) RejectDTO dto) {
        return R.data(taskService.reject(id, dto), "已退回");
    }

    @PostMapping("/{id}/forward")
    @Operation(summary = "转发/转办")
    public R<Boolean> forward(@PathVariable("id") Long id, @RequestBody ForwardDTO dto) {
        return R.data(taskService.forward(id, dto), "已转办");
    }

    @PostMapping("/{id}/add-sign")
    @Operation(summary = "加签", description = "0前加签 1后加签")
    public R<Boolean> addSign(@PathVariable("id") Long id, @RequestBody AddSignDTO dto) {
        return R.data(taskService.addSign(id, dto), "已加签");
    }

    @PostMapping("/{id}/circulate")
    @Operation(summary = "抄送/传阅")
    public R<Boolean> circulate(@PathVariable("id") Long id, @RequestBody CirculateDTO dto) {
        return R.data(taskService.circulate(id, dto), "已抄送");
    }

    @PostMapping("/{id}/urge")
    @Operation(summary = "催办/督办")
    public R<Boolean> urge(@PathVariable("id") Long id, @RequestBody(required = false) UrgeDTO dto) {
        return R.data(taskService.urge(id, dto), "已催办");
    }

    private static Long resolveAssignee(Long assignee) {
        return (assignee != null) ? assignee : SecureUtil.getUserId();
    }

}
