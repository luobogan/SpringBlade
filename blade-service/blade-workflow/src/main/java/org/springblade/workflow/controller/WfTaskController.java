package org.springblade.workflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.dto.WfTaskListFilter;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.dto.CirculateDTO;
import org.springblade.workflow.dto.ForwardDTO;
import org.springblade.workflow.dto.RejectDTO;
import org.springblade.workflow.dto.UrgeDTO;
import org.springblade.workflow.service.IWfTaskService;
import org.springblade.workflow.utils.WfNoticeContext;
import org.springblade.workflow.vo.RejectCandidatesVO;
import org.springblade.workflow.vo.WfTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 流程任务控制器
 *
 * <p>承载国产 OA 特有语义：同意 / 退回 / 转发 / 加签 / 抄送 / 催办。
 * 前端经网关以 {@code /api/blade-workflow/task/...} 访问。</p>
 *
 * <p><b>鉴权</b>：待办 / 已办 / 办理是<b>全体员工</b>都要用的能力，因此这里只要求
 * 「已登录」（{@link WorkflowConstant#HAS_AUTH}），不再要求 {@code workflow} 角色。
 * 「能不能动这一条任务」由服务层记录级校验兜底（
 * {@code WfTaskServiceImpl} 内 {@code WfAuthUtil.requireOperateTask}）：
 * 只有该任务办理人本人或流程管理员能同意/退回/转办/加签/抄送/标记已查看；
 * 催办限发起人、该节点办理人或管理员；待办/已办查询的 {@code assignee} 入参
 * 对非管理员一律收敛为当前登录人。</p>
 */
@RestController
@RequestMapping("/task")
@PreAuth(WorkflowConstant.HAS_AUTH)
@RequiredArgsConstructor
@Tag(name = "流程任务", description = "待办/已办查询与审批处理")
public class WfTaskController {

    private final IWfTaskService taskService;

    @GetMapping("/todo")
    @Operation(summary = "我的待办", description = "支持按标题/流程定义/表单/时间范围筛选（去 wf_ 表 T-10）")
    public R<List<WfTaskVO>> todo(
        @Parameter(description = "办理人，默认当前登录人") @RequestParam(value = "assignee", required = false) Long assignee,
        @Parameter(description = "流程标题模糊") @RequestParam(value = "title", required = false) String title,
        @Parameter(description = "流程定义ID") @RequestParam(value = "defId", required = false) Long defId,
        @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId,
        @Parameter(description = "接收时间范围起点 ISO8601") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date beginTime,
        @Parameter(description = "接收时间范围终点 ISO8601") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) {
        WfTaskListFilter filter = new WfTaskListFilter();
        filter.setTitle(title);
        filter.setDefId(defId);
        filter.setFormId(formId);
        filter.setBeginTime(beginTime);
        filter.setEndTime(endTime);
        return R.data(taskService.todo(resolveAssignee(assignee), filter));
    }

    @GetMapping("/done")
    @Operation(summary = "我的已办", description = "支持按标题/流程定义/表单/时间范围筛选（去 wf_ 表 T-10）")
    public R<List<WfTaskVO>> done(
        @Parameter(description = "办理人，默认当前登录人") @RequestParam(value = "assignee", required = false) Long assignee,
        @Parameter(description = "流程标题模糊") @RequestParam(value = "title", required = false) String title,
        @Parameter(description = "流程定义ID") @RequestParam(value = "defId", required = false) Long defId,
        @Parameter(description = "表单ID") @RequestParam(value = "formId", required = false) Long formId,
        @Parameter(description = "处理时间范围起点 ISO8601") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date beginTime,
        @Parameter(description = "处理时间范围终点 ISO8601") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) {
        WfTaskListFilter filter = new WfTaskListFilter();
        filter.setTitle(title);
        filter.setDefId(defId);
        filter.setFormId(formId);
        filter.setBeginTime(beginTime);
        filter.setEndTime(endTime);
        return R.data(taskService.done(resolveAssignee(assignee), filter));
    }

    /**
     * 我的待办（服务端真分页）。
     *
     * <p>与 {@link #todo} 并存：旧接口仍返回全量 List，供 Feign {@code WfTaskClient}、
     * 办理页待办判断、业务引用选择器等既有调用方使用；列表页改用本接口。</p>
     */
    @GetMapping("/todo/page")
    @Operation(summary = "我的待办（分页）", description = "服务端分页，筛选条件下推 SQL")
    public R<IPage<WfTaskVO>> todoPage(
        @RequestParam(value = "assignee", required = false) Long assignee,
        @RequestParam(value = "current", required = false, defaultValue = "1") Long current,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "defId", required = false) Long defId,
        @RequestParam(value = "formId", required = false) Long formId,
        @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date beginTime,
        @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) {
        WfTaskListFilter filter = new WfTaskListFilter();
        filter.setTitle(title);
        filter.setStatus(status);
        filter.setDefId(defId);
        filter.setFormId(formId);
        filter.setBeginTime(beginTime);
        filter.setEndTime(endTime);
        return R.data(taskService.todoPage(resolveAssignee(assignee), filter, current, pageSize));
    }

    /** 我的已办（服务端真分页），与 {@link #done} 并存。 */
    @GetMapping("/done/page")
    @Operation(summary = "我的已办（分页）", description = "服务端分页，筛选条件下推 SQL")
    public R<IPage<WfTaskVO>> donePage(
        @RequestParam(value = "assignee", required = false) Long assignee,
        @RequestParam(value = "current", required = false, defaultValue = "1") Long current,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "defId", required = false) Long defId,
        @RequestParam(value = "formId", required = false) Long formId,
        @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date beginTime,
        @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endTime) {
        WfTaskListFilter filter = new WfTaskListFilter();
        filter.setTitle(title);
        filter.setStatus(status);
        filter.setDefId(defId);
        filter.setFormId(formId);
        filter.setBeginTime(beginTime);
        filter.setEndTime(endTime);
        return R.data(taskService.donePage(resolveAssignee(assignee), filter, current, pageSize));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "同意", description = "按节点 sign_order 判定是否推进：会签需全部办完，或签任一人即推进，依次逐个激活")
    public R<Boolean> approve(@PathVariable("id") Long id, @RequestBody(required = false) ApproveDTO dto) {
        try {
            // 把 service 层的非阻断配置提示（如出口条件字段未随提交下发）拼进 msg，前端据此提示用户
            return R.data(taskService.approve(id, dto), WfNoticeContext.join("审批完成"));
        } finally {
            WfNoticeContext.clear();
        }
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "退回", description = "将流程回退到目标节点（保持实例运行）；targetNodeKey 为空时退默认/上一节点")
    public R<Boolean> reject(@PathVariable("id") Long id, @RequestBody(required = false) RejectDTO dto) {
        return R.data(taskService.reject(id, dto), "已退回");
    }

    @GetMapping("/{id}/reject-nodes")
    @Operation(summary = "可退回节点", description = "退回前查询当前节点可退回的目标节点及退回方式（直接/选择），对应 ecology RejectNodeSet")
    public R<RejectCandidatesVO> rejectNodes(@PathVariable("id") Long id) {
        return R.data(taskService.rejectNodes(id));
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

    @PostMapping("/{id}/view")
    @Operation(summary = "标记已查看",
        description = "办理人打开待办/办理页时调用，记录首次查看时间；流程图「操作者」面板据此区分「已查看」与「未操作」")
    public R<Boolean> view(@PathVariable("id") Long id) {
        return R.data(taskService.markViewed(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情（含所属实例ID）",
        description = "供消息中心 / 浮窗点击「待办任务」跳转办理页时，由 taskId 解析出 instanceId；记录级鉴权（仅办理人或管理员可见）")
    public R<WfTaskVO> detail(@PathVariable("id") Long id) {
        return R.data(taskService.detail(id));
    }

    private static Long resolveAssignee(Long assignee) {
        return (assignee != null) ? assignee : SecureUtil.getUserId();
    }

}
